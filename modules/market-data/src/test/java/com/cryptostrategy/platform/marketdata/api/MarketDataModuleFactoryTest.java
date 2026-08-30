package com.cryptostrategy.platform.marketdata.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.model.CandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.DatasetFinalization;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import com.cryptostrategy.platform.marketdata.api.port.in.CreateDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.LoadHistoricalCandlesUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.SubscribeCandlesUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.VerifyDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.out.ClosedCandleStore;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetStore;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketDataModuleFactoryTest {
    @Test
    void createsEveryPublicInputPortUsingOnlyPublicCollaborators() {
        var components = MarketDataModuleFactory.create(
                MarketDataModuleFactory.fixtureProvider(List.of()),
                closedCandleStore(),
                datasetStore(),
                emptyReader(),
                Clock.systemUTC());

        assertInstanceOf(LoadHistoricalCandlesUseCase.class, components.historical());
        assertInstanceOf(CreateDatasetUseCase.class, components.createDataset());
        assertInstanceOf(GetDatasetUseCase.class, components.getDataset());
        assertInstanceOf(VerifyDatasetUseCase.class, components.verifyDataset());
        assertInstanceOf(SubscribeCandlesUseCase.class, components.realtime());
    }

    @Test
    void publicFactorySignaturesDoNotExposeInternalTypes() {
        for (Method method : MarketDataModuleFactory.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            assertPublicBoundaryType(method.getReturnType());
            for (Class<?> parameterType : method.getParameterTypes()) {
                assertPublicBoundaryType(parameterType);
            }
        }
        assertNotNull(MarketDataModuleFactory.RecoverySettings.class.getRecordComponents());
    }

    private static void assertPublicBoundaryType(Class<?> type) {
        assertFalse(type.getName().contains(".internal."), () -> "Internal type leaked: " + type.getName());
    }

    private static DatasetCandleReader emptyReader() {
        return (datasetId, fromSequence, batchSize) ->
                new CandleBatch(datasetId, fromSequence, List.of(), fromSequence, false);
    }

    private static DatasetStore datasetStore() {
        return new DatasetStore() {
            @Override
            public DatasetSnapshot finalizeAtomically(DatasetFinalization finalization) {
                throw new UnsupportedOperationException("Not invoked by composition test");
            }

            @Override
            public Optional<DatasetSnapshot> find(DatasetVersionId datasetId) {
                return Optional.empty();
            }

            @Override
            public Optional<DatasetSnapshot> findByChecksum(String checksum) {
                return Optional.empty();
            }
        };
    }

    private static ClosedCandleStore closedCandleStore() {
        return new ClosedCandleStore() {
            @Override
            public PersistedCandle saveClosed(Candle candle) {
                throw new UnsupportedOperationException("Not invoked by composition test");
            }

            @Override
            public List<PersistedCandle> saveClosedBatch(List<Candle> candles) {
                return List.of();
            }

            @Override
            public List<PersistedCandle> findRange(
                    MarketProvider provider,
                    TradingPairId tradingPairId,
                    Timeframe timeframe,
                    Instant startInclusive,
                    Instant endExclusive) {
                return List.of();
            }
        };
    }
}
