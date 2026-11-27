package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.domain.Customer;
import com.anisboulila.customerservice.error.CorrelationIdFilter;
import com.anisboulila.customerservice.error.RestProblemExceptionHandler;
import com.anisboulila.customerservice.security.CustomerSecurityConfiguration;
import com.anisboulila.customerservice.security.SecurityProblemWriter;
import com.anisboulila.customerservice.service.CustomerApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerListController.class)
@Import({CustomerSecurityConfiguration.class, SecurityProblemWriter.class, CorrelationIdFilter.class,
        RestProblemExceptionHandler.class, CustomerIdentityValidator.class})
class CustomerListControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CustomerApplicationService customers;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode("customer")).thenReturn(jwt("subject-one", List.of("CUSTOMER")));
        when(jwtDecoder.decode("operator")).thenReturn(jwt("operator", List.of("OPERATOR")));
        when(jwtDecoder.decode("dual")).thenReturn(jwt("dual", List.of("CUSTOMER", "OPERATOR")));
        when(jwtDecoder.decode("other")).thenReturn(jwt("other", List.of("OTHER")));
        when(jwtDecoder.decode("blank")).thenReturn(jwt(" ", List.of("CUSTOMER")));
    }

    @Test
    void returnsOnlyVerifiedSubjectDespiteForgedParametersAndHeaders() throws Exception {
        when(customers.findByVerifiedSubject("subject-one")).thenReturn(Optional.of(
                Customer.create("subject-one", "Alice", "alice@example.test", Instant.EPOCH)));
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer customer")
                        .header("X-User-Id", "subject-two").param("sub", "subject-two"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alice"))
                .andExpect(jsonPath("$.content[0].sub").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        verify(customers).findByVerifiedSubject("subject-one");
    }

    @Test
    void returnsEmptyPageForMissingProfileAndOutOfRangePage() throws Exception {
        when(customers.findByVerifiedSubject("subject-one")).thenReturn(Optional.empty())
                .thenReturn(Optional.of(Customer.create("subject-one", "Alice", "alice@example.test", Instant.EPOCH)));
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer customer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0));
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer customer")
                        .param("page", "1").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void rejectsInvalidPaginationWithoutCallingService() throws Exception {
        for (String page : List.of("-1", "abc", "", "2147483648")) {
            mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer customer").param("page", page))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAGE"));
        }
        for (String size : List.of("-1", "0", "101", "abc", "", "2147483648")) {
            mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer customer").param("size", size))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
        }
        verifyNoInteractions(customers);
    }

    @Test
    void rejectsUnauthorizedRolesAndInvalidSubject() throws Exception {
        mockMvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
        for (String token : List.of("operator", "dual", "other")) {
            mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer blank"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(customers);
    }

    private static Jwt jwt(String sub, List<String> roles) {
        return Jwt.withTokenValue("test").header("alg", "RS256")
                .claim("sub", sub).claim("realm_access", Map.of("roles", roles)).build();
    }
}
