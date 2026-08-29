package com.cryptostrategy.platform.domain.api.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Candle(CandleKey key, Instant closeTime, BigDecimal open, BigDecimal high,
                     BigDecimal low, BigDecimal close, BigDecimal volume, boolean closed) {
    public Candle {
        Objects.requireNonNull(key, "key"); Objects.requireNonNull(closeTime, "closeTime");
        open = exact(open, "open"); high = exact(high, "high"); low = exact(low, "low");
        close = exact(close, "close"); volume = exact(volume, "volume");
        if (!closeTime.equals(key.timeframe().next(key.openTime()))) throw new IllegalArgumentException("Close time must be the exclusive interval boundary");
        if (high.compareTo(open) < 0 || high.compareTo(low) < 0 || high.compareTo(close) < 0) throw new IllegalArgumentException("Invalid high");
        if (low.compareTo(open) > 0 || low.compareTo(high) > 0 || low.compareTo(close) > 0) throw new IllegalArgumentException("Invalid low");
    }
    private static BigDecimal exact(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.signum() < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        BigDecimal normalized = value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
        int scale = Math.max(normalized.scale(), 0);
        int integerDigits = Math.max(normalized.precision() - normalized.scale(), 0);
        if (scale > 12 || integerDigits > 18 || integerDigits + scale > 30) throw new IllegalArgumentException(name + " exceeds numeric(30,12)");
        return normalized;
    }
    public boolean canonicalContentEquals(Candle other) {
        return key.equals(other.key) && closeTime.equals(other.closeTime) && closed == other.closed
                && open.compareTo(other.open) == 0 && high.compareTo(other.high) == 0
                && low.compareTo(other.low) == 0 && close.compareTo(other.close) == 0
                && volume.compareTo(other.volume) == 0;
    }
}
