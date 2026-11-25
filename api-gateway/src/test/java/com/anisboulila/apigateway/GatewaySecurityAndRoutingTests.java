package com.anisboulila.apigateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.web.exposure.include=health"
        })
class GatewaySecurityAndRoutingTests {

    private static final String ISSUER = "http://issuer.test/realms/customer-dev";
    private static final RSAKey SIGNING_KEY = generateKey("gateway-test-key");
    private static final RSAKey FOREIGN_KEY = generateKey("foreign-test-key");
    private static final AtomicInteger downstreamRequestCount = new AtomicInteger();
    private static final AtomicReference<String> downstreamAuthorization = new AtomicReference<>();
    private static final AtomicReference<String> downstreamPath = new AtomicReference<>();
    private static final AtomicReference<String> downstreamMethod = new AtomicReference<>();
    private static final HttpServer downstream = startDownstream();

    @LocalServerPort
    private int gatewayPort;

    @LocalManagementPort
    private int managementPort;

    private WebTestClient client;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry properties) {
        properties.add("gateway.security.jwt.issuer-uri", () -> ISSUER);
        properties.add("gateway.security.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + downstream.getAddress().getPort() + "/jwks");
        properties.add("gateway.security.jwt.audience", () -> "customer-service");
        properties.add("gateway.routes.customer-service-uri",
                () -> "http://127.0.0.1:" + downstream.getAddress().getPort());
    }

    @BeforeEach
    void setUp() {
        downstreamRequestCount.set(0);
        downstreamAuthorization.set(null);
        downstreamPath.set(null);
        downstreamMethod.set(null);
        client = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + gatewayPort)
                .build();
    }

    @AfterAll
    static void stopDownstream() {
        downstream.stop(0);
    }

    @Test
    void servesHealthOnlyOnTheManagementPortWithoutAuthentication() {
        WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + managementPort)
                .build()
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");

        client.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isNotFound();

        assertThat(downstreamRequestCount).hasValue(0);
    }

    @Test
    void rejectsRequestsWithoutTokenEvenWhenIdentityHeadersAreForged() {
        client.get()
                .uri("/api/v1/customers")
                .header("X-User-Id", "forged-user")
                .header("X-Roles", "OPERATOR")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(downstreamRequestCount).hasValue(0);
    }

    @Test
    void rejectsTokenSignedByAnUntrustedKey() throws Exception {
        String invalidToken = createToken(ISSUER, "customer-service", Instant.now().plusSeconds(120),
                Instant.now().minusSeconds(1), FOREIGN_KEY);

        client.get()
                .uri("/api/v1/customers")
                .headers(headers -> headers.setBearerAuth(invalidToken))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(downstreamRequestCount).hasValue(0);
    }

    @Test
    void rejectsTokensWithWrongIssuerExpiredValidityOrAudience() throws Exception {
        assertUnauthorized(createToken("http://other-issuer.test/realm", "customer-service",
                Instant.now().plusSeconds(120), Instant.now().minusSeconds(1), SIGNING_KEY));
        assertUnauthorized(createToken(ISSUER, "customer-service",
                Instant.now().minusSeconds(1), Instant.now().minusSeconds(120), SIGNING_KEY));
        assertUnauthorized(createToken(ISSUER, "customer-service",
                Instant.now().plusSeconds(120), Instant.now().plusSeconds(120), SIGNING_KEY));
        assertUnauthorized(createToken(ISSUER, "another-service",
                Instant.now().plusSeconds(120), Instant.now().minusSeconds(1), SIGNING_KEY));
        assertUnauthorized(createToken(ISSUER, null,
                Instant.now().plusSeconds(120), Instant.now().minusSeconds(1), SIGNING_KEY));

        assertThat(downstreamRequestCount).hasValue(0);
    }

    @Test
    void routesOnlyConfiguredCustomerOperationsAndForwardsOriginalBearerToken() throws Exception {
        String validToken = createToken(ISSUER, "customer-service", Instant.now().plusSeconds(120),
                Instant.now().minusSeconds(1), SIGNING_KEY);

        client.get()
                .uri("/api/v1/customers")
                .headers(headers -> headers.setBearerAuth(validToken))
                .header("X-User-Id", "untrusted-user")
                .header("X-Roles", "OPERATOR")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.service").isEqualTo("customer-stub");
        assertForwardedRequest("GET", "/api/v1/customers", validToken);

        client.post()
                .uri("/api/v1/customers")
                .headers(headers -> headers.setBearerAuth(validToken))
                .bodyValue("{\"name\":\"Test\"}")
                .exchange()
                .expectStatus().isOk();
        assertForwardedRequest("POST", "/api/v1/customers", validToken);

        client.get()
                .uri("/api/v1/customers/customer-123")
                .headers(headers -> headers.setBearerAuth(validToken))
                .exchange()
                .expectStatus().isOk();
        assertForwardedRequest("GET", "/api/v1/customers/customer-123", validToken);
    }

    @Test
    void doesNotExposeActuatorOrUnconfiguredServiceRoutesThroughTheGateway() throws Exception {
        String validToken = createToken(ISSUER, "customer-service", Instant.now().plusSeconds(120),
                Instant.now().minusSeconds(1), SIGNING_KEY);

        client.get()
                .uri("/actuator/health")
                .headers(headers -> headers.setBearerAuth(validToken))
                .exchange()
                .expectStatus().isNotFound();
        client.get()
                .uri("/api/v1/accounts")
                .headers(headers -> headers.setBearerAuth(validToken))
                .exchange()
                .expectStatus().isNotFound();

        assertThat(downstreamRequestCount).hasValue(0);
    }

    private void assertUnauthorized(String token) {
        client.get()
                .uri("/api/v1/customers")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private static String createToken(
            String issuer,
            String audience,
            Instant expiration,
            Instant notBefore,
            RSAKey key) throws JOSEException {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("gateway-test-user")
                .issueTime(Date.from(Instant.now()))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiration));
        if (audience != null) {
            claims.audience(audience);
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private void assertForwardedRequest(String method, String path, String token) {
        assertThat(downstreamMethod).hasValue(method);
        assertThat(downstreamPath).hasValue(path);
        assertThat(downstreamAuthorization).hasValue("Bearer " + token);
        assertThat(downstreamRequestCount).hasValueGreaterThan(0);
    }

    private static RSAKey generateKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048)
                    .keyID(keyId)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
        } catch (JOSEException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static HttpServer startDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", GatewaySecurityAndRoutingTests::handleDownstreamRequest);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void handleDownstreamRequest(HttpExchange exchange) throws IOException {
        if ("/jwks".equals(exchange.getRequestURI().getPath())) {
            send(exchange, 200, new JWKSet(SIGNING_KEY.toPublicJWK()).toString());
            return;
        }

        // Le faux aval confirme le routage sans dépendre d'un endpoint métier encore à implémenter.
        downstreamRequestCount.incrementAndGet();
        downstreamAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        downstreamPath.set(exchange.getRequestURI().getPath());
        downstreamMethod.set(exchange.getRequestMethod());
        exchange.getRequestBody().readAllBytes();
        send(exchange, 200, "{\"service\":\"customer-stub\"}");
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
