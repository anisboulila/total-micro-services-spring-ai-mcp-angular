package com.anisboulila.customerservice.error;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration(proxyBeanMethods = false)
class ProblemProbeSecurityConfiguration {

    // Cette chaîne n'existe que dans le slice MockMvc et n'autorise que les probes du gestionnaire d'erreurs.
    @Bean
    SecurityFilterChain problemProbeSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/test/problems/**").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
