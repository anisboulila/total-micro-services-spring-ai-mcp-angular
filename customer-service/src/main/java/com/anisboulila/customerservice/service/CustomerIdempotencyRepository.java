package com.anisboulila.customerservice.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class CustomerIdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String table;

    public CustomerIdempotencyRepository(JdbcTemplate jdbcTemplate,
            @Value("${spring.jpa.properties.hibernate.default_schema:public}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        if (!schema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid customer database schema");
        }
        this.table = "\"" + schema + "\".customer_idempotency";
    }

    public void save(String sub, String operation, String key, String hash, UUID customerId,
                     int status, String body, String location, Instant createdAt, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO %s
                    (actor_sub, operation, idempotency_key, request_hash, customer_id,
                     response_status, response_body, response_location, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(table), sub, operation, key, hash, customerId, status, body, location,
                Timestamp.from(createdAt), Timestamp.from(expiresAt));
    }
}
