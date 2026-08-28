package com.cryptostrategy.platform.api.error;

import java.time.Instant;
import java.util.Map;

public record ErrorEnvelope(
        String code,
        String message,
        Map<String, Object> details,
        String correlationId,
        Instant timestamp) {
}
