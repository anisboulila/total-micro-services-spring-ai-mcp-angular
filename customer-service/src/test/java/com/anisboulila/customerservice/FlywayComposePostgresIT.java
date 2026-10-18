package com.anisboulila.customerservice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/flyway-proof",
        "spring.flyway.schemas=cps_flyway_probe",
        "spring.flyway.default-schema=cps_flyway_probe",
        "spring.jpa.hibernate.ddl-auto=none"
})
class FlywayComposePostgresIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesAndRecordsTheVersionedMigrationOnComposePostgreSql() {
        assertTrue(jdbcTemplate.queryForObject("SELECT version()", String.class)
                .startsWith("PostgreSQL"));
        assertNotNull(flyway.info().current());
        assertEquals("1", flyway.info().current().getVersion().getVersion());

        Integer historyRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_flyway_probe.flyway_schema_history "
                        + "WHERE version = '1' AND success = TRUE",
                Integer.class);
        assertEquals(1, historyRows);
    }
}
