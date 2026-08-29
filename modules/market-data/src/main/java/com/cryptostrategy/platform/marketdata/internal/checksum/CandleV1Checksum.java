package com.cryptostrategy.platform.marketdata.internal.checksum;

import com.cryptostrategy.platform.domain.api.market.Candle;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class CandleV1Checksum {
    public static final String VERSION = "candle-v1";
    public String calculate(List<Candle> candles) {
        List<Candle> ordered = candles.stream().sorted(Comparator.comparing(candle -> candle.key().openTime())).toList();
        Accumulator accumulator = accumulator();
        ordered.forEach(accumulator::add);
        return accumulator.finish();
    }
    public Accumulator accumulator() { return new Accumulator(); }
    public static final class Accumulator {
        private final MessageDigest digest;
        private boolean finished;
        private Accumulator() {
            try { digest = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
            digest.update(CanonicalCandleEncoder.encodeHeader(VERSION));
        }
        public void add(Candle candle) {
            if (finished) throw new IllegalStateException("Checksum accumulator is already finished");
            digest.update(CanonicalCandleEncoder.encodeCandle(candle));
        }
        public String finish() {
            if (finished) throw new IllegalStateException("Checksum accumulator is already finished");
            finished = true;
            return "sha256:" + HexFormat.of().formatHex(digest.digest());
        }
    }
}
