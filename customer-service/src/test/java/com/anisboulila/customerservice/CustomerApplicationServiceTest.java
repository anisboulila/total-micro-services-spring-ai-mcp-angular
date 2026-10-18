package com.anisboulila.customerservice;

import com.anisboulila.customerservice.domain.Customer;
import com.anisboulila.customerservice.repository.CustomerRepository;
import com.anisboulila.customerservice.service.CustomerApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void createsCanonicalCustomerWithServiceTimestamp() {
        Instant now = Instant.parse("2026-10-16T15:00:00Z");
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository, Clock.fixed(now, ZoneOffset.UTC));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = service.createFromVerifiedIdentity(
                "oidc-subject", "  Ada Lovelace  ", "  ADA@Example.Test ");

        ArgumentCaptor<Customer> savedCustomer = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(savedCustomer.capture());
        assertSame(customer, savedCustomer.getValue());
        assertEquals("Ada Lovelace", customer.getName());
        assertEquals("ada@example.test", customer.getEmail());
        assertEquals(now, customer.getCreatedAt());
    }

    @Test
    void rejectsInvalidNameBeforeCallingRepository() {
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository, Clock.systemUTC());

        assertThrows(IllegalArgumentException.class,
                () -> service.createFromVerifiedIdentity("oidc-subject", "   ", "a@example.test"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createFromVerifiedIdentity("oidc-subject",
                        "n".repeat(121), "a@example.test"));
        verifyNoInteractions(customerRepository);
    }

    @Test
    void leavesIdentifierGenerationToPostgres() {
        Customer customer = Customer.create(
                "oidc-subject", "Ada", "ada@example.test", Instant.EPOCH);

        assertNull(customer.getId());
    }
}
