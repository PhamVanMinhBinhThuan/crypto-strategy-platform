package com.cryptostrategy.platform.api.error;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record ErrorEnvelope(
        String code,
        String message,
        Map<String, Object> details,
        String correlationId,
        Instant timestamp) {
    private static final Pattern PUBLIC_CODE = Pattern.compile("[A-Z][A-Z0-9_]*");

    public ErrorEnvelope {
        if (code == null || !PUBLIC_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Public error code must use UPPER_SNAKE_CASE");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Public error message is required");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Public error correlation ID is required");
        }
        details = SafeErrorDetails.copyOf(details);
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }
}
