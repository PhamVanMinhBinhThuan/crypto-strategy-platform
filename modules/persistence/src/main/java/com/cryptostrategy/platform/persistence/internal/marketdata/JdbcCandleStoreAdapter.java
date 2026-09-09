package com.cryptostrategy.platform.persistence.internal.marketdata;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleId;
import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import com.cryptostrategy.platform.marketdata.api.port.out.ClosedCandleStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcCandleStoreAdapter implements ClosedCandleStore {
    static final int BATCH_SIZE = 1_000;

    private final JdbcTemplate jdbc;
    private final JdbcMarketReferenceDataAdapter references;

    public JdbcCandleStoreAdapter(
            JdbcTemplate jdbc,
            JdbcMarketReferenceDataAdapter references) {
        this.jdbc = jdbc;
        this.references = references;
    }

    @Override
    public PersistedCandle saveClosed(Candle candle) {
        return saveClosedBatch(List.of(Objects.requireNonNull(candle, "candle"))).getFirst();
    }

    @Override
    public List<PersistedCandle> saveClosedBatch(List<Candle> candles) {
        List<Candle> ordered = validateAndOrder(candles);
        if (ordered.isEmpty()) return List.of();

        Map<BatchIdentity, List<Candle>> groups = new LinkedHashMap<>();
        for (Candle candle : ordered) {
            groups.computeIfAbsent(BatchIdentity.from(candle), ignored -> new ArrayList<>())
                    .add(candle);
        }

        Map<CandleIdentity, PersistedCandle> persistedByIdentity = new LinkedHashMap<>();
        for (Map.Entry<BatchIdentity, List<Candle>> group : groups.entrySet()) {
            TradingPair pair = references.resolveTradingPair(
                    group.getValue().getFirst().key().tradingPair());
            for (PersistedCandle persisted : persistResolvedGroup(group.getValue(), pair)) {
                CandleIdentity identity = new CandleIdentity(
                        group.getKey(), persisted.candle().key().openTime());
                persistedByIdentity.put(identity, persisted);
            }
        }

        return ordered.stream()
                .map(candle -> requirePersisted(
                        persistedByIdentity.get(CandleIdentity.from(candle)), candle))
                .toList();
    }

    List<PersistedCandle> saveClosedBatch(
            List<Candle> candles,
            TradingPair resolvedPair) {
        Objects.requireNonNull(resolvedPair, "resolvedPair");
        List<Candle> ordered = validateAndOrder(candles);
        if (ordered.isEmpty()) return List.of();

        BatchIdentity expected = BatchIdentity.from(ordered.getFirst());
        BatchIdentity resolvedIdentity = BatchIdentity.from(
                ordered.getFirst().key().provider(),
                resolvedPair,
                ordered.getFirst().key().timeframe());
        if (!expected.equals(resolvedIdentity)
                || ordered.stream().anyMatch(candle -> !expected.equals(BatchIdentity.from(candle)))) {
            throw new MarketDataException(
                    MarketDataErrorCode.INVALID_MARKET_QUERY,
                    "A resolved Candle batch must share provider, trading pair, and timeframe");
        }
        return persistResolvedGroup(ordered, resolvedPair);
    }

    @Override
    public List<PersistedCandle> findRange(
            MarketProvider provider,
            TradingPairId pairId,
            Timeframe timeframe,
            Instant start,
            Instant end) {
        return jdbc.query(
                MarketDataSql.FIND_CANDLE_RANGE,
                (rs, row) -> MarketDataRows.candle(rs),
                provider.value(),
                pairId.value(),
                timeframe.code(),
                Timestamp.from(start),
                Timestamp.from(end));
    }

    private List<PersistedCandle> persistResolvedGroup(
            List<Candle> ordered,
            TradingPair resolvedPair) {
        List<CandleInsert> inserts = ordered.stream()
                .map(candle -> new CandleInsert(
                        CandleId.generate(), withPair(candle, resolvedPair)))
                .toList();

        jdbc.batchUpdate(
                MarketDataSql.INSERT_CANDLE,
                inserts,
                BATCH_SIZE,
                (statement, insert) -> {
                    Candle candle = insert.candle();
                    statement.setString(1, insert.candleId().value());
                    statement.setString(2, candle.key().provider().value());
                    statement.setString(3, resolvedPair.tradingPairId().value());
                    statement.setString(4, candle.key().timeframe().code());
                    statement.setTimestamp(5, Timestamp.from(candle.key().openTime()));
                    statement.setTimestamp(6, Timestamp.from(candle.closeTime()));
                    statement.setBigDecimal(7, candle.open());
                    statement.setBigDecimal(8, candle.high());
                    statement.setBigDecimal(9, candle.low());
                    statement.setBigDecimal(10, candle.close());
                    statement.setBigDecimal(11, candle.volume());
                });

        Candle first = inserts.getFirst().candle();
        Candle last = inserts.getLast().candle();
        Map<Instant, PersistedCandle> persistedByOpenTime = new LinkedHashMap<>();
        for (PersistedCandle persisted : findRange(
                first.key().provider(),
                resolvedPair.tradingPairId(),
                first.key().timeframe(),
                first.key().openTime(),
                last.closeTime())) {
            persistedByOpenTime.put(persisted.candle().key().openTime(), persisted);
        }

        List<PersistedCandle> persisted = new ArrayList<>(inserts.size());
        for (CandleInsert insert : inserts) {
            Candle expected = insert.candle();
            PersistedCandle stored = requirePersisted(
                    persistedByOpenTime.get(expected.key().openTime()), expected);
            if (!stored.candle().canonicalContentEquals(expected)) {
                throw new MarketDataException(
                        MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT,
                        "Accepted Candle content differs at " + expected.key().openTime());
            }
            persisted.add(stored);
        }
        return List.copyOf(persisted);
    }

    private static List<Candle> validateAndOrder(List<Candle> candles) {
        Objects.requireNonNull(candles, "candles");
        if (candles.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Candle batch must not contain null values");
        }
        if (candles.stream().anyMatch(candle -> !candle.closed())) {
            throw new MarketDataException(
                    MarketDataErrorCode.INVALID_MARKET_QUERY,
                    "Only closed Candles may persist");
        }
        return candles.stream()
                .sorted(Comparator.comparing(value -> value.key().openTime()))
                .toList();
    }

    private static PersistedCandle requirePersisted(
            PersistedCandle persisted,
            Candle expected) {
        if (persisted == null) {
            throw new MarketDataException(
                    MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT,
                    "Persisted Candle is missing at " + expected.key().openTime());
        }
        return persisted;
    }

    private static Candle withPair(Candle candle, TradingPair pair) {
        return new Candle(
                new CandleKey(
                        candle.key().provider(),
                        pair,
                        candle.key().timeframe(),
                        candle.key().openTime()),
                candle.closeTime(),
                candle.open(),
                candle.high(),
                candle.low(),
                candle.close(),
                candle.volume(),
                candle.closed());
    }

    private record CandleInsert(CandleId candleId, Candle candle) { }

    private record BatchIdentity(
            String provider,
            String baseAsset,
            String quoteAsset,
            String timeframe) {
        private static BatchIdentity from(Candle candle) {
            return from(
                    candle.key().provider(),
                    candle.key().tradingPair(),
                    candle.key().timeframe());
        }

        private static BatchIdentity from(
                MarketProvider provider,
                TradingPair pair,
                Timeframe timeframe) {
            return new BatchIdentity(
                    provider.value(),
                    pair.baseAsset().symbol().value(),
                    pair.quoteAsset().symbol().value(),
                    timeframe.code());
        }
    }

    private record CandleIdentity(BatchIdentity batch, Instant openTime) {
        private static CandleIdentity from(Candle candle) {
            return new CandleIdentity(
                    BatchIdentity.from(candle), candle.key().openTime());
        }
    }
}
