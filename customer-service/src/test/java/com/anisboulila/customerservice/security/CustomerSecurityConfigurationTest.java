package com.anisboulila.customerservice.security;

import com.anisboulila.customerservice.error.CorrelationIdFilter;
import com.anisboulila.customerservice.error.RestProblemExceptionHandler;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityProbeController.class)
@Import({
        CustomerSecurityConfiguration.class,
        SecurityProblemWriter.class,
        CorrelationIdFilter.class,
        RestProblemExceptionHandler.class
})
class CustomerSecurityConfigurationTest {

    // Le vérificateur reçoit la clé publique de ce serveur JWKS; les JWT sont signés à la volée
    // avec la clé privée de test, afin d'exercer le décodage réel plutôt que le helper jwt() de Spring.
    // Ces clés synthétiques ne prouvent pas que le realm Keycloak local est déjà configuré pareil.
    private static final String ISSUER = "https://identity.example.test/realms/customer-dev";
    private static final String AUDIENCE = "customer-service";
    private static final KeyPair SIGNING_KEY = generateRsaKeyPair();
    private static HttpServer jwksServer;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureJwtProperties(DynamicPropertyRegistry registry) {
        startJwksServer();
        registry.add("customer.security.jwt.issuer-uri", () -> ISSUER);
        registry.add("customer.security.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks");
        registry.add("customer.security.jwt.audience", () -> AUDIENCE);
    }

    @AfterAll
    static void stopJwksServer() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @Test
    void rejectsRequestsWithoutBearerTokenUsingProblemDetails() throws Exception {
        mockMvc.perform(get("/test/security/principal"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME,
                        org.hamcrest.Matchers.matchesRegex(
                                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        assertUnauthorized(signedToken(
                SIGNING_KEY, ISSUER, List.of(AUDIENCE), Instant.now().minusSeconds(120), List.of("CUSTOMER")));
    }

    @Test
    void rejectsTokenWithInvalidSignature() throws Exception {
        assertUnauthorized(signedToken(
                generateRsaKeyPair(), ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300), List.of("CUSTOMER")));
    }

    @Test
    void rejectsTokenFromUnexpectedIssuer() throws Exception {
        assertUnauthorized(signedToken(
                SIGNING_KEY, "https://untrusted.example.test/realms/customer-dev",
                List.of(AUDIENCE), Instant.now().plusSeconds(300), List.of("CUSTOMER")));
    }

    @Test
    void rejectsTokenWithWrongAudience() throws Exception {
        assertUnauthorized(signedToken(
                SIGNING_KEY, ISSUER, List.of("another-api"), Instant.now().plusSeconds(300), List.of("CUSTOMER")));
    }

    @Test
    void rejectsTokenWithoutAudience() throws Exception {
        assertUnauthorized(signedToken(
                SIGNING_KEY, ISSUER, List.of(), Instant.now().plusSeconds(300), List.of("CUSTOMER")));
    }

    @Test
    void acceptsCryptographicallyVerifiedTokenAndExtractsSubjectAndRealmRoles() throws Exception {
        String token = signedToken(
                SIGNING_KEY, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300),
                List.of("CUSTOMER", "OPERATOR"));

        mockMvc.perform(get("/test/security/principal").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("verified-user-123"))
                .andExpect(jsonPath("$.authorities[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.authorities[1]").value("OPERATOR"));
    }

    @Test
    void ignoresUntrustedIdentityAndRoleHeaders() throws Exception {
        String token = signedToken(
                SIGNING_KEY, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300), List.of("CUSTOMER"));

        // Les en-têtes contrôlés par l'appelant ne peuvent ni remplacer sub ni ajouter OPERATOR.
        mockMvc.perform(get("/test/security/principal")
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "attacker-controlled-user")
                        .header("X-Roles", "OPERATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("verified-user-123"))
                .andExpect(jsonPath("$.authorities[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.authorities.length()").value(1));
    }

    private void assertUnauthorized(String token) throws Exception {
        String body = mockMvc.perform(get("/test/security/principal")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFalse(body.contains(token));
    }

    private static String signedToken(
            KeyPair signingKey,
            String issuer,
            List<String> audiences,
            Instant expiresAt,
            List<String> roles) throws JOSEException {
        Instant issuedAt = Instant.now().minusSeconds(5);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("verified-user-123")
                .audience(audiences)
                .issueTime(Date.from(issuedAt))
                .notBeforeTime(Date.from(issuedAt.minusSeconds(1)))
                .expirationTime(Date.from(expiresAt))
                .claim("realm_access", Map.of("roles", roles))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID("test-signing-key")
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) signingKey.getPrivate()));
        return jwt.serialize();
    }

    private static void startJwksServer() {
        if (jwksServer != null) {
            return;
        }
        try {
            RSAKey publicJwk = new RSAKey.Builder((RSAPublicKey) SIGNING_KEY.getPublic())
                    .keyID("test-signing-key")
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
            byte[] jwks = new JWKSet(publicJwk).toString().getBytes(StandardCharsets.UTF_8);
            jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            jwksServer.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                exchange.sendResponseHeaders(200, jwks.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(jwks);
                }
            });
            jwksServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start the synthetic JWKS endpoint", exception);
        }
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA is unavailable for JWT security tests", exception);
        }
    }
}
