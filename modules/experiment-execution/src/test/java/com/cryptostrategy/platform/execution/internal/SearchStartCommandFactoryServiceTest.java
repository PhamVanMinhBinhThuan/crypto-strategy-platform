package com.cryptostrategy.platform.execution.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.execution.api.port.in.RequestedGeneratorId;
import com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.strategy.api.StrategyModuleFactory;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyComponent;
import com.cryptostrategy.platform.strategy.api.model.user.query.ResolveStrategySnapshotQuery;
import com.cryptostrategy.platform.strategy.api.port.in.ResolveStrategySnapshotUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SearchStartCommandFactoryServiceTest {
    private static final UUID OWNER =
            UUID.fromString("91000000-0000-4000-8000-000000000014");
    private static final DatasetVersionId DATASET_ID =
            new DatasetVersionId("01J00000000000000000000401");
    private static final UserStrategyVersionId USER_VERSION_ID =
            new UserStrategyVersionId("01J00000000000000000000402");
    private static final Instant NOW = Instant.parse("2026-09-04T03:00:00Z");

    @Test
    void resolvesPublishedCompositeByOwnerAndFreezesItIntoManifest() {
        GetDatasetUseCase datasets = mock(GetDatasetUseCase.class);
        DatasetSnapshot dataset = dataset();
        when(datasets.getDataset(DATASET_ID)).thenReturn(dataset);
        StrategyRegistry registry = mock(StrategyRegistry.class);
        ResolveStrategySnapshotUseCase userStrategies = mock(ResolveStrategySnapshotUseCase.class);
        StrategyFingerprintCalculator fingerprints = StrategyModuleFactory.fingerprints();
        CompositeStrategyDraftSource source = compositeSource();
        String fingerprint = fingerprints.composite(
                source.policyId(),
                source.policyVersion(),
                source.policyParameters(),
                source.components().stream()
                        .map(component -> new StrategyFingerprintCalculator.Component(
                                component.strategyReference(), component.parameters()))
                        .toList());
        when(userStrategies.resolveSnapshot(OWNER, new ResolveStrategySnapshotQuery(USER_VERSION_ID)))
                .thenReturn(new CompositeStrategySnapshot(
                        new UserStrategyId("01J00000000000000000000403"),
                        USER_VERSION_ID,
                        1,
                        OWNER,
                        source,
                        fingerprint));

        var command = service(datasets, registry, userStrategies, fingerprints).create(request());
        StrategyProvenanceSnapshot provenance = command.manifest().strategyProvenance();

        assertThat(provenance.kind().name()).isEqualTo("COMPOSITE");
        assertThat(provenance.sourceUserStrategyVersionId()).contains(USER_VERSION_ID);
        assertThat(provenance.components()).hasSize(2);
        assertThat(command.manifest().searchConfig()).containsEntry("strategyKind", "COMPOSITE")
                .containsEntry("userStrategyVersionId", USER_VERSION_ID.value())
                .containsEntry("searchSpace", Map.of());
        assertThat(command.searchRun().stopConditions().maximumCandidates()).isEqualTo(1);
        verify(userStrategies).resolveSnapshot(
                OWNER, new ResolveStrategySnapshotQuery(USER_VERSION_ID));
    }

    @Test
    void requiresExactlyOneOwnedStrategySource() {
        GetDatasetUseCase datasets = mock(GetDatasetUseCase.class);
        ResolveStrategySnapshotUseCase userStrategies = mock(ResolveStrategySnapshotUseCase.class);
        StrategyFingerprintCalculator fingerprints = StrategyModuleFactory.fingerprints();
        SearchStartCommandFactoryService service = service(
                datasets, mock(StrategyRegistry.class), userStrategies, fingerprints);
        SearchStartCommandFactory.Request both = new SearchStartCommandFactory.Request(
                OWNER,
                "key",
                "hash",
                "correlation",
                "invalid",
                DATASET_ID,
                new RequestedGeneratorId("random-search"),
                "1.0.0",
                14L,
                USER_VERSION_ID,
                new StrategyPluginId("ma-crossover"),
                "1.0.0",
                Map.of(),
                1,
                60,
                1);

        assertThatThrownBy(() -> service.create(both))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one");
    }

    private static SearchStartCommandFactory.Request request() {
        return new SearchStartCommandFactory.Request(
                OWNER,
                "f014-user-strategy",
                "request-hash",
                "correlation-014",
                "Published composite Search",
                DATASET_ID,
                new RequestedGeneratorId("random-search"),
                "1.0.0",
                14L,
                USER_VERSION_ID,
                null,
                null,
                Map.of(),
                1,
                60,
                1);
    }

    private static SearchStartCommandFactoryService service(
            GetDatasetUseCase datasets,
            StrategyRegistry registry,
            ResolveStrategySnapshotUseCase userStrategies,
            StrategyFingerprintCalculator fingerprints) {
        return new SearchStartCommandFactoryService(
                datasets,
                registry,
                userStrategies,
                fingerprints,
                new ObjectMapper().findAndRegisterModules(),
                "f014",
                "commit",
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static CompositeStrategyDraftSource compositeSource() {
        StrategyParameterSet empty = StrategyParameterSet.empty();
        return new CompositeStrategyDraftSource(
                new CombinationPolicyId("majority-vote"),
                SemanticVersion.parse("1.0.0"),
                empty,
                List.of(
                        new UserStrategyComponent(reference("ma-crossover", '1'), empty),
                        new UserStrategyComponent(reference("rsi-threshold", '2'), empty)));
    }

    private static StrategyReference reference(String pluginId, char suffix) {
        return new StrategyReference(
                new StrategyVersionId("01J0000000000000000000040" + suffix),
                new StrategyPluginId(pluginId),
                SemanticVersion.parse("1.0.0"));
    }

    private static DatasetSnapshot dataset() {
        TradingPair pair = mock(TradingPair.class);
        when(pair.canonicalSymbol()).thenReturn("BTC/USDT");
        return new DatasetSnapshot(
                DATASET_ID,
                "candle-v1",
                MarketProvider.BINANCE,
                pair,
                Timeframe.ONE_HOUR,
                "binance-v1",
                NOW.minusSeconds(3600),
                NOW,
                1,
                "sha256:" + "a".repeat(64),
                NOW);
    }
}
