package com.anisboulila.customerservice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.schemas=cps_customer_schema",
        "spring.flyway.default-schema=cps_customer_schema",
        "spring.jpa.properties.hibernate.default_schema=cps_customer_schema"
})
@Transactional
class CustomerSchemaComposePostgresIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesCustomerMigrationAndCreatesExpectedColumnsAndIndexes() {
        assertNotNull(flyway.info().current());
        assertEquals("1", flyway.info().current().getVersion().getVersion());

        Integer historyRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.flyway_schema_history "
                        + "WHERE version = '1' AND success = TRUE",
                Integer.class);
        assertEquals(1, historyRows);

        Integer customerColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'cps_customer_schema' "
                        + "AND table_name = 'customer'",
                Integer.class);
        assertEquals(5, customerColumns);

        String emailIndex = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes "
                        + "WHERE schemaname = 'cps_customer_schema' "
                        + "AND tablename = 'customer' "
                        + "AND indexname = 'uk_customer_email_canonical'",
                String.class);
        assertTrue(emailIndex.startsWith("CREATE UNIQUE INDEX"));
        assertTrue(emailIndex.contains("lower(btrim(email))"));
    }

    @Test
    void acceptsAValidCustomerRecordAndGeneratesItsId() {
        String sub = UUID.randomUUID().toString();
        insertCustomer(sub, "Ada Lovelace", "ada@example.test");

        UUID id = jdbcTemplate.queryForObject(
                "SELECT id FROM cps_customer_schema.customer WHERE sub = ?",
                UUID.class,
                sub);
        assertNotNull(id);
    }

    @Test
    void rejectsDuplicateSub() {
        String sub = UUID.randomUUID().toString();
        insertCustomer(sub, "Ada Lovelace", "ada@example.test");

        assertThrows(DataIntegrityViolationException.class,
                () -> insertCustomer(sub, "Grace Hopper", "grace@example.test"));
    }

    @Test
    void rejectsEmailDuplicateRegardlessOfCaseAndSurroundingSpaces() {
        insertCustomer(UUID.randomUUID().toString(), "Ada Lovelace", "ada@example.test");

        assertThrows(DataIntegrityViolationException.class,
                () -> insertCustomer(UUID.randomUUID().toString(),
                        "Grace Hopper", "  ADA@EXAMPLE.TEST  "));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertCustomer(UUID.randomUUID().toString(), "   ", "blank@example.test"));
    }

    @Test
    void rejectsOverlongName() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertCustomer(UUID.randomUUID().toString(),
                        "n".repeat(121), "long@example.test"));
    }

    private void insertCustomer(String sub, String name, String email) {
        jdbcTemplate.update(
                "INSERT INTO cps_customer_schema.customer (sub, name, email, created_at) "
                        + "VALUES (?, ?, ?, ?)",
                sub,
                name,
                email,
                OffsetDateTime.now(ZoneOffset.UTC));
    }
}
