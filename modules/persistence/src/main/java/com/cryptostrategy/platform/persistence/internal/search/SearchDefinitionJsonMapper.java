package com.cryptostrategy.platform.persistence.internal.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Version-aware persistence codec for Search configuration and candidate definitions. */
public final class SearchDefinitionJsonMapper {
    private final ObjectMapper json;

    public SearchDefinitionJsonMapper() {
        this(new ObjectMapper());
    }

    SearchDefinitionJsonMapper(ObjectMapper json) {
        this.json = Objects.requireNonNull(json, "json");
    }

    public String writeSearchConfig(Map<String, Object> value) {
        Map<String, Object> normalized = normalized(value);
        validateSearchConfig(normalized);
        return write(normalized);
    }

    public Map<String, Object> readSearchConfig(String value) {
        Map<String, Object> decoded = read(value);
        validateSearchConfig(decoded);
        return decoded;
    }

    public String writeCandidateDefinition(Map<String, Object> value) {
        Map<String, Object> normalized = normalized(value);
        validateCandidate(normalized);
        return write(normalized);
    }

    public Map<String, Object> readCandidateDefinition(String value) {
        Map<String, Object> decoded = read(value);
        validateCandidate(decoded);
        return decoded;
    }

    private static void validateSearchConfig(Map<String, Object> value) {
        Object contract = value.get("contractVersion");
        if (contract == null || "search-config-v1".equals(contract)) return;
        if (!"search-config-v2".equals(contract)) {
            throw new IllegalStateException("Unsupported Search configuration contract");
        }
        Object rawSpace = value.get("searchSpace");
        if (!(rawSpace instanceof Map<?, ?> space)
                || !Integer.valueOf(2).equals(integer(space.get("schemaVersion")))) {
            throw new IllegalStateException("Search configuration v2 requires searchSpace schemaVersion 2");
        }
    }

    private static void validateCandidate(Map<String, Object> value) {
        Object rawVersion = value.get("schemaVersion");
        if (rawVersion == null) return;
        if (!Integer.valueOf(2).equals(integer(rawVersion))
                || !(value.get("components") instanceof java.util.List<?> components)
                || components.isEmpty()
                || !(value.get("combinationPolicy") instanceof Map<?, ?>)) {
            throw new IllegalStateException("Invalid composite Candidate definition schema");
        }
    }

    private static Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Map<String, Object> normalized(Map<String, Object> value) {
        return Map.copyOf(new TreeMap<>(Objects.requireNonNull(value, "value")));
    }

    private String write(Map<String, Object> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Failed to serialize Search JSON", failure);
        }
    }

    private Map<String, Object> read(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try {
            return normalized(json.readValue(value, new TypeReference<>() {}));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Failed to parse Search JSON", failure);
        }
    }
}
