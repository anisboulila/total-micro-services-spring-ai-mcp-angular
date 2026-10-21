package com.anisboulila.customerservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(CustomerJwtProperties.class)
public class CustomerSecurityConfiguration {

    @Bean
    SecurityFilterChain customerSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            SecurityProblemWriter problemWriter) throws Exception {
        // Chaque requête applicative exige un JWT vérifié; seule la sonde de santé interne est publique.
        AuthenticationEntryPoint problemEntryPoint = (request, response, failure) -> problemWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Unauthorized",
                "Authentication is required or the bearer token is invalid.");
        AccessDeniedHandler problemAccessDeniedHandler = (request, response, failure) -> problemWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Forbidden",
                "The authenticated user is not allowed to access this resource.");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemEntryPoint)
                        .accessDeniedHandler(problemAccessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(problemEntryPoint)
                        .accessDeniedHandler(problemAccessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    JwtDecoder customerJwtDecoder(CustomerJwtProperties properties) {
        // Nimbus vérifie la signature avec les clés publiées par l'issuer; les validateurs contrôlent
        // ensuite iss, exp et nbf. Le validateur complémentaire exige l'audience de ce service.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> hasRequiredAudience(jwt, properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuerUri()),
                audienceValidator));
        return decoder;
    }

    private boolean hasRequiredAudience(Jwt jwt, String expectedAudience) {
        List<String> audiences = jwt.getAudience();
        return audiences != null && !audiences.isEmpty() && audiences.contains(expectedAudience);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Après validation, JwtAuthenticationToken expose sub via Authentication#getName.
        // Le convertisseur ne lit aucun en-tête libre pour établir les autorités.
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::realmAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> realmAuthorities(Jwt jwt) {
        // Seuls les rôles signés dans realm_access.roles deviennent des autorités, sans préfixe ajouté.
        Object realmAccessClaim = jwt.getClaim("realm_access");
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return List.of();
        }

        Object rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(role -> !role.isBlank())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
