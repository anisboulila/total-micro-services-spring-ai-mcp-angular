package com.anisboulila.customerservice.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

public final class InvalidCustomerFieldException extends JsonMappingException {

    private final String field;
    private final String code;

    InvalidCustomerFieldException(JsonParser parser, String field, String code) {
        super(parser, "Invalid customer request field.");
        this.field = field;
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public String getCode() {
        return code;
    }
}
