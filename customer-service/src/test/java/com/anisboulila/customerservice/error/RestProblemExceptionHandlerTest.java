package com.anisboulila.customerservice.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProblemProbeController.class)
@Import({
        RestProblemExceptionHandler.class,
        CorrelationIdFilter.class,
        ProblemProbeSecurityConfiguration.class
})
class RestProblemExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsRfc9457ProblemWithStableCodeAndValidatedCorrelationId() throws Exception {
        String correlationId = "a8a2e287-b820-4f74-9e6f-686b2d7e5b91";
        String expectedBody = """
                {
                  "type": "about:blank",
                  "title": "Invalid request",
                  "status": 400,
                  "detail": "One or more request fields are invalid.",
                  "instance": "/test/problems/validation",
                  "code": "VALIDATION_FAILED",
                  "correlationId": "%s",
                  "errors": [{"field": "name", "code": "NOT_BLANK"}]
                }
                """.formatted(correlationId);

        mockMvc.perform(get("/test/problems/validation")
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json(expectedBody, JsonCompareMode.STRICT))
                .andExpect(header().stringValues(CorrelationIdFilter.HEADER_NAME, correlationId))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_BLANK"));
    }

    @Test
    void replacesInvalidCorrelationIdAndHidesUnexpectedExceptionDetails() throws Exception {
        String invalidCorrelationId = "Bearer secret-token; SELECT * FROM customer";
        var result = mockMvc.perform(get("/test/problems/internal")
                        .header(CorrelationIdFilter.HEADER_NAME, invalidCorrelationId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String correlationId = result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
        assertTrue(UUID.fromString(correlationId).toString().equals(correlationId));
        assertNotEquals(invalidCorrelationId, correlationId);
        assertTrue(body.contains("\"correlationId\":\"" + correlationId + "\""));
        assertFalse(body.contains("SELECT"));
        assertFalse(body.contains("Bearer"));
        assertFalse(body.contains("secret-token"));
        assertFalse(body.contains("password=secret"));
        assertFalse(body.contains("IllegalStateException"));
        assertFalse(body.contains("at com.anisboulila"));
    }

}
