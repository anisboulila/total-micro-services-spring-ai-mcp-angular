package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.error.CorrelationIdFilter;
import com.anisboulila.customerservice.error.RestProblemExceptionHandler;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(controllers = CreateCustomerRequestTest.ValidationProbeController.class)
@Import({RestProblemExceptionHandler.class, CorrelationIdFilter.class,
        CustomerIdentityValidator.class, CreateCustomerRequestTest.ProbeSecurity.class,
        CreateCustomerRequestTest.ValidationProbeController.class})
class CreateCustomerRequestTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsAndNormalizesNamesAtBothBoundaries() throws Exception {
        assertName("  A  ", "A");
        assertName("  " + "x".repeat(120) + "  ", "x".repeat(120));
    }

    @Test
    void rejectsMissingBlankAndOversizedNames() throws Exception {
        assertInvalid("{}");
        assertInvalid("{\"name\":null}");
        assertInvalid("{\"name\":\"   \"}");
        assertInvalid("{\"name\":\"  " + "x".repeat(121) + "  \"}");
    }

    @Test
    void rejectsServerControlledUnknownAndIncorrectlyTypedFields() throws Exception {
        assertInvalid("{\"name\":\"Ada\",\"sub\":\"forged\"}");
        assertInvalid("{\"name\":\"Ada\",\"sub\":null}");
        assertInvalid("{\"name\":\"Ada\",\"email\":\"forged@example.test\"}");
        assertInvalid("{\"name\":\"Ada\",\"email\":null}");
        assertInvalid("{\"name\":\"Ada\",\"email_verified\":true}");
        assertInvalid("{\"name\":\"Ada\",\"customerId\":\"forged\"}");
        assertInvalid("{\"name\":\"Ada\",\"other\":\"value\"}");
        assertInvalid("{\"name\":123}");
        assertInvalid("{\"name\":\"Ada\",\"name\":\"Other\"}");
        assertInvalid("[]");
        assertInvalid("{");
    }

    @Test
    void identifiesReservedFieldWithoutReturningItsValue() throws Exception {
        mockMvc.perform(post("/test/customer-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\",\"email\":\"private@example.test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].code").value("RESERVED"))
                .andExpect(content().string(not(containsString("private@example.test"))));
    }

    @Test
    void returnsStructuredErrorForUnverifiedEmail() throws Exception {
        mockMvc.perform(post("/test/customer-validation")
                        .header("X-Test-Email-Verified", "false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.correlationId").isString());
    }

    private void assertName(String input, String expected) throws Exception {
        mockMvc.perform(post("/test/customer-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + input + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(expected));
    }

    private void assertInvalid(String body) throws Exception {
        mockMvc.perform(post("/test/customer-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.correlationId").isString());
    }

    @RestController
    static class ValidationProbeController {
        private final CustomerIdentityValidator identityValidator;

        ValidationProbeController(CustomerIdentityValidator identityValidator) {
            this.identityValidator = identityValidator;
        }

        @PostMapping("/test/customer-validation")
        Map<String, String> validate(@Valid @RequestBody CreateCustomerRequest request,
                                     @RequestHeader(value = "X-Test-Email-Verified", defaultValue = "true")
                                     boolean emailVerified) {
            Jwt jwt = Jwt.withTokenValue("test-only").header("alg", "none")
                    .claim("sub", "test-subject")
                    .claim("email", "test@example.test")
                    .claim("email_verified", emailVerified)
                    .build();
            identityValidator.validate(jwt);
            return Map.of("name", request.name());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeSecurity {
        @Bean
        SecurityFilterChain validationProbeSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/test/customer-validation").permitAll()
                            .anyRequest().denyAll())
                    .build();
        }
    }
}
