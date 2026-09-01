package com.cryptostrategy.platform.api.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class SafeErrorDetails {
    private static final Set<String> STRING_FIELDS = Set.of(
            "resourceType",
            "resourceId",
            "currentState");

    private SafeErrorDetails() {
    }

    static Map<String, Object> copyOf(Map<String, Object> source) {
        Objects.requireNonNull(source, "details");
        if (source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, copyValue(key, value)));
        return Map.copyOf(copy);
    }

    private static Object copyValue(String key, Object value) {
        Objects.requireNonNull(key, "detail key");
        Objects.requireNonNull(value, "detail value");
        if (STRING_FIELDS.contains(key)) {
            return safeString(key, value);
        }
        return switch (key) {
            case "fieldErrors" -> copyFieldErrors(value);
            case "allowedStates" -> copyAllowedStates(value);
            case "retryable" -> requireType(key, value, Boolean.class);
            case "retryAfterSeconds" -> copyRetryAfter(value);
            default -> throw new IllegalArgumentException("Unsupported public error detail: " + key);
        };
    }

    private static List<Map<String, String>> copyFieldErrors(Object value) {
        if (!(value instanceof List<?> fieldErrors)) {
            throw invalidType("fieldErrors", "a list");
        }
        List<Map<String, String>> copy = new ArrayList<>(fieldErrors.size());
        for (Object fieldError : fieldErrors) {
            if (!(fieldError instanceof Map<?, ?> map)
                    || !map.keySet().equals(Set.of("field", "reason"))) {
                throw new IllegalArgumentException(
                        "Each public field error must contain only field and reason");
            }
            copy.add(Map.of(
                    "field", safeString("field", map.get("field")),
                    "reason", safeString("reason", map.get("reason"))));
        }
        return List.copyOf(copy);
    }

    private static List<String> copyAllowedStates(Object value) {
        if (!(value instanceof List<?> states)) {
            throw invalidType("allowedStates", "a list");
        }
        return states.stream()
                .map(state -> safeString("allowedStates", state))
                .toList();
    }

    private static int copyRetryAfter(Object value) {
        if (!(value instanceof Number number)) {
            throw invalidType("retryAfterSeconds", "a non-negative integer");
        }
        long seconds = number.longValue();
        if (seconds < 0 || seconds > Integer.MAX_VALUE || number.doubleValue() != seconds) {
            throw invalidType("retryAfterSeconds", "a non-negative integer");
        }
        return (int) seconds;
    }

    private static String safeString(String key, Object value) {
        String string = requireType(key, value, String.class);
        if (string.isBlank() || string.length() > 256 || string.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid public error detail: " + key);
        }
        return string;
    }

    private static <T> T requireType(String key, Object value, Class<T> type) {
        if (!type.isInstance(value)) {
            throw invalidType(key, type.getSimpleName());
        }
        return type.cast(value);
    }

    private static IllegalArgumentException invalidType(String key, String expected) {
        return new IllegalArgumentException("Public error detail " + key + " must be " + expected);
    }
}
