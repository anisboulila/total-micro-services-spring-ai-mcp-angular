package com.anisboulila.apigateway.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.net.URI;

@ConfigurationProperties(prefix = "gateway.routes")
public record CustomerRouteProperties(URI customerServiceUri) {

    public CustomerRouteProperties {
        Assert.notNull(customerServiceUri, "Customer service URI must not be null");
    }
}
