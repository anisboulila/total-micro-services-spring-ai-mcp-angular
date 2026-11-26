package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.domain.Customer;

import java.time.Instant;
import java.util.UUID;

// Le contrat HTTP reste distinct de l'entité et ne révèle pas le sujet OIDC.
public record CustomerResponse(UUID id, String name, String email, Instant createdAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getCreatedAt());
    }
}
