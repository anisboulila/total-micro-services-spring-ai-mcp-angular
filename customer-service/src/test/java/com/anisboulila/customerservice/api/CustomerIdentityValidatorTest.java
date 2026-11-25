package com.anisboulila.customerservice.api;

import com.anisboulila.customerservice.error.ApiProblemException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerIdentityValidatorTest {

    private final CustomerIdentityValidator validator = new CustomerIdentityValidator();

    @Test
    void acceptsSubjectAndVerifiedEmailFromJwt() {
        Jwt jwt = token().claim("sub", "verified-subject")
                .claim("email", "ada@example.test")
                .claim("email_verified", true)
                .build();

        assertEquals(new CustomerIdentityValidator.VerifiedIdentity("verified-subject", "ada@example.test"),
                validator.validate(jwt));
    }

    @Test
    void rejectsMissingOrUnverifiedEmailWithStableCode() {
        assertEmailRejected(token().claim("sub", "verified-subject").claim("email_verified", true).build());
        assertEmailRejected(token().claim("sub", "verified-subject").claim("email", " ").claim("email_verified", true).build());
        assertEmailRejected(token().claim("sub", "verified-subject").claim("email", "ada@example.test").build());
        assertEmailRejected(token().claim("sub", "verified-subject").claim("email", "ada@example.test")
                .claim("email_verified", false).build());
        assertEmailRejected(token().claim("sub", "verified-subject").claim("email", "ada@example.test")
                .claim("email_verified", "true").build());
        assertEmailRejected(token().claim("sub", "verified-subject").claim("email", 42)
                .claim("email_verified", true).build());
    }

    @Test
    void rejectsMissingOrBlankSubjectSeparately() {
        for (Jwt jwt : new Jwt[] {
                token().claim("email", "ada@example.test").claim("email_verified", true).build(),
                token().claim("sub", " ").claim("email", "ada@example.test").claim("email_verified", true).build()
        }) {
            ApiProblemException error = assertThrows(ApiProblemException.class, () -> validator.validate(jwt));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
            assertEquals("VALIDATION_FAILED", error.getCode());
        }
    }

    private void assertEmailRejected(Jwt jwt) {
        ApiProblemException error = assertThrows(ApiProblemException.class, () -> validator.validate(jwt));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("EMAIL_NOT_VERIFIED", error.getCode());
    }

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("test-only").header("alg", "none");
    }
}
