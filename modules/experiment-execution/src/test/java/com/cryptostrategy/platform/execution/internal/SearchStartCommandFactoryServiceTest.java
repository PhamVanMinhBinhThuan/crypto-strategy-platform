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
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSchema;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterDefinition;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategySnapshot;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyComponent;
import com.cryptostrategy.platform.strategy.api.model.user.query.ResolveStrategySnapshotQuery;
import com.cryptostrategy.platform.strategy.api.port.in.ResolveStrategySnapshotUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    @Test
    void expandsExactDecimalRangesAndFreezesCanonicalOptions() {
        GetDatasetUseCase datasets = mock(GetDatasetUseCase.class);
        DatasetSnapshot dataset = dataset();
        when(datasets.getDataset(DATASET_ID)).thenReturn(dataset);
        StrategyRegistry registry = mock(StrategyRegistry.class);
        ResolveStrategySnapshotUseCase userStrategies = mock(ResolveStrategySnapshotUseCase.class);
        StrategyFingerprintCalculator fingerprints = StrategyModuleFactory.fingerprints();
        StrategyReference reference = reference("rsi", '4');
        ParameterDefinition threshold = new ParameterDefinition(
                "buyThreshold", ParameterType.DECIMAL, true,
                Optional.of(new StrategyParameterValue.DecimalValue(new BigDecimal("30"))),
                Optional.of(new BigDecimal("0")), Optional.of(new BigDecimal("100")),
                Set.of(), "Buy threshold");
        StrategyDescriptor descriptor = new StrategyDescriptor(
                reference, "strategy-descriptor-v1", "RSI", "RSI", "momentum",
                Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD), 2,
                new StrategyParameterSchema(List.of(threshold), List.of()), "descriptor-fingerprint");
        when(registry.descriptor(reference.pluginId(), reference.implementationVersion()))
                .thenReturn(descriptor);
        when(registry.resolveParameters(
                org.mockito.ArgumentMatchers.eq(reference.pluginId()),
                org.mockito.ArgumentMatchers.eq(reference.implementationVersion()),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenAnswer(invocation -> StrategyParameterSet.of(invocation.getArgument(2)));
        SearchStartCommandFactory.ParameterDomain decimalRange =
                new SearchStartCommandFactory.ParameterDomain(
                        "DECIMAL_RANGE", new BigDecimal("20.5"), new BigDecimal("21.5"),
                        new BigDecimal("0.5"), List.of());
        SearchStartCommandFactory.Request request = new SearchStartCommandFactory.Request(
                OWNER, "decimal-key", "decimal-hash", "decimal-correlation", "Decimal search",
                DATASET_ID, new RequestedGeneratorId("random-search"), "1.0.0", 42L,
                null, null, null, Map.of(), 100, 60, null, 10,
                List.of(new SearchStartCommandFactory.StrategyPoolEntryRequest(
                        reference.pluginId(), reference.implementationVersion().toString(), null,
                        Map.of("buyThreshold", decimalRange))),
                1, 1, new CombinationPolicyId("majority-vote"), "1.0.0", List.of(), 2);

        var command = service(datasets, registry, userStrategies, fingerprints).create(request);

        assertThat(command.searchRun().stopConditions().maximumCandidates()).isEqualTo(3);
        assertThat(command.manifest().searchConfig().toString())
                .contains("buyThreshold", "20.5", "21", "21.5", "DECIMAL");
    }

    @Test
    void freezesConfiguredBacktestAssumptionsAndRetainsDefaultsWhenOmitted() {
        GetDatasetUseCase datasets = mock(GetDatasetUseCase.class);
        DatasetSnapshot dataset = dataset();
        when(datasets.getDataset(DATASET_ID)).thenReturn(dataset);
        StrategyRegistry registry = mock(StrategyRegistry.class);
        ResolveStrategySnapshotUseCase userStrategies = mock(ResolveStrategySnapshotUseCase.class);
        StrategyFingerprintCalculator fingerprints = StrategyModuleFactory.fingerprints();
        CompositeStrategyDraftSource source = compositeSource();
        when(userStrategies.resolveSnapshot(OWNER, new ResolveStrategySnapshotQuery(USER_VERSION_ID)))
                .thenReturn(new CompositeStrategySnapshot(
                        new UserStrategyId("01J00000000000000000000403"), USER_VERSION_ID, 1,
                        OWNER, source, fingerprints.composite(source.policyId(), source.policyVersion(),
                                source.policyParameters(), source.components().stream()
                                        .map(component -> new StrategyFingerprintCalculator.Component(
                                                component.strategyReference(), component.parameters()))
                                        .toList())));

        SearchStartCommandFactory.Request base = request();
        SearchStartCommandFactory.Request configured = new SearchStartCommandFactory.Request(
                base.ownerUserId(), base.idempotencyKey(), base.canonicalRequestHash(),
                base.correlationId(), base.name(), base.datasetId(), base.generatorId(),
                base.generatorVersion(), base.seed(), base.userStrategyVersionId(),
                base.strategyId(), base.strategyVersion(), base.parameters(),
                base.maximumCandidates(), base.maximumDurationSeconds(),
                base.maximumWithoutImprovement(), base.topK(), base.strategyPool(),
                base.minimumComponents(), base.maximumComponents(), base.combinationPolicyId(),
                base.combinationPolicyVersion(), base.constraints(), base.requestedConcurrency(),
                new SearchStartCommandFactory.BacktestAssumptionsRequest(
                        new BigDecimal("25000.50"), new BigDecimal("0.001"),
                        new BigDecimal("0.0005")));

        var service = service(datasets, registry, userStrategies, fingerprints);
        assertThat(service.create(configured).manifest().backtestConfig())
                .containsEntry("initialCapital", "25000.500000000000")
                .containsEntry("feeRate", "0.0010000000")
                .containsEntry("slippageRate", "0.0005000000");
        assertThat(service.create(base).manifest().backtestConfig())
                .containsEntry("initialCapital", "10000.000000000000")
                .containsEntry("feeRate", "0.0010000000")
                .containsEntry("slippageRate", "0.0000000000");
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
