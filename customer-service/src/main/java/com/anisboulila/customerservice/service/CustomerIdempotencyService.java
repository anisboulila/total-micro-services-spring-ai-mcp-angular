package com.anisboulila.customerservice.service;

import com.anisboulila.customerservice.api.CustomerResponse;
import com.anisboulila.customerservice.domain.Customer;
import com.anisboulila.customerservice.error.ApiProblemException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.http.HttpStatus;

import java.nio.ByteBuffer;
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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CreationResult create(String sub, String name, String email, String key) {
        // L'empreinte utilise les valeurs effectives, jamais le token ou la clé.
        String hash = fingerprint(new CanonicalRequest(OPERATION, sub, name,
                email.strip().toLowerCase(Locale.ROOT)));
        // Toutes les instances prennent le même verrou avant de rechercher ou créer le résultat.
        idempotency.lock(lockId(sub, OPERATION, key));
        var stored = idempotency.findForUpdate(sub, OPERATION, key);
        Instant now = clock.instant();
        if (stored.isPresent()) {
            var previous = stored.get();
            if (previous.expiresAt().isAfter(now)) {
                if (!previous.hash().equals(hash)) {
                    throw new ApiProblemException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                            "Idempotency key reused", "The idempotency key was already used for a different request.");
                }
                // Rejouer le résultat enregistré sans refaire l'insertion du profil.
                return new CreationResult(previous.status(), previous.body(), previous.location());
            }
            // La suppression et une éventuelle nouvelle création sont dans la même transaction.
            idempotency.delete(sub, OPERATION, key);
        }
        Customer customer = customers.createFromVerifiedIdentity(sub, name, email);
        String body = serialize(CustomerResponse.from(customer));
        String location = "/api/v1/customers/" + customer.getId();
        now = clock.instant();
        // Les deux écritures partagent la transaction : un échec annule aussi le profil.
        idempotency.save(sub, OPERATION, key, hash, customer.getId(), 201, body, location,
                now, now.plus(Duration.ofDays(7)));
        return new CreationResult(201, body, location);
    }

    private String fingerprint(CanonicalRequest request) {
        return HexFormat.of().formatHex(digest(request));
    }

    private byte[] digest(Object value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(serialize(value).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private long lockId(String sub, String operation, String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Le préfixage par longueur rend l'encodage stable et évite les concaténations ambiguës.
            for (String part : new String[] {"customer-idempotency-v1", sub, operation, key}) {
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return ByteBuffer.wrap(digest.digest()).getLong();
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

    public record CreationResult(int status, String body, String location) {
    }
}
