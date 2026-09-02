package com.cryptostrategy.platform.api.market;

import com.cryptostrategy.platform.api.transport.InvalidCursorException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

final class MarketCursor {
    private static final String VERSION = "market-cursor-v1";

    private MarketCursor() {}

    static String encode(Instant nextOpenTime, MarketRequestMapper.MarketRange range) {
        String value = VERSION + "|" + nextOpenTime.toEpochMilli() + "|" + fingerprint(range);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static Instant decode(String cursor, MarketRequestMapper.MarketRange range) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 3
                    || !VERSION.equals(parts[0])
                    || !fingerprint(range).equals(parts[2])) {
                throw invalid(null);
            }
            Instant nextOpenTime = Instant.ofEpochMilli(Long.parseLong(parts[1]));
            if (nextOpenTime.isBefore(range.startTime())
                    || !nextOpenTime.isBefore(range.endTime())
                    || !range.timeframe().isAligned(nextOpenTime)) {
                throw invalid(null);
            }
            return nextOpenTime;
        } catch (InvalidCursorException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid(exception);
        }
    }

    private static String fingerprint(MarketRequestMapper.MarketRange range) {
        String canonical = range.provider().value()
                + "|" + range.tradingPair().canonicalSymbol()
                + "|" + range.timeframe().code()
                + "|" + range.startTime()
                + "|" + range.endTime();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static InvalidCursorException invalid(Throwable cause) {
        return cause == null
                ? new InvalidCursorException("cursor is invalid")
                : new InvalidCursorException("cursor is invalid", cause);
    }
}
