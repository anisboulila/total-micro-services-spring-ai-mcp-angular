package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.error.ApiProblemException;
import com.anisboulila.customerservice.service.CustomerApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerPaginationConfigurationTest {

    @Test
    void usesConfiguredDefaultsAndMaximum() {
        CustomerApplicationService service = mock(CustomerApplicationService.class);
        when(service.findByVerifiedSubject("subject")).thenReturn(Optional.empty());
        CustomerListController controller = new CustomerListController(new CustomerIdentityValidator(), service, 5, 10);
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").claim("sub", "subject").build();
        assertEquals(5, controller.list(jwt, null, null).size());
        assertEquals(10, controller.list(jwt, "0", "10").size());
        assertEquals("INVALID_PAGE_SIZE",
                assertThrows(ApiProblemException.class, () -> controller.list(jwt, "0", "11")).getCode());
    }

    @Test
    void rejectsInvalidConfiguration() {
        CustomerApplicationService service = mock(CustomerApplicationService.class);
        CustomerIdentityValidator identity = new CustomerIdentityValidator();
        assertThrows(IllegalArgumentException.class, () -> new CustomerListController(identity, service, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new CustomerListController(identity, service, 11, 10));
        assertThrows(IllegalArgumentException.class, () -> new CustomerListController(identity, service, 1, 0));
    }
}
