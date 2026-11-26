package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.error.ApiProblemException;
import com.anisboulila.customerservice.service.CustomerIdempotencyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerIdentityValidator identityValidator;
    private final CustomerIdempotencyService customerService;
    private final boolean creationEnabled;

    public CustomerController(CustomerIdentityValidator identityValidator,
                              CustomerIdempotencyService customerService,
                              @Value("${customer.profile-creation.enabled:false}") boolean creationEnabled) {
        this.identityValidator = identityValidator;
        this.customerService = customerService;
        this.creationEnabled = creationEnabled;
    }

    @PostMapping
    public ResponseEntity<String> create(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody CreateCustomerRequest request,
                                         @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        // Garde temporaire jusqu'à la validation complète du parcours de création.
        if (!creationEnabled) {
            throw new ApiProblemException(HttpStatus.SERVICE_UNAVAILABLE, "PROFILE_CREATION_DISABLED",
                    "Profile creation unavailable", "Profile creation is not enabled.");
        }

        if (key == null || key.isBlank() || key.codePointCount(0, key.length()) > 255) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Invalid idempotency key", "A non-blank idempotency key of at most 255 characters is required.");
        }
        // Le sub et l'e-mail viennent du jeton validé, jamais du corps ou d'un en-tête libre.
        CustomerIdentityValidator.VerifiedIdentity identity = identityValidator.validate(jwt);
        CustomerIdempotencyService.CreationResult result =
                customerService.create(identity.sub(), request.name(), identity.email(), key);
        return ResponseEntity.status(result.status()).location(URI.create(result.location()))
                .contentType(MediaType.APPLICATION_JSON).body(result.body());
    }
}
