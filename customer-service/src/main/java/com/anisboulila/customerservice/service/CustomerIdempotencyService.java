package com.anisboulila.customerservice.service;

import com.anisboulila.customerservice.api.CustomerResponse;
import com.anisboulila.customerservice.domain.Customer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class CustomerIdempotencyService {

    private static final String OPERATION = "POST /api/v1/customers";
    private final CustomerApplicationService customers;
    private final CustomerIdempotencyRepository idempotency;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CustomerIdempotencyService(CustomerApplicationService customers,
                                      CustomerIdempotencyRepository idempotency,
                                      ObjectMapper objectMapper, Clock clock) {
        this.customers = customers;
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public CreationResult create(String sub, String name, String email, String key) {
        // L'empreinte utilise les valeurs effectives, jamais le token ou la clé.
        String hash = fingerprint(new CanonicalRequest(OPERATION, sub, name,
                email.strip().toLowerCase(Locale.ROOT)));
        Customer customer = customers.createFromVerifiedIdentity(sub, name, email);
        String body = serialize(CustomerResponse.from(customer));
        String location = "/api/v1/customers/" + customer.getId();
        Instant now = clock.instant();
        // Les deux écritures partagent la transaction : un échec annule aussi le profil.
        idempotency.save(sub, OPERATION, key, hash, customer.getId(), 201, body, location,
                now, now.plus(Duration.ofDays(7)));
        return new CreationResult(body, location);
    }

    private String fingerprint(CanonicalRequest request) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(serialize(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize customer creation", exception);
        }
    }

    private record CanonicalRequest(String operation, String sub, String name, String email) {
    }

    public record CreationResult(String body, String location) {
    }
}
