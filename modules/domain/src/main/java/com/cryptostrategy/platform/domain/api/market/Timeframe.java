package com.cryptostrategy.platform.domain.api.market;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public enum Timeframe {
    ONE_MINUTE("1m", Duration.ofMinutes(1)),
    FIVE_MINUTES("5m", Duration.ofMinutes(5)),
    FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
    THIRTY_MINUTES("30m", Duration.ofMinutes(30)),
    ONE_HOUR("1h", Duration.ofHours(1)),
    TWO_HOURS("2h", Duration.ofHours(2)),
    FOUR_HOURS("4h", Duration.ofHours(4)),
    ONE_DAY("1d", Duration.ofDays(1));

    private final String code;
    private final Duration duration;
    Timeframe(String code, Duration duration) { this.code = code; this.duration = duration; }
    public String code() { return code; }
    public Duration duration() { return duration; }
    public boolean isAligned(Instant instant) { return Math.floorMod(instant.getEpochSecond(), duration.toSeconds()) == 0 && instant.getNano() == 0; }
    public Instant next(Instant instant) { if (!isAligned(instant)) throw new IllegalArgumentException("Instant is not aligned"); return instant.plus(duration); }
    public static Timeframe fromCode(String code) { return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported timeframe")); }
    @Override public String toString() { return code; }
}
