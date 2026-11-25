package com.anisboulila.customerservice.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.util.Set;

public final class CreateCustomerRequestDeserializer extends JsonDeserializer<CreateCustomerRequest> {

    private static final Set<String> RESERVED_FIELDS = Set.of("sub", "email", "email_verified", "customerId");

    @Override
    public CreateCustomerRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.isExpectedStartObjectToken()) {
            throw invalidRequest(parser);
        }

        String name = null;
        boolean nameSeen = false;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() != JsonToken.FIELD_NAME) {
                throw invalidRequest(parser);
            }
            String field = parser.currentName();
            if (RESERVED_FIELDS.contains(field)) {
                throw new InvalidCustomerFieldException(parser, field, "RESERVED");
            }
            if (!"name".equals(field) || nameSeen) {
                throw invalidRequest(parser);
            }
            nameSeen = true;
            JsonToken value = parser.nextToken();
            if (value == JsonToken.VALUE_STRING) {
                name = parser.getText();
            } else if (value != JsonToken.VALUE_NULL) {
                throw new InvalidCustomerFieldException(parser, "name", "INVALID");
            }
        }
        return new CreateCustomerRequest(name);
    }

    private static JsonMappingException invalidRequest(JsonParser parser) {
        return JsonMappingException.from(parser, "Invalid customer request body.");
    }
}
