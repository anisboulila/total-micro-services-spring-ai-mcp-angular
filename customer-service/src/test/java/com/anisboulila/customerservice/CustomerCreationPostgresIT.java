package com.anisboulila.customerservice;

import com.anisboulila.customerservice.service.CustomerIdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "customer.profile-creation.enabled=true",
        "spring.flyway.schemas=cps_customer_schema",
        "spring.flyway.default-schema=cps_customer_schema",
        "spring.jpa.properties.hibernate.default_schema=cps_customer_schema"
})
@AutoConfigureMockMvc
class CustomerCreationPostgresIT {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private Clock clock;

    @MockitoSpyBean
    private CustomerIdempotencyRepository idempotencyRepository;

    @BeforeEach
    void setUp() {
        // Cette suppression concerne exclusivement le conteneur PostgreSQL créé pour ce test.
        jdbcTemplate.update("DELETE FROM cps_customer_schema.customer");
        when(clock.instant()).thenReturn(START);
        when(jwtDecoder.decode("first")).thenReturn(jwt("subject-one", "ada@example.test", "CUSTOMER"));
        when(jwtDecoder.decode("same-sub")).thenReturn(jwt("subject-one", "other@example.test", "CUSTOMER"));
        when(jwtDecoder.decode("same-email")).thenReturn(jwt("subject-two", " ADA@EXAMPLE.TEST ", "CUSTOMER"));
        when(jwtDecoder.decode("operator")).thenReturn(jwt("operator", "operator@example.test", "OPERATOR"));
        when(jwtDecoder.decode("second")).thenReturn(jwt("subject-two", "grace@example.test", "CUSTOMER"));
    }

    @Test
    void createsProfileAndRejectsBothDuplicateIdentitiesWithoutChangingIt(CapturedOutput output) throws Exception {
        var created = mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer first")
                        .header("Idempotency-Key", "test-key")
                        .contentType("application/json")
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@example.test"))
                .andReturn();

        Map<String, Object> initial = jdbcTemplate.queryForMap(
                "SELECT id, sub, name, email, created_at FROM cps_customer_schema.customer WHERE sub = ?", "subject-one");
        assertEquals("/api/v1/customers/" + initial.get("id"), created.getResponse().getHeader("Location"));
        Map<String, Object> saved = jdbcTemplate.queryForMap(
                "SELECT * FROM cps_customer_schema.customer_idempotency WHERE actor_sub = ?", "subject-one");
        assertEquals("POST /api/v1/customers", saved.get("operation"));
        assertEquals("test-key", saved.get("idempotency_key"));
        assertEquals(initial.get("id"), saved.get("customer_id"));
        assertEquals(201, saved.get("response_status"));
        assertEquals(created.getResponse().getContentAsString(), saved.get("response_body"));
        assertEquals(created.getResponse().getHeader("Location"), saved.get("response_location"));
        assertEquals("aadee19662427801d7ae14c2159c77787c4b410dfe34ae00724c2c4cfe279441",
                saved.get("request_hash"));
        assertEquals(Duration.ofDays(7), Duration.between(
                ((Timestamp) saved.get("created_at")).toInstant(),
                ((Timestamp) saved.get("expires_at")).toInstant()));

        assertConflict("same-sub", "CUSTOMER_PROFILE_EXISTS");
        assertUnchanged(initial);
        assertConflict("same-email", "EMAIL_ALREADY_EXISTS");
        assertUnchanged(initial);
        assertFalse(output.getOut().contains("subject-one"));
        assertFalse(output.getOut().contains("ada@example.test"));
    }

