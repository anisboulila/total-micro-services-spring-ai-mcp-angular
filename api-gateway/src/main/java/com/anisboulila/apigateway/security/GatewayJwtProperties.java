package com.anisboulila.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "gateway.security.jwt")
public record GatewayJwtProperties(
        String issuerUri,
        String jwkSetUri,
        String audience) {

    public GatewayJwtProperties {
        Assert.hasText(issuerUri, "Gateway JWT issuer URI must not be blank");
        Assert.hasText(jwkSetUri, "Gateway JWT JWK Set URI must not be blank");
        Assert.hasText(audience, "Gateway JWT audience must not be blank");
    }
}
