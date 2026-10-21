package com.anisboulila.customerservice.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "customer.security.jwt")
public record CustomerJwtProperties(
        String issuerUri,
        String jwkSetUri,
        String audience) {

    public CustomerJwtProperties {
        Assert.hasText(issuerUri, "JWT issuer URI must not be blank");
        Assert.hasText(jwkSetUri, "JWT JWK Set URI must not be blank");
        Assert.hasText(audience, "JWT audience must not be blank");
    }
}
