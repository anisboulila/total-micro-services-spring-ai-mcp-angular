package com.anisboulila.customerservice.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Centralise les exceptions MVC dans le contrat RFC 9457 sans exposer les détails techniques. */
@RestControllerAdvice
public class RestProblemExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestProblemExceptionHandler.class);
    private static final String VALIDATION_DETAIL = "One or more request fields are invalid.";

    @ExceptionHandler(ApiProblemException.class)
    public ResponseEntity<ProblemDetail> handleApiProblem(
            ApiProblemException exception,
            HttpServletRequest request) {
        String correlationId = CorrelationIdFilter.getOrCreateCorrelationId(request);
        LOGGER.warn("REST problem correlationId={} status={} code={}",
                correlationId, exception.getStatus().value(), exception.getCode());
        return createResponse(
                exception.getStatus(),
                exception.getCode(),
                exception.getTitle(),
                exception.getDetail(),
                exception.getErrors(),
                correlationId,
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ApiProblemException.FieldError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(RestProblemExceptionHandler::toFieldError)
                .distinct()
                .sorted(Comparator.comparing(ApiProblemException.FieldError::field)
                        .thenComparing(ApiProblemException.FieldError::code))
                .toList();
        String correlationId = CorrelationIdFilter.getOrCreateCorrelationId(request);
        LOGGER.warn("REST problem correlationId={} status={} code={}",
                correlationId, HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED");
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Invalid request",
                VALIDATION_DETAIL,
                errors,
                correlationId,
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        String correlationId = CorrelationIdFilter.getOrCreateCorrelationId(request);
        LOGGER.warn("REST problem correlationId={} status={} code={}",
                correlationId, HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED");
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Invalid request",
                "The request body is invalid.",
                List.of(),
                correlationId,
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        String correlationId = CorrelationIdFilter.getOrCreateCorrelationId(request);
        // Les logs gardent une trace diagnostique sans recopier le message brut, potentiellement sensible.
        LOGGER.error("Unhandled REST exception correlationId={} sanitizedTrace={}",
                correlationId, sanitizedTrace(exception));
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal Server Error",
                "An unexpected error occurred.",
                List.of(),
                correlationId,
                request);
    }

    private static ApiProblemException.FieldError toFieldError(FieldError error) {
        String constraintCode = error.getCode();
        String stableCode = constraintCode == null
                ? "INVALID"
                : constraintCode.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        return new ApiProblemException.FieldError(error.getField(), stableCode);
    }

    private static ResponseEntity<ProblemDetail> createResponse(
            HttpStatus status,
            String code,
            String title,
            String detail,
            List<ApiProblemException.FieldError> errors,
            String correlationId,
            HttpServletRequest request) {
        // ProblemDetail fournit les champs RFC 9457; code et correlationId complètent le contrat stable du projet.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", correlationId);
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String sanitizedTrace(Throwable failure) {
        StringBuilder trace = new StringBuilder();
        Throwable cause = failure;
        int causeDepth = 0;
        while (cause != null && causeDepth++ < 8) {
            if (trace.length() > 0) {
                trace.append("\nCaused by ");
            }
            trace.append(cause.getClass().getName());
            StackTraceElement[] frames = cause.getStackTrace();
            for (int index = 0; index < Math.min(frames.length, 32); index++) {
                trace.append("\n\tat ").append(frames[index]);
            }
            cause = cause.getCause();
        }
        return trace.toString();
    }
}
