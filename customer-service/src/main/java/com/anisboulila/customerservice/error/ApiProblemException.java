package com.anisboulila.customerservice.error;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ApiProblemException extends RuntimeException {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String detail;
    private final List<FieldError> errors;

    public ApiProblemException(
            HttpStatus status,
            String code,
            String title,
            String detail) {
        this(status, code, title, detail, List.of());
    }

    public ApiProblemException(
            HttpStatus status,
            String code,
            String title,
            String detail,
            List<FieldError> errors) {
        super(detail);
        this.status = Objects.requireNonNull(status, "status");
        this.code = requireCode(code);
        this.title = Objects.requireNonNull(title, "title");
        this.detail = Objects.requireNonNull(detail, "detail");
        this.errors = List.copyOf(errors);
    }

    private static String requireCode(String code) {
        Objects.requireNonNull(code, "code");
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("code must use UPPER_SNAKE_CASE");
        }
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public record FieldError(String field, String code) {
        public FieldError {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(code, "code");
            if (!CODE_PATTERN.matcher(code).matches()) {
                throw new IllegalArgumentException("field error code must use UPPER_SNAKE_CASE");
            }
        }
    }
}
