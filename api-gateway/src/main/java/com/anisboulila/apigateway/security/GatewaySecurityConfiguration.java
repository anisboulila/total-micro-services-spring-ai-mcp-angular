package com.anisboulila.apigateway.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain gatewaySecurityWebFilterChain(ServerHttpSecurity http) {
        // Le Gateway est sans session : chaque requête protégée doit présenter son propre bearer token.
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    ReactiveJwtDecoder gatewayJwtDecoder(GatewayJwtProperties properties) {
        // Nimbus vérifie la signature via le JWKS; le validateur standard contrôle aussi issuer,
        // expiration et dates de validité. L'audience restreint le jeton à notre ressource Customer.
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(properties.jwkSetUri())
                .build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(properties.audience())
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuerUri()),
                audienceValidator));
        return decoder;
    }
}
