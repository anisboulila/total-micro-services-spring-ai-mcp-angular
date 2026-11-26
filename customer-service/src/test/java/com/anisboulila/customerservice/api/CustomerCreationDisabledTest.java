package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.error.CorrelationIdFilter;
import com.anisboulila.customerservice.error.RestProblemExceptionHandler;
import com.anisboulila.customerservice.security.CustomerSecurityConfiguration;
import com.anisboulila.customerservice.security.SecurityProblemWriter;
import com.anisboulila.customerservice.service.CustomerIdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@Import({CustomerSecurityConfiguration.class, SecurityProblemWriter.class, CorrelationIdFilter.class,
        RestProblemExceptionHandler.class, CustomerIdentityValidator.class})
class CustomerCreationDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomerIdempotencyService customerService;

    @Test
    void creationIsDisabledByDefaultWithoutSaving() throws Exception {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "RS256")
                .claim("sub", "verified-sub")
                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                .build();
        when(jwtDecoder.decode("test-token")).thenReturn(jwt);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROFILE_CREATION_DISABLED"));
        verifyNoInteractions(customerService);
    }
}
