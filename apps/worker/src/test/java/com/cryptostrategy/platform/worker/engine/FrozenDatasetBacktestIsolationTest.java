package com.cryptostrategy.platform.worker.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cryptostrategy.platform.backtesting.api.BacktestingModuleFactory;
import com.cryptostrategy.platform.backtesting.api.model.BacktestRunCommand;
import com.cryptostrategy.platform.backtesting.api.port.out.ResolvedStrategy;
import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleId;
import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.execution.FrozenBacktestExecution;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.marketdata.api.model.CandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.DatasetIntegrityResult;
import com.cryptostrategy.platform.marketdata.api.model.DatasetMembership;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FrozenDatasetBacktestIsolationTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-4000-8000-000000000801");
    private static final ExperimentId EXPERIMENT = new ExperimentId("01J00000000000000000000801");
    private static final DatasetVersionId DATASET = new DatasetVersionId("01J00000000000000000000802");
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void multipleCandidatesReadTheSameStoredSnapshotWithoutCallingAProvider() {
        Fixture fixture = new Fixture();
        MarketDataProvider provider = mock(MarketDataProvider.class);
        AtomicInteger reads = new AtomicInteger();
        DatasetCandleReader reader = (datasetId, from, size) -> {
            reads.incrementAndGet();
            assertThat(datasetId).isEqualTo(DATASET);
            return new CandleBatch(datasetId, from, fixture.members.subList(from, fixture.members.size()),
                    fixture.members.size(), false);
        };
        Strategy hold = candle -> new StrategyDecision(StrategySignal.HOLD,
                candle.evaluationTime(), fixture.reference, "HOLD", "fixture", Map.of());
        var service = BacktestingModuleFactory.runBacktestService(
                (owner, experiment, candidate, job, attempt) -> fixture.frozen(candidate, job, attempt),
                ignored -> fixture.dataset,
                ignored -> DatasetIntegrityResult.validResult(),
                reader,
                (provenance, candidate) -> new ResolvedStrategy(hold, 1, "strategy-fingerprint"),
                result -> result);

        service.run(command("01J00000000000000000000811", "01J00000000000000000000812",
                "01J00000000000000000000813"));
        service.run(command("01J00000000000000000000821", "01J00000000000000000000822",
                "01J00000000000000000000823"));

        assertThat(reads).hasValue(2);
        verifyNoInteractions(provider);
    }

    private static BacktestRunCommand command(String candidate, String job, String attempt) {
        return new BacktestRunCommand(OWNER, EXPERIMENT, new CandidateId(candidate),
                new JobId(job), new AttemptId(attempt), 10);
    }

    private static final class Fixture {
        private final TradingPair pair;
        private final DatasetSnapshot dataset;
        private final List<DatasetMembership> members;
        private final StrategyReference reference = new StrategyReference(
                new StrategyVersionId("01J00000000000000000000803"),
                new StrategyPluginId("fixture"), SemanticVersion.parse("1.0.0"));

        private Fixture() {
            Asset btc = new Asset(new AssetId("01J00000000000000000000804"),
                    new AssetSymbol("BTC"), Optional.empty(), true);
            Asset usdt = new Asset(new AssetId("01J00000000000000000000805"),
                    new AssetSymbol("USDT"), Optional.empty(), true);
            pair = new TradingPair(new TradingPairId("01J00000000000000000000806"), btc, usdt, true);
            Candle candle = new Candle(new CandleKey(MarketProvider.BINANCE, pair,
                    Timeframe.ONE_MINUTE, START), START.plusSeconds(60), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(101), BigDecimal.valueOf(99), BigDecimal.valueOf(100),
                    BigDecimal.ONE, true);
            members = List.of(new DatasetMembership(DATASET, 0,
                    new PersistedCandle(new CandleId("01J00000000000000000000807"), candle)));
            dataset = new DatasetSnapshot(DATASET, "candle-v1", MarketProvider.BINANCE, pair,
                    Timeframe.ONE_MINUTE, "binance-v1", START, START.plusSeconds(60), 1,
                    "sha256:" + "8".repeat(64), START.plusSeconds(61));
        }

        private FrozenBacktestExecution frozen(CandidateId candidateId, JobId jobId, AttemptId attemptId) {
            var provenance = new DatasetProvenanceSnapshot(DATASET, "candle-v1", dataset.checksum(),
                    "BINANCE", "BTC/USDT", "1m", "binance-v1", START, START.plusSeconds(60), 1);
            var strategy = StrategyProvenanceSnapshot.single(reference, StrategyParameterSet.empty(),
                    Optional.empty(), "strategy-v1:sha256:" + "7".repeat(64));
            var manifest = new ExperimentManifest(EXPERIMENT, "manifest-v2", provenance, strategy,
                    Map.of("assumptionsVersion", "backtest-assumptions-v1", "initialCapital", "1000",
                            "feeRate", "0.001", "slippageRate", "0", "executionPriceRule", "NEXT_CANDLE_OPEN",
                            "positionMode", "LONG_ONLY", "forceCloseAtEnd", true, "roundingMode", "HALF_EVEN"),
                    Map.of("contractVersion", "search-config-v2"), Map.of(), null,
                    "test", "test", "manifest-fingerprint", START);
            var candidate = new CandidateDefinition(candidateId, EXPERIMENT, 0, Map.of(), null,
                    "candidate-fingerprint", START);
            var job = new Job(jobId, EXPERIMENT, candidateId, JobType.BACKTEST, JobStatus.RUNNING,
                    "correlation", 1, 0, 0, null, START, START, null, null, null, null, START, START);
            var attempt = new ExecutionAttempt(attemptId, jobId, candidateId, 1, AttemptStatus.RUNNING,
                    "worker", START, null, null, null, null, false, START);
            var experiment = new Experiment(EXPERIMENT, OWNER, "F-015", ExperimentStatus.RUNNING,
                    null, null, START, null, null, null, START);
            return new FrozenBacktestExecution(experiment, manifest, candidate, job, attempt);
        }
    }
}
