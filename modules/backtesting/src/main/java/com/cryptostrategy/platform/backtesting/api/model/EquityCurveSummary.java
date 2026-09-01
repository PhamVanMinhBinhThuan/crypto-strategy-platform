package com.cryptostrategy.platform.backtesting.api.model;
import java.util.Objects;
public record EquityCurveSummary(long pointCount, Money peakEquity, Money troughEquity, long peakSequence,
        long troughSequence, String curveDigest) {
    public EquityCurveSummary { Objects.requireNonNull(peakEquity);Objects.requireNonNull(troughEquity);Objects.requireNonNull(curveDigest);if(pointCount<1||peakSequence<0||troughSequence<peakSequence||troughSequence>=pointCount||!curveDigest.matches("^sha256:[0-9a-f]{64}$"))throw new IllegalArgumentException("Invalid equity summary"); }
}
