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
import java.util.Optional;
import java.util.List;
import java.util.UUID;

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
        when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = service.createFromVerifiedIdentity(
                "oidc-subject", "  Ada Lovelace  ", "  ADA@Example.Test ");

        ArgumentCaptor<Customer> savedCustomer = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).saveAndFlush(savedCustomer.capture());
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

    @Test
    void retrievesOnlyByVerifiedSubject() {
        CustomerApplicationService service = new CustomerApplicationService(customerRepository, Clock.systemUTC());
        Customer customer = Customer.create("subject", "Ada", "ada@example.test", Instant.EPOCH);
        when(customerRepository.findBySub("subject")).thenReturn(Optional.of(customer));
        assertEquals(Optional.of(customer), service.findByVerifiedSubject("subject"));
        verify(customerRepository).findBySub("subject");
    }

    @Test
    void selectsRepositoryQueryAccordingToReadPermission() {
        CustomerApplicationService service = new CustomerApplicationService(customerRepository, Clock.systemUTC());
        UUID id = UUID.fromString("12345678-1234-1234-1234-123456789012");
        service.findReadable(id, "subject", false);
        service.findReadable(id, "subject", true);
        verify(customerRepository).findByIdAndSub(id, "subject");
        verify(customerRepository).findById(id);
    }

    @Test
    void largeOffsetUsesPostgresQueryRatherThanAssumingPageIsEmpty() {
        CustomerApplicationService service = new CustomerApplicationService(customerRepository, Clock.systemUTC());
        long offset = 2_147_483_648L;
        Customer customer = Customer.create("subject", "Ada", "ada@example.test", Instant.EPOCH);
        when(customerRepository.findPageWithLongOffset(2, offset)).thenReturn(List.of(customer));
        when(customerRepository.count()).thenReturn(offset + 1);
        var page = service.listForOperator(1_073_741_824, 2);
        assertEquals(List.of(customer), page.getContent());
        assertEquals(offset + 1, page.getTotalElements());
    }
}
