package com.anisboulila.apigateway.routing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
@EnableConfigurationProperties(CustomerRouteProperties.class)
public class CustomerRoutesConfiguration {

    @Bean
    RouteLocator customerRoutes(RouteLocatorBuilder routes, CustomerRouteProperties properties) {
        // Les routes explicites évitent d'exposer par découverte des services ou chemins non prévus.
        String customerServiceUri = properties.customerServiceUri().toString();
        return routes.routes()
                .route("customer-list", route -> route
                        .path("/api/v1/customers")
                        .and()
                        .method(HttpMethod.GET)
                        .uri(customerServiceUri))
                .route("customer-create", route -> route
                        .path("/api/v1/customers")
                        .and()
                        .method(HttpMethod.POST)
                        .uri(customerServiceUri))
                .route("customer-read-by-id", route -> route
                        .path("/api/v1/customers/{id}")
                        .and()
                        .method(HttpMethod.GET)
                        .uri(customerServiceUri))
                .build();
    }
}
