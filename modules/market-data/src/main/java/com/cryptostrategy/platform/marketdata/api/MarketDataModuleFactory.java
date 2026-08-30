package com.cryptostrategy.platform.marketdata.api;

import com.cryptostrategy.platform.marketdata.api.port.in.CreateDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.LoadHistoricalCandlesUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.SubscribeCandlesUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.VerifyDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.out.ClosedCandleStore;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetStore;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import com.cryptostrategy.platform.marketdata.internal.application.DatasetAssembler;
import com.cryptostrategy.platform.marketdata.internal.application.DatasetIntegrityVerifier;
import com.cryptostrategy.platform.marketdata.internal.application.DatasetService;
import com.cryptostrategy.platform.marketdata.internal.application.HistoricalCandleService;
import com.cryptostrategy.platform.marketdata.internal.application.RealtimeSubscriptionService;
import com.cryptostrategy.platform.marketdata.internal.checksum.CandleV1Checksum;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.BinanceCandleMapper;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.BinanceHistoricalProvider;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.BinanceStreamProvider;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.BinanceRetryPolicy;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.transport.JdkBinanceRestTransport;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.transport.JdkBinanceStreamTransport;
import com.cryptostrategy.platform.marketdata.internal.provider.fixture.FixtureMarketDataProvider;
import com.cryptostrategy.platform.marketdata.internal.realtime.SharedSubscriptionRegistry;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

public final class MarketDataModuleFactory {
    private MarketDataModuleFactory() { }
    public static MarketDataProvider fixtureProvider(List<Candle> candles) {
        return new FixtureMarketDataProvider(List.copyOf(candles));
    }
    public static MarketDataProvider binanceProvider(URI restBaseUrl, URI streamBaseUrl,
            Duration connectTimeout, Duration requestTimeout, String normalizationVersion) {
        return binanceProvider(restBaseUrl, streamBaseUrl, connectTimeout, requestTimeout, normalizationVersion,
                3, Duration.ofMillis(250), Duration.ofSeconds(5));
    }
    public static MarketDataProvider binanceProvider(URI restBaseUrl, URI streamBaseUrl,
            Duration connectTimeout, Duration requestTimeout, String normalizationVersion,
            int retryAttempts, Duration retryInitialDelay, Duration retryMaxDelay) {
        ObjectMapper mapper = new ObjectMapper();
        BinanceCandleMapper candleMapper = new BinanceCandleMapper(mapper);
        HttpClient streamClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        MarketDataProvider stream = new BinanceStreamProvider(
                new JdkBinanceStreamTransport(streamBaseUrl, streamClient), candleMapper, normalizationVersion);
        return new BinanceHistoricalProvider(
                new JdkBinanceRestTransport(restBaseUrl, connectTimeout, requestTimeout),
                candleMapper, normalizationVersion, stream,
                new BinanceRetryPolicy(retryAttempts, retryInitialDelay, retryMaxDelay, Thread::sleep));
    }
    public static Components create(MarketDataProvider provider, ClosedCandleStore candles, DatasetStore datasets, DatasetCandleReader reader, Clock clock) {
        CandleV1Checksum checksum = new CandleV1Checksum(); HistoricalCandleService historical = new HistoricalCandleService(provider);
        DatasetService dataset = new DatasetService(historical, new DatasetAssembler(clock, checksum), datasets, new DatasetIntegrityVerifier(reader, checksum));
        RealtimeSubscriptionService realtime = new RealtimeSubscriptionService(new SharedSubscriptionRegistry(provider), candles);
        return new Components(historical, dataset, dataset, dataset, realtime);
    }
    public record Components(LoadHistoricalCandlesUseCase historical, CreateDatasetUseCase createDataset,
                             GetDatasetUseCase getDataset, VerifyDatasetUseCase verifyDataset,
                             SubscribeCandlesUseCase realtime) { }
}
