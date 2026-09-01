package com.cryptostrategy.platform.api.transport;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Validates public pagination input while keeping continuation cursors opaque. */
@Component
public final class PageRequestMapper {
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;
    private static final int MAX_CURSOR_LENGTH = 1024;
    private static final Pattern OPAQUE_CURSOR = Pattern.compile("[A-Za-z0-9_-]+");

    public PageRequest map(Integer limit, String cursor) {
        return map(limit, cursor, DEFAULT_LIMIT, MAX_LIMIT);
    }

    public PageRequest map(
            Integer limit,
            String cursor,
            int defaultLimit,
            int maxLimit) {
        requireBounds(defaultLimit, maxLimit);
        int resolvedLimit = limit == null ? defaultLimit : limit;
        if (resolvedLimit < 1 || resolvedLimit > maxLimit) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + maxLimit);
        }
        Optional<String> resolvedCursor = cursor == null
                ? Optional.empty()
                : Optional.of(requireCursor(cursor, "cursor"));
        return new PageRequest(resolvedLimit, resolvedCursor);
    }

    static String requireCursor(String cursor, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");
        if (cursor == null
                || cursor.isBlank()
                || cursor.length() > MAX_CURSOR_LENGTH
                || !OPAQUE_CURSOR.matcher(cursor).matches()) {
            throw new IllegalArgumentException(fieldName + " is malformed");
        }
        return cursor;
    }

    private static void requireBounds(int defaultLimit, int maxLimit) {
        if (defaultLimit < 1
                || maxLimit < 1
                || defaultLimit > maxLimit
                || maxLimit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "pagination bounds must satisfy 1 <= default <= max <= 200");
        }
    }

    public record PageRequest(int limit, Optional<String> cursor) {
        public PageRequest {
            cursor = Objects.requireNonNull(cursor, "cursor");
        }
    }
}
