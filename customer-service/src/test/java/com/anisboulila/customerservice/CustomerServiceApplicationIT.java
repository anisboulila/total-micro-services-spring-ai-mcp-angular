package com.anisboulila.customerservice;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/flyway-proof",
        "spring.flyway.schemas=cps_flyway_probe",
        "spring.flyway.default-schema=cps_flyway_probe",
        "spring.jpa.hibernate.ddl-auto=none"
})
class CustomerServiceApplicationIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void flywayAppliesAndRecordsTheVersionedMigrationOnPostgreSql() {
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

    @Test
    void applicationContextFailsWhenFlywayMigrationIsInvalid() {
        new ApplicationContextRunner()
                .withUserConfiguration(CustomerServiceApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=" + postgres.getJdbcUrl(),
                        "spring.datasource.username=" + postgres.getUsername(),
                        "spring.datasource.password=" + postgres.getPassword(),
                        "spring.flyway.enabled=true",
                        "spring.flyway.validate-on-migrate=true",
                        "spring.flyway.create-schemas=true",
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.flyway.locations=classpath:db/flyway-invalid",
                        "spring.flyway.schemas=cps_flyway_invalid",
                        "spring.flyway.default-schema=cps_flyway_invalid")
                .run(context -> {
                    Throwable startupFailure = context.getStartupFailure();
                    assertNotNull(startupFailure);
                    assertTrue(hasCause(startupFailure, FlywayException.class));
                });
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
