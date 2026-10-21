package com.anisboulila.customerservice.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProblemProbeController {

    // Contrôleur exclusivement de test : il permet d'exercer les deux chemins réels du handler sans endpoint métier.
    @GetMapping("/test/problems/validation")
    public void validationProblem() {
        // L'exception applicative réelle suit le même chemin MVC que les erreurs métier contrôlées.
        throw new ApiProblemException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Invalid request",
                "One or more request fields are invalid.",
                List.of(new ApiProblemException.FieldError("name", "NOT_BLANK")));
    }

    @GetMapping("/test/problems/internal")
    public void internalProblem() {
        throw new IllegalStateException(
                "SELECT * FROM customer; Authorization: Bearer secret-token; password=secret");
    }
}
