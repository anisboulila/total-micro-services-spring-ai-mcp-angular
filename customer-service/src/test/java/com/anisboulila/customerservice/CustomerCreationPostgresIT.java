package com.anisboulila.customerservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "customer.profile-creation.enabled=true",
        "spring.flyway.schemas=cps_customer_schema",
        "spring.flyway.default-schema=cps_customer_schema",
        "spring.jpa.properties.hibernate.default_schema=cps_customer_schema"
})
@AutoConfigureMockMvc
class CustomerCreationPostgresIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        // Cette suppression concerne exclusivement le conteneur PostgreSQL créé pour ce test.
        jdbcTemplate.update("DELETE FROM cps_customer_schema.customer");
        when(jwtDecoder.decode("first")).thenReturn(jwt("subject-one", "ada@example.test", "CUSTOMER"));
        when(jwtDecoder.decode("same-sub")).thenReturn(jwt("subject-one", "other@example.test", "CUSTOMER"));
        when(jwtDecoder.decode("same-email")).thenReturn(jwt("subject-two", " ADA@EXAMPLE.TEST ", "CUSTOMER"));
        when(jwtDecoder.decode("operator")).thenReturn(jwt("operator", "operator@example.test", "OPERATOR"));
    }

    @Test
    void createsProfileAndRejectsBothDuplicateIdentitiesWithoutChangingIt(CapturedOutput output) throws Exception {
        var created = mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer first")
                        .contentType("application/json")
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@example.test"))
                .andReturn();

        Map<String, Object> initial = jdbcTemplate.queryForMap(
                "SELECT id, sub, name, email, created_at FROM cps_customer_schema.customer WHERE sub = ?", "subject-one");
        assertEquals("/api/v1/customers/" + initial.get("id"), created.getResponse().getHeader("Location"));

        assertConflict("same-sub", "CUSTOMER_PROFILE_EXISTS");
        assertUnchanged(initial);
        assertConflict("same-email", "EMAIL_ALREADY_EXISTS");
        assertUnchanged(initial);
        assertFalse(output.getOut().contains("subject-one"));
        assertFalse(output.getOut().contains("ada@example.test"));
    }

    @Test
    void operatorCannotCreateProfile() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer operator")
                        .contentType("application/json")
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_PROFILE_CREATION"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
    }

    private void assertConflict(String token, String code) throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"name\":\"Grace\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    private void assertUnchanged(Map<String, Object> initial) {
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
        assertEquals(initial, jdbcTemplate.queryForMap(
                "SELECT id, sub, name, email, created_at FROM cps_customer_schema.customer WHERE sub = ?", "subject-one"));
    }

    private static Jwt jwt(String sub, String email, String role) {
        return Jwt.withTokenValue("test-token").header("alg", "RS256")
                .claim("sub", sub)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
