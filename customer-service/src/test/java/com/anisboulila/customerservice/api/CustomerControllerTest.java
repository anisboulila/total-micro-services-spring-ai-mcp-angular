package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.error.CorrelationIdFilter;
import com.anisboulila.customerservice.error.RestProblemExceptionHandler;
import com.anisboulila.customerservice.security.CustomerSecurityConfiguration;
import com.anisboulila.customerservice.security.SecurityProblemWriter;
import com.anisboulila.customerservice.service.CustomerIdempotencyService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@ActiveProfiles("local")
@Import({CustomerSecurityConfiguration.class, SecurityProblemWriter.class, CorrelationIdFilter.class,
        RestProblemExceptionHandler.class, CustomerIdentityValidator.class})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomerIdempotencyService customerService;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode("customer-token")).thenReturn(jwt(List.of("CUSTOMER"), true));
    }

    @Test
    void createsProfileFromVerifiedIdentityAndReturnsLocation() throws Exception {
        UUID id = UUID.fromString("1346a219-911a-44ea-8750-005cb36eef2f");
        String body = "{\"id\":\"" + id + "\",\"name\":\"Ada\",\"email\":\"ada@example.test\","
                + "\"createdAt\":\"2026-01-01T00:00:00Z\"}";
        when(customerService.create("verified-sub", "Ada", "ADA@example.test", "key-1"))
                .thenReturn(new CustomerIdempotencyService.CreationResult(201, body, "/api/v1/customers/" + id));

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer customer-token")
                        .header("X-User-Id", "forged-sub")
                        .header("X-Roles", "OPERATOR")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Ada  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/customers/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@example.test"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.sub").doesNotExist());
        verify(customerService).create("verified-sub", "Ada", "ADA@example.test", "key-1");
    }

    @Test
    void rejectsOperatorEvenWithCustomerRoleAndInvalidBody() throws Exception {
        when(jwtDecoder.decode("operator-token")).thenReturn(jwt(List.of("OPERATOR"), true));
        when(jwtDecoder.decode("dual-token")).thenReturn(jwt(List.of("OPERATOR", "CUSTOMER"), true));
        for (String token : List.of("operator-token", "dual-token")) {
            mockMvc.perform(post("/api/v1/customers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\" \"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN_PROFILE_CREATION"));
        }
        verifyNoInteractions(customerService);
    }

    @Test
    void rejectsAuthenticatedUserWithoutCustomerRole() throws Exception {
        when(jwtDecoder.decode("other-token")).thenReturn(jwt(List.of("other"), true));
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer other-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(customerService);
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(customerService);
    }

    @Test
    void rejectsInvalidInputAndUnverifiedEmailWithoutSaving() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"sub\":\"forged\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        when(jwtDecoder.decode("unverified-token")).thenReturn(jwt(List.of("CUSTOMER"), false));
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer unverified-token")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        verifyNoInteractions(customerService);
    }

    @Test
    void translatesOnlyKnownUniqueConstraintsWithoutExposingTechnicalDetails() throws Exception {
        String[][] cases = {
                {"uk_customer_sub", "23505", "409", "CUSTOMER_PROFILE_EXISTS"},
                {"uk_customer_email_canonical", "23505", "409", "EMAIL_ALREADY_EXISTS"},
                {"other_constraint", "23505", "500", "INTERNAL_ERROR"},
                {"uk_customer_sub", "23514", "500", "INTERNAL_ERROR"}
        };
        for (String[] testCase : cases) {
            SQLException sql = new SQLException("secret SQL details", testCase[1]);
            ConstraintViolationException cause =
                    new ConstraintViolationException("database failure", sql, testCase[0]);
            doThrow(new DataIntegrityViolationException("secret database details", cause))
                    .when(customerService).create("verified-sub", "Ada", "ADA@example.test", "key-1");

            mockMvc.perform(post("/api/v1/customers")
                            .header("Authorization", "Bearer customer-token")
                            .header("Idempotency-Key", "key-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Ada\"}"))
                    .andExpect(status().is(Integer.parseInt(testCase[2])))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value(testCase[3]))
                    .andExpect(jsonPath("$.correlationId").isNotEmpty())
                    .andExpect(content().string(not(containsString("secret"))))
                    .andExpect(content().string(not(containsString(testCase[0]))));
        }

    }

    @Test
    void rejectsMissingBlankAndOversizedKeysWithoutSaving() throws Exception {
        for (String key : List.of("", "   ", "x".repeat(256))) {
            mockMvc.perform(post("/api/v1/customers")
                            .header("Authorization", "Bearer customer-token")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Ada\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
        }
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
        verifyNoInteractions(customerService);
    }

    @Test
    void acceptsKeyOfExactly255Characters() throws Exception {
        String key = "x".repeat(255);
        when(customerService.create("verified-sub", "Ada", "ADA@example.test", key))
                .thenReturn(new CustomerIdempotencyService.CreationResult(201, "{}", "/api/v1/customers/1"));
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer customer-token")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isCreated());
    }

    private static Jwt jwt(List<String> roles, boolean emailVerified) {
        return Jwt.withTokenValue("test-token").header("alg", "RS256")
                .claim("sub", "verified-sub")
                .claim("email", "ADA@example.test")
                .claim("email_verified", emailVerified)
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }
}
