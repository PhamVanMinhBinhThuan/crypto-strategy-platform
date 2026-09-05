package com.cryptostrategy.platform.api.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.config.MarketDataProperties;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.port.in.CreateDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.ListDatasetsUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FrozenDatasetApiTest {
    @Test
    void listsOnlyDatasetsGrantedToTheAuthenticatedOwnerWithCompleteProvenance() {
        UUID owner = UUID.fromString("00000000-0000-4000-8000-000000000701");
        Instant now = Instant.parse("2026-09-05T00:00:00Z");
        ListDatasetsUseCase datasets = mock(ListDatasetsUseCase.class);
        DatasetSnapshot snapshot = snapshot(now);
        when(datasets.listRecentDatasets(owner, 25)).thenReturn(List.of(snapshot));
        DatasetController controller = new DatasetController(mock(CreateDatasetUseCase.class),
                mock(GetDatasetUseCase.class), datasets, mock(MarketRequestMapper.class),
                mock(MarketDataProperties.class), mock(IdempotencyCommandExecutor.class));

        var response = controller.list(new AuthenticatedUserContext(owner, now.plusSeconds(60)), 25);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.datasetId()).isEqualTo(snapshot.datasetVersionId());
            assertThat(item.pair()).isEqualTo("BTC/USDT");
            assertThat(item.startTime()).isEqualTo(snapshot.rangeStart());
            assertThat(item.endTime()).isEqualTo(snapshot.rangeEnd());
            assertThat(item.checksum()).isEqualTo(snapshot.checksum());
        });
        verify(datasets).listRecentDatasets(owner, 25);
    }

    private static DatasetSnapshot snapshot(Instant now) {
        Asset btc = new Asset(new AssetId("01J00000000000000000000701"),
                new AssetSymbol("BTC"), Optional.of("Bitcoin"), true);
        Asset usdt = new Asset(new AssetId("01J00000000000000000000702"),
                new AssetSymbol("USDT"), Optional.of("Tether"), true);
        TradingPair pair = new TradingPair(new TradingPairId("01J00000000000000000000703"),
                btc, usdt, true);
        return new DatasetSnapshot(new DatasetVersionId("01J00000000000000000000704"),
                "candle-v1", MarketProvider.BINANCE, pair, Timeframe.ONE_HOUR, "binance-v1",
                now.minusSeconds(86400), now, 24, "sha256:" + "a".repeat(64), now);
    }
}
