package com.anisboulila.customerservice.domain;

import com.anisboulila.customerservice.persistence.PostgresGeneratedUuid;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer")
@Access(AccessType.FIELD)
public class Customer {

    @Id
    @PostgresGeneratedUuid
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "sub", nullable = false, columnDefinition = "text")
    private String sub;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, columnDefinition = "text")
    private String email;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    protected Customer() {
    }

    private Customer(String sub, String name, String email, Instant createdAt) {
        this.sub = Objects.requireNonNull(sub, "sub");
        this.name = normalizeName(name);
        this.email = normalizeEmail(email);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Customer create(String sub, String name, String email, Instant createdAt) {
        return new Customer(sub, name, email, createdAt);
    }

    private static String normalizeName(String name) {
        String normalized = Objects.requireNonNull(name, "name").strip();
        if (normalized.isEmpty() || normalized.length() > 120) {
            throw new IllegalArgumentException("name must contain between 1 and 120 characters");
        }
        return normalized;
    }

    private static String normalizeEmail(String email) {
        String normalized = Objects.requireNonNull(email, "email").strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getSub() {
        return sub;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
