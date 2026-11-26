package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.domain.Customer;
import com.anisboulila.customerservice.error.ApiProblemException;
import com.anisboulila.customerservice.service.CustomerApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerIdentityValidator identityValidator;
    private final CustomerApplicationService customerService;
    private final boolean creationEnabled;

    public CustomerController(CustomerIdentityValidator identityValidator,
                              CustomerApplicationService customerService,
                              @Value("${customer.profile-creation.enabled:false}") boolean creationEnabled) {
        this.identityValidator = identityValidator;
        this.customerService = customerService;
        this.creationEnabled = creationEnabled;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody CreateCustomerRequest request) {
        // Garde temporaire : les doublons et l'idempotence ne sont pas encore implémentés.
        if (!creationEnabled) {
            throw new ApiProblemException(HttpStatus.SERVICE_UNAVAILABLE, "PROFILE_CREATION_DISABLED",
                    "Profile creation unavailable", "Profile creation is not enabled.");
        }

        // Le sub et l'e-mail viennent du jeton validé, jamais du corps ou d'un en-tête libre.
        CustomerIdentityValidator.VerifiedIdentity identity = identityValidator.validate(jwt);
        Customer customer = customerService.createFromVerifiedIdentity(identity.sub(), request.name(), identity.email());
        URI location = URI.create("/api/v1/customers/" + customer.getId());
        return ResponseEntity.created(location).body(CustomerResponse.from(customer));
    }
}
