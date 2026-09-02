package com.cryptostrategy.platform.api.realtime;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Redis Stream names used to fan transient F-007 notifications into local sessions. */
@ConfigurationProperties(prefix = "platform.realtime.streams")
public record RealtimeStreamProperties(
        boolean enabled,
        String progress,
        String lifecycle,
        String candidateEvaluated,
        Duration pollTimeout) {

    public RealtimeStreamProperties {
        progress = textOrDefault(progress, "progress.events.v1");
        lifecycle = textOrDefault(lifecycle, "lifecycle.events.v1");
        candidateEvaluated = textOrDefault(candidateEvaluated, "candidate.evaluated.v1");
        pollTimeout = pollTimeout == null ? Duration.ofSeconds(1) : pollTimeout;
        if (pollTimeout.isZero() || pollTimeout.isNegative()) {
            throw new IllegalArgumentException("Realtime stream poll timeout must be positive");
        }
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
