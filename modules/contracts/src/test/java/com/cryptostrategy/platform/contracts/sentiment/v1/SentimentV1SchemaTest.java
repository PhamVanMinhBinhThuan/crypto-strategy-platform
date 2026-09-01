package com.cryptostrategy.platform.contracts.sentiment.v1;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import java.io.InputStream;
import java.util.*;
import org.junit.jupiter.api.Test;

class SentimentV1SchemaTest {
    private static final String ROOT = "contracts/sentiment-v1/";
    private final ObjectMapper json = new ObjectMapper();

    @Test void valid_shared_fixtures_obey_closed_inventory_and_exact_decimal_rules() throws Exception {
        assertClosed("analyze-request.schema.json", "fixtures/valid-request.json");
        assertClosed("analyze-success.schema.json", "fixtures/valid-success.json");
        assertClosed("error-response.schema.json", "fixtures/transient-error.json");
        assertClosed("health.schema.json", "fixtures/ready.json");

        JsonNode schema = read("analyze-success.schema.json");
        JsonNode success = read("fixtures/valid-success.json");
        assertExactDecimal(schema, success, "confidence", List.of("0.0", "0.820", "1.0000000000", "0.12345678901", "1.1"));
        assertExactDecimal(schema, success, "polarityScore", List.of("-0.0", "0.640", "-1.000", "-1.1", "0.12345678901"));
    }

    @Test void runtime_resource_inventory_is_complete_and_english_only() throws Exception {
        for (String name : List.of(
                "analyze-request.schema.json", "analyze-success.schema.json", "error-response.schema.json", "health.schema.json",
                "fixtures/valid-request.json", "fixtures/valid-success.json", "fixtures/transient-error.json", "fixtures/ready.json")) {
            assertNotNull(resource(name), name);
            assertTrue(read(name).isObject(), name);
        }
        assertEquals("en", read("analyze-request.schema.json").path("properties").path("language").path("const").asText());
        assertEquals("en", read("fixtures/valid-request.json").path("language").asText());
        assertEquals("en", read("fixtures/valid-success.json").path("language").asText());
    }

    private void assertClosed(String schemaName, String fixtureName) throws Exception {
        JsonNode schema = read(schemaName), fixture = read(fixtureName);
        assertFalse(schema.path("additionalProperties").asBoolean(true));
        Set<String> allowed = new HashSet<>();
        schema.path("properties").fieldNames().forEachRemaining(allowed::add);
        fixture.fieldNames().forEachRemaining(field -> assertTrue(allowed.contains(field), field));
        for (JsonNode required : schema.path("required")) assertTrue(fixture.has(required.asText()), required.asText());
        assertFalse(allowed.contains("unexpected"));
    }

    private void assertExactDecimal(JsonNode schema, JsonNode fixture, String field, List<String> rejected) {
        assertEquals("string", schema.path("properties").path(field).path("type").asText());
        assertTrue(fixture.path(field).isTextual());
        String pattern = schema.path("properties").path(field).path("pattern").asText();
        assertTrue(fixture.path(field).asText().matches(pattern));
        rejected.forEach(value -> assertFalse(value.matches(pattern), field + " accepted " + value));
    }

    private JsonNode read(String name) throws Exception {
        try (InputStream stream = resource(name)) {
            assertNotNull(stream, name);
            return json.readTree(stream);
        }
    }

    private InputStream resource(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(ROOT + name);
    }
}
