package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.error.ApiProblemException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CustomerIdentityValidator {

    public VerifiedIdentity validate(Jwt verifiedJwt) {
        String sub = validatedSubject(verifiedJwt);
        Object emailClaim = verifiedJwt.getClaims().get("email");
        if (!(emailClaim instanceof String email) || email.isBlank()
                || !Boolean.TRUE.equals(verifiedJwt.getClaims().get("email_verified"))) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_VERIFIED",
                    "Email not verified", "A verified email is required.");
        }
        return new VerifiedIdentity(sub, email);
    }

    public String validatedSubject(Jwt verifiedJwt) {
        Objects.requireNonNull(verifiedJwt, "verifiedJwt");
        Object subject = verifiedJwt.getClaims().get("sub");
        if (!(subject instanceof String sub) || sub.isBlank()) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Invalid request", "The authenticated identity is invalid.");
        }

        return sub;
    }

    public record VerifiedIdentity(String sub, String email) {
    }
}
