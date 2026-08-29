package com.cryptostrategy.platform.domain.api.market;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketDomainModelTest {
    private static final String ULID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Test void typedIdsAcceptCanonicalUlidsAndRejectUuidOrLowercase() {
        assertEquals(ULID, new CandleId(ULID).value());
        assertThrows(IllegalArgumentException.class, () -> new AssetId(ULID.toLowerCase()));
        assertThrows(IllegalArgumentException.class, () -> new TradingPairId("550e8400-e29b-41d4-a716-446655440000"));
        assertEquals(26, DatasetVersionId.generate().value().length());
    }

    @Test void pairExposesCanonicalBaseAndQuoteSymbol() {
        Asset base = asset("01ARZ3NDEKTSV4RRFFQ69G5FAV", "BTC");
        Asset quote = asset("01ARZ3NDEKTSV4RRFFQ69G5FAW", "USDT");
        TradingPair pair = new TradingPair(new TradingPairId("01ARZ3NDEKTSV4RRFFQ69G5FAX"), base, quote, true);
        assertSame(base, pair.baseAsset());
        assertSame(quote, pair.quoteAsset());
        assertEquals("BTC/USDT", pair.canonicalSymbol());
        assertThrows(IllegalArgumentException.class, () -> new TradingPair(pair.tradingPairId(), base, base, true));
    }

    @Test void timeframeAndCandleEnforceUtcIntervalAndExactNumericRules() {
        Instant open = Instant.parse("2026-01-01T00:00:00Z");
        TradingPair pair = pair();
        Candle candle = new Candle(new CandleKey(MarketProvider.BINANCE, pair, Timeframe.ONE_MINUTE, open),
                open.plusSeconds(60), new BigDecimal("1.000"), new BigDecimal("2.0"),
                new BigDecimal("0.5"), new BigDecimal("1.50"), new BigDecimal("12.000"), true);
        assertEquals(new BigDecimal("1"), candle.open());
        assertEquals(open.plusSeconds(60), candle.closeTime());
        assertThrows(IllegalArgumentException.class, () -> new Candle(candle.key(), open.plusSeconds(59),
                candle.open(), candle.high(), candle.low(), candle.close(), candle.volume(), true));
        assertThrows(IllegalArgumentException.class, () -> new Candle(candle.key(), candle.closeTime(),
                new BigDecimal("1234567890123456789"), candle.high(), candle.low(), candle.close(), candle.volume(), true));
        assertFalse(Timeframe.ONE_MINUTE.isAligned(open.plusMillis(1)));
    }

    @Test void canonicalNamesRejectNonCanonicalInput() {
        assertThrows(IllegalArgumentException.class, () -> new AssetSymbol("btc"));
        assertThrows(IllegalArgumentException.class, () -> new MarketProvider("binance"));
        assertEquals(Timeframe.FOUR_HOURS, Timeframe.fromCode("4h"));
    }

    private static Asset asset(String id, String symbol) {
        return new Asset(new AssetId(id), new AssetSymbol(symbol), Optional.empty(), true);
    }
    private static TradingPair pair() {
        return new TradingPair(new TradingPairId("01ARZ3NDEKTSV4RRFFQ69G5FAX"),
                asset("01ARZ3NDEKTSV4RRFFQ69G5FAV", "BTC"),
                asset("01ARZ3NDEKTSV4RRFFQ69G5FAW", "USDT"), true);
    }
}
