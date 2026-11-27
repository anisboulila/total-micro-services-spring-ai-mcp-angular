package com.anisboulila.customerservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = {
        "customer.profile-creation.enabled=true",
        "spring.flyway.schemas=cps_customer_schema",
        "spring.flyway.default-schema=cps_customer_schema",
        "spring.jpa.properties.hibernate.default_schema=cps_customer_schema"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
class CustomerKeycloakPostgresIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.8.1")
            .withExposedPorts(8080)
            .withCopyFileToContainer(MountableFile.forClasspathResource("keycloak/customer-test-realm.json"),
                    "/opt/keycloak/data/import/customer-integration-test-realm.json")
            .withCommand("start-dev", "--import-realm")
            .waitingFor(Wait.forHttp("/realms/customer-integration-test/.well-known/openid-configuration")
                    .forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));

    @DynamicPropertySource
    static void configureServices(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("customer.security.jwt.issuer-uri", CustomerKeycloakPostgresIT::issuer);
        registry.add("customer.security.jwt.jwk-set-uri", () -> issuer() + "/protocol/openid-connect/certs");
        registry.add("customer.security.jwt.audience", () -> "customer-service");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearProfiles() {
        jdbcTemplate.update("DELETE FROM cps_customer_schema.customer");
    }

    @Test
    void realTokensEnforceIdentityRolesAndIdempotence() throws Exception {
        String alice = token("customer-test-client", "alice-test");
        String bob = token("customer-test-client", "bob-test");
        String operator = token("customer-test-client", "operator-test");
        String dual = token("customer-test-client", "dual-test");
        String wrongAudience = token("wrong-audience-client", "alice-test");

        MvcResult aliceCreated = create(alice, "alice-key", "Alice");
        MvcResult aliceReplayed = create(alice, "alice-key", "Alice");
        assertEquals(aliceCreated.getResponse().getContentAsString(), aliceReplayed.getResponse().getContentAsString());
        assertEquals(aliceCreated.getResponse().getHeader("Location"), aliceReplayed.getResponse().getHeader("Location"));
        MvcResult bobCreated = create(bob, "bob-key", "Bob");
        String aliceLocation = Objects.requireNonNull(aliceCreated.getResponse().getHeader("Location"));
        String bobLocation = Objects.requireNonNull(bobCreated.getResponse().getHeader("Location"));

        mockMvc.perform(get(aliceLocation).header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Alice"));
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alice"))
                .andExpect(jsonPath("$.totalElements").value(1));
        String correlation = "d60c6a93-287c-49e9-9b56-40a72dc08f04";
        MvcResult foreign = mockMvc.perform(get(bobLocation).header("Authorization", "Bearer " + alice)
                        .header("X-Correlation-ID", correlation))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andReturn();
        MvcResult missing = mockMvc.perform(get("/api/v1/customers/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + alice).header("X-Correlation-ID", correlation))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andReturn();
        ObjectNode foreignBody = (ObjectNode) objectMapper.readTree(foreign.getResponse().getContentAsString());
        ObjectNode missingBody = (ObjectNode) objectMapper.readTree(missing.getResponse().getContentAsString());
        foreignBody.remove("instance");
        missingBody.remove("instance");
        assertEquals(foreignBody, missingBody);

        for (String token : new String[] {operator, dual}) {
            mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + token)
                            .param("page", "0").param("size", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2));
            mockMvc.perform(get(bobLocation).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Bob"));
            mockMvc.perform(post("/api/v1/customers").header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", "forbidden-key").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Forbidden\"}"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN_PROFILE_CREATION"));
        }
        mockMvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + wrongAudience))
                .andExpect(status().isUnauthorized());
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer_idempotency", Integer.class));
    }

    private MvcResult create(String token, String key, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/customers").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("name", name))))
                .andReturn();
        if (result.getResponse().getStatus() != 201) {
            throw new AssertionError("Unexpected creation response: " + result.getResponse().getContentAsString());
        }
        assertNotNull(result.getResponse().getHeader("Location"));
        return result;
    }

    private static String issuer() {
        return "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080)
                + "/realms/customer-integration-test";
    }

    private String token(String client, String username) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", client);
        form.add("username", username);
        form.add("password", "test-only");
        JsonNode response = RestClient.create().post().uri(issuer() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(JsonNode.class);
        assertNotNull(response);
        JsonNode accessToken = response.get("access_token");
        assertNotNull(accessToken);
        assertTrue(accessToken.isTextual() && !accessToken.asText().isBlank());
        return accessToken.asText();
    }
}
