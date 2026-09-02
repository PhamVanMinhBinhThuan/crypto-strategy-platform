package com.cryptostrategy.platform.api.strategy;

import com.cryptostrategy.platform.api.transport.InvalidCursorException;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class StrategyCursor {
    private static final String VERSION = "v1";

    private StrategyCursor() {}

    static String encode(Stream stream, String internalCursor) {
        if (internalCursor == null || internalCursor.isBlank()) {
            throw new IllegalArgumentException("Internal cursor must not be blank");
        }
        String value = VERSION + ":" + stream.name() + ":" + internalCursor;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static String decode(Stream expectedStream, String publicCursor) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(publicCursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 3);
            if (parts.length != 3
                    || !VERSION.equals(parts[0])
                    || !expectedStream.name().equals(parts[1])
                    || parts[2].isBlank()) {
                throw invalid();
            }
            return validateInternal(expectedStream, parts[2]);
        } catch (IllegalArgumentException exception) {
            if (exception instanceof InvalidCursorException invalidCursor) {
                throw invalidCursor;
            }
            throw new InvalidCursorException("Strategy cursor is malformed", exception);
        }
    }

    private static InvalidCursorException invalid() {
        return new InvalidCursorException("Strategy cursor does not match this collection");
    }

    private static String validateInternal(Stream stream, String value) {
        if (stream == Stream.PRIVATE) {
            return Ulids.requireValid(value);
        }
        int offset = Integer.parseInt(value);
        if (offset < 0 || !Integer.toString(offset).equals(value)) {
            throw invalid();
        }
        return value;
    }

    enum Stream {
        SYSTEM,
        PRIVATE
    }
}
