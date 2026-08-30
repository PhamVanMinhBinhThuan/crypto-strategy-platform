package com.cryptostrategy.platform.marketdata.internal.checksum;

import com.cryptostrategy.platform.domain.api.market.Candle;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CanonicalCandleEncoder {
    private CanonicalCandleEncoder() { }
    public static byte[] encodeHeader(String version) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        field(output, version);
        newline(output);
        return output.toByteArray();
    }
    public static byte[] encodeCandle(Candle candle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        field(output, candle.key().provider().value()); field(output, candle.key().tradingPair().canonicalSymbol());
        field(output, candle.key().timeframe().code()); field(output, candle.key().openTime().toString());
        field(output, candle.closeTime().toString()); field(output, decimal(candle.open())); field(output, decimal(candle.high()));
        field(output, decimal(candle.low())); field(output, decimal(candle.close())); field(output, decimal(candle.volume())); newline(output);
        return output.toByteArray();
    }
    public static byte[] encode(List<Candle> candles, String version) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(encodeHeader(version));
        for (Candle candle : candles) {
            output.writeBytes(encodeCandle(candle));
        }
        return output.toByteArray();
    }
    private static String decimal(java.math.BigDecimal value) { return value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString(); }
    private static void field(ByteArrayOutputStream output, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeBytes(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII)); output.write('#'); output.writeBytes(bytes);
    }
    private static void newline(ByteArrayOutputStream output) { output.write('\n'); }
}
