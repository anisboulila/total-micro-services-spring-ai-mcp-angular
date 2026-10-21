package com.anisboulila.customerservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SecurityProbeController {

    @GetMapping("/test/security/principal")
    public Map<String, Object> principal(Authentication authentication) {
        // Avec JwtAuthenticationToken, getName() provient de sub; le contrôleur ne consulte aucun header d'identité.
        return Map.of(
                "sub", authentication.getName(),
                "authorities", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .sorted()
                        .toList());
    }
}
