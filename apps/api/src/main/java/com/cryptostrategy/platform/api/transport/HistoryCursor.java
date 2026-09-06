package com.cryptostrategy.platform.api.transport;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

/** Versioned cursor shared by recent-resource endpoints. */
public record HistoryCursor(String kind, Instant timestamp, String id) {
    private static final String VERSION = "history-v1";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_-]{6,128}");

    public String encode() {
        String raw = String.join("\n", VERSION, kind, timestamp.toString(), id);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static HistoryCursor decode(String value, String expectedKind) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\n", -1);
            if (parts.length != 4 || !VERSION.equals(parts[0])
                    || !expectedKind.equals(parts[1]) || !IDENTIFIER.matcher(parts[3]).matches()) {
                throw new IllegalArgumentException("Invalid history cursor");
            }
            return new HistoryCursor(parts[1], Instant.parse(parts[2]), parts[3]);
        } catch (RuntimeException failure) {
            throw new InvalidCursorException("cursor is malformed");
        }
    }
}