    @Test
    void operatorCannotCreateProfile() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer operator")
                        .contentType("application/json")
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_PROFILE_CREATION"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
    }

    @Test
    void rollsBackCustomerAndIdempotencyRecordWhenFailureOccursAfterBothInserts() throws Exception {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("simulated failure after idempotency insert");
        }).when(idempotencyRepository).save(anyString(), anyString(), anyString(), anyString(),
                any(), anyInt(), anyString(), anyString(), any(), any());

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer first")
                        .header("Idempotency-Key", "rollback-key")
                        .contentType("application/json")
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isInternalServerError());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer_idempotency", Integer.class));
    }

    @Test
    void uniqueKeyIsScopedToActorAndOperation() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer first")
                        .header("Idempotency-Key", "shared-key")
                        .contentType("application/json")
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isCreated());

        String copy = """
                INSERT INTO cps_customer_schema.customer_idempotency
                    (actor_sub, operation, idempotency_key, request_hash, customer_id,
                     response_status, response_body, response_location, created_at, expires_at)
                SELECT ?, ?, idempotency_key, request_hash, customer_id, response_status,
                       response_body, response_location, created_at, expires_at
                  FROM cps_customer_schema.customer_idempotency
                 WHERE actor_sub = 'subject-one' AND operation = 'POST /api/v1/customers'
                """;
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(copy, "subject-one", "POST /api/v1/customers"));
        assertEquals(1, jdbcTemplate.update(copy, "subject-two", "POST /api/v1/customers"));
        assertEquals(1, jdbcTemplate.update(copy, "subject-one", "POST /another-operation"));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer_idempotency", Integer.class));
    }

    @Test
    void replaysExactResponseRejectsDifferentContentAndScopesKeyByActor() throws Exception {
        var first = create("first", "shared-key", "{\"name\":\"Ada\"}")
                .andExpect(status().isCreated()).andReturn().getResponse();
        var replay = create("first", "shared-key", "{ \"name\" : \"  Ada  \" }")
                .andExpect(status().isCreated()).andReturn().getResponse();
        assertEquals(first.getStatus(), replay.getStatus());
        assertEquals(first.getContentAsString(), replay.getContentAsString());
        assertEquals(first.getHeader("Location"), replay.getHeader("Location"));
        create("first", "shared-key", "{\"name\":\"Grace\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        create("same-sub", "shared-key", "{\"name\":\"Ada\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        create("second", "shared-key", "{\"name\":\"Grace\"}")
                .andExpect(status().isCreated());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer_idempotency", Integer.class));
    }

    @Test
    void expiresExactlyAtSevenDaysWithoutRemovingExistingProfile() throws Exception {
        var first = create("first", "expiry-key", "{\"name\":\"Ada\"}")
                .andExpect(status().isCreated()).andReturn().getResponse();
        when(clock.instant()).thenReturn(START.plus(Duration.ofDays(7)).minusMillis(1));
        var replay = create("first", "expiry-key", "{\"name\":\"Ada\"}")
                .andExpect(status().isCreated()).andReturn().getResponse();
        assertEquals(first.getContentAsString(), replay.getContentAsString());

        when(clock.instant()).thenReturn(START.plus(Duration.ofDays(7)));
        create("first", "expiry-key", "{\"name\":\"Ada\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_PROFILE_EXISTS"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
        // Le conflit annule aussi la suppression de la clé expirée.
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer_idempotency", Integer.class));
    }

    @Test
    void expiredRecordCanBeReplacedWhenNewCreationIsPermitted() throws Exception {
        create("first", "expiry-key", "{\"name\":\"Ada\"}").andExpect(status().isCreated());
        // Simuler une ancienne clé pour un acteur qui n'a pas encore de profil.
        jdbcTemplate.update("UPDATE cps_customer_schema.customer_idempotency SET actor_sub = ? WHERE actor_sub = ?",
                "subject-two", "subject-one");
        when(clock.instant()).thenReturn(START.plus(Duration.ofDays(7)));
        create("second", "expiry-key", "{\"name\":\"Grace\"}").andExpect(status().isCreated());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer_idempotency", Integer.class));
        assertEquals(Timestamp.from(START.plus(Duration.ofDays(14))), jdbcTemplate.queryForObject(
                "SELECT expires_at FROM cps_customer_schema.customer_idempotency WHERE actor_sub = ?",
                Timestamp.class, "subject-two"));
    }

    @Test
    void rejectedValidationDoesNotConsumeKey() throws Exception {
        create("first", "retry-key", "{\"name\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer_idempotency", Integer.class));
        create("first", "retry-key", "{\"name\":\"Ada\"}").andExpect(status().isCreated());
    }

    @Test
    void concurrentIdenticalRequestsWaitAndReplayExactly() throws Exception {
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        pauseFirstInsert(inserted, release, false);
        var executor = Executors.newFixedThreadPool(4);
        try {
            Future<MockHttpServletResponse> first = executor.submit(() -> response("first", "race-key", "Ada"));
            assertTrue(inserted.await(10, TimeUnit.SECONDS), "first transaction did not insert");
            List<Future<MockHttpServletResponse>> retries = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                retries.add(executor.submit(() -> response("first", "race-key", "Ada")));
            }
            awaitAdvisoryWaiters(3);
            release.countDown();
            var original = first.get(10, TimeUnit.SECONDS);
            assertEquals(201, original.getStatus());
            for (var retry : retries) {
                var replay = retry.get(10, TimeUnit.SECONDS);
                assertEquals(original.getStatus(), replay.getStatus());
                assertEquals(original.getContentAsString(), replay.getContentAsString());
                assertEquals(original.getHeader("Location"), replay.getHeader("Location"));
            }
            assertCounts(1, 1);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentDifferentContentWaitsThenReturnsConflict() throws Exception {
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        pauseFirstInsert(inserted, release, false);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> response("first", "race-key", "Ada"));
            assertTrue(inserted.await(10, TimeUnit.SECONDS));
            var different = executor.submit(() -> response("first", "race-key", "Grace"));
            awaitAdvisoryWaiters(1);
            release.countDown();
            assertEquals(201, first.get(10, TimeUnit.SECONDS).getStatus());
            var conflict = different.get(10, TimeUnit.SECONDS);
            assertEquals(409, conflict.getStatus());
            assertTrue(conflict.getContentAsString().contains("IDEMPOTENCY_KEY_REUSED"));
            assertCounts(1, 1);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void waitingRequestCanCreateAfterFirstTransactionRollsBack() throws Exception {
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        pauseFirstInsert(inserted, release, true);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> response("first", "race-key", "Ada"));
            assertTrue(inserted.await(10, TimeUnit.SECONDS));
            var retry = executor.submit(() -> response("first", "race-key", "Ada"));
            awaitAdvisoryWaiters(1);
            release.countDown();
            assertEquals(500, first.get(10, TimeUnit.SECONDS).getStatus());
            var successful = retry.get(10, TimeUnit.SECONDS);
            assertEquals(201, successful.getStatus());
            var replay = response("first", "race-key", "Ada");
            assertEquals(successful.getContentAsString(), replay.getContentAsString());
            assertEquals(successful.getHeader("Location"), replay.getHeader("Location"));
            assertCounts(1, 1);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void differentActorWithSameKeyCanProceedWhileFirstTransactionIsOpen() throws Exception {
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        pauseFirstInsert(inserted, release, false);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> response("first", "race-key", "Ada"));
            assertTrue(inserted.await(10, TimeUnit.SECONDS));
            var otherActor = executor.submit(() -> response("second", "race-key", "Grace"));
            assertEquals(201, otherActor.get(10, TimeUnit.SECONDS).getStatus());
            release.countDown();
            assertEquals(201, first.get(10, TimeUnit.SECONDS).getStatus());
            assertCounts(2, 2);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentRequestsReplaceExpiredRecordOnlyOnce() throws Exception {
        create("first", "expiry-race", "{\"name\":\"Ada\"}").andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE cps_customer_schema.customer_idempotency SET actor_sub = ? WHERE actor_sub = ?",
                "subject-two", "subject-one");
        when(clock.instant()).thenReturn(START.plus(Duration.ofDays(7)));
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        pauseFirstInsert(inserted, release, false);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> response("second", "expiry-race", "Grace"));
            assertTrue(inserted.await(10, TimeUnit.SECONDS));
            var retry = executor.submit(() -> response("second", "expiry-race", "Grace"));
            awaitAdvisoryWaiters(1);
            release.countDown();
            var original = first.get(10, TimeUnit.SECONDS);
            var replay = retry.get(10, TimeUnit.SECONDS);
            assertEquals(201, original.getStatus());
            assertEquals(original.getStatus(), replay.getStatus());
            assertEquals(original.getContentAsString(), replay.getContentAsString());
            assertEquals(original.getHeader("Location"), replay.getHeader("Location"));
            assertCounts(2, 1);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private void pauseFirstInsert(CountDownLatch inserted, CountDownLatch release, boolean fail) {
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (calls.getAndIncrement() == 0) {
                inserted.countDown();
                if (!release.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting to release transaction");
                }
                if (fail) {
                    throw new IllegalStateException("simulated failure after idempotency insert");
                }
            }
            return result;
        }).when(idempotencyRepository).save(anyString(), anyString(), anyString(), anyString(),
                any(), anyInt(), anyString(), anyString(), any(), any());
    }

    private void awaitAdvisoryWaiters(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted", Integer.class);
            if (count != null && count >= expected) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Expected " + expected + " PostgreSQL advisory lock waiters");
    }

    private MockHttpServletResponse response(String token, String key, String name) throws Exception {
        return create(token, key, "{\"name\":\"" + name + "\"}").andReturn().getResponse();
    }

    private void assertCounts(int customers, int records) {
        assertEquals(customers, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
        assertEquals(records, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cps_customer_schema.customer_idempotency", Integer.class));
    }

    private org.springframework.test.web.servlet.ResultActions create(String token, String key, String body)
            throws Exception {
        return mockMvc.perform(post("/api/v1/customers")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(body));
    }

    private void assertConflict(String token, String code) throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "other-key")
                        .contentType("application/json")
                        .content("{\"name\":\"Grace\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    private void assertUnchanged(Map<String, Object> initial) {
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cps_customer_schema.customer", Integer.class));
        assertEquals(initial, jdbcTemplate.queryForMap(
                "SELECT id, sub, name, email, created_at FROM cps_customer_schema.customer WHERE sub = ?", "subject-one"));
    }

    private static Jwt jwt(String sub, String email, String role) {
        return Jwt.withTokenValue("test-token").header("alg", "RS256")
                .claim("sub", sub)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
