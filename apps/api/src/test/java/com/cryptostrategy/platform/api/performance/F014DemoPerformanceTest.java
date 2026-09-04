package com.cryptostrategy.platform.api.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.internal.DeterministicBacktestEngine;
import com.cryptostrategy.platform.backtesting.internal.ResolvedBacktestRun;
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
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.marketdata.api.model.CandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.DatasetMembership;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.strategies.api.StrategyPlugins;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Repeatable in-process compute benchmark for F014. This is not a production SLA and deliberately
 * excludes application startup, network, Redis and PostgreSQL latency. T055 publishes all raw runs
 * and limitations instead of treating this harness as live end-to-end evidence.
 */
class F014DemoPerformanceTest {
    private static final int RUNS = 3;
    private static final int CANDIDATES = 12;
    private static final int CANDLES = 2_000;
    private static final int BATCH_SIZE = 256;
    private static final int PARALLELISM = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final long CANDIDATE_P95_BUDGET_NANOS = Duration.ofSeconds(2).toNanos();

    @Test
    void measuresThreeRunsOfTheSameWorkloadAndRejectsTimeoutsOrDuplicateResults()
            throws Exception {
        Workload workload = Workload.create();
        warmUp(workload);

        List<Double> speedups = new ArrayList<>();
        for (int run = 1; run <= RUNS; run++) {
            // Alternate order so the 3-worker profile is not always favoured by later JIT/cache state.
            Measurement oneWorker;
            Measurement threeWorkers;
            if (run % 2 == 1) {
                oneWorker = measure(workload, run, 1);
                threeWorkers = measure(workload, run, PARALLELISM);
            } else {
                threeWorkers = measure(workload, run, PARALLELISM);
                oneWorker = measure(workload, run, 1);
            }

            assertValid(oneWorker);
            assertValid(threeWorkers);
            // Throughput is candidates/second, therefore compare parallel / sequential.
            double speedup = threeWorkers.throughputPerSecond() / oneWorker.throughputPerSecond();
            speedups.add(speedup);
            publish(oneWorker, speedup);
            publish(threeWorkers, speedup);
        }

        System.out.printf(
                Locale.ROOT,
                "F014_BENCHMARK_SUMMARY profile=IN_PROCESS_BACKTEST runs=%d candidates_per_run=%d candles_per_candidate=%d median_speedup_3v1=%.3f%n",
                RUNS,
                CANDIDATES,
                CANDLES,
                median(speedups));
    }

    private static void warmUp(Workload workload) throws Exception {
        Measurement warmup = measure(workload, 0, 1, 3);
        assertThat(warmup.completed()).isEqualTo(3);
    }

    private static Measurement measure(Workload workload, int run, int concurrency)
            throws Exception {
        return measure(workload, run, concurrency, CANDIDATES);
    }

    private static Measurement measure(Workload workload, int run, int concurrency, int candidates)
            throws Exception {
        long totalStarted = System.nanoTime();
        var executor = Executors.newFixedThreadPool(concurrency);
        List<Callable<CandidateMeasurement>> tasks = new ArrayList<>();
        for (int index = 0; index < candidates; index++) {
            int candidateIndex = index;
            tasks.add(() -> workload.runCandidate(run, concurrency, candidateIndex));
        }

        long computeStarted = System.nanoTime();
        List<Future<CandidateMeasurement>> futures;
        try {
            futures = executor.invokeAll(tasks, TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        long computeNanos = System.nanoTime() - computeStarted;

        long verificationStarted = System.nanoTime();
        List<CandidateMeasurement> completed = new ArrayList<>();
        int timedOut = 0;
        for (Future<CandidateMeasurement> future : futures) {
            if (future.isCancelled()) timedOut += 1;
            else completed.add(future.get());
        }
        int duplicateResultIds = completed.size()
                - new HashSet<>(completed.stream().map(value -> value.result().resultId()).toList())
                        .size();
        int duplicateCandidateIds = completed.size()
                - new HashSet<>(completed.stream()
                                .map(value -> value.result().candidateId())
                                .toList())
                        .size();
        long verificationNanos = System.nanoTime() - verificationStarted;
        long totalNanos = System.nanoTime() - totalStarted;
        long p95Nanos = percentile95(
                completed.stream().map(CandidateMeasurement::elapsedNanos).toList());
        double throughput = completed.size() / (computeNanos / 1_000_000_000.0);

        return new Measurement(
                run,
                concurrency,
                candidates,
                completed.size(),
                timedOut,
                duplicateResultIds,
                duplicateCandidateIds,
                computeNanos,
                verificationNanos,
                totalNanos,
                p95Nanos,
                throughput);
    }

    private static void assertValid(Measurement value) {
        assertThat(value.completed()).isEqualTo(value.submitted());
        assertThat(value.timedOut()).isZero();
        assertThat(value.duplicateResultIds()).isZero();
        assertThat(value.duplicateCandidateIds()).isZero();
        assertThat(value.candidateP95Nanos()).isLessThan(CANDIDATE_P95_BUDGET_NANOS);
    }

    private static void publish(Measurement value, double speedup) {
        System.out.printf(
                Locale.ROOT,
                "F014_BENCHMARK run=%d profile=IN_PROCESS_BACKTEST concurrency=%d candidates=%d candles_per_candidate=%d batch_size=%d compute_ms=%.3f verification_ms=%.3f total_ms=%.3f candidate_p95_ms=%.3f throughput_candidates_per_second=%.3f completed=%d timeouts=%d duplicate_result_ids=%d duplicate_candidate_ids=%d speedup_3v1=%.3f%n",
                value.run(),
                value.concurrency(),
                value.submitted(),
                CANDLES,
                BATCH_SIZE,
                milliseconds(value.computeNanos()),
                milliseconds(value.verificationNanos()),
                milliseconds(value.totalNanos()),
                milliseconds(value.candidateP95Nanos()),
                value.throughputPerSecond(),
                value.completed(),
                value.timedOut(),
                value.duplicateResultIds(),
                value.duplicateCandidateIds(),
                speedup);
    }

    private static double milliseconds(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static long percentile95(List<Long> values) {
        assertThat(values).isNotEmpty();
        List<Long> ordered = values.stream().sorted(Comparator.naturalOrder()).toList();
        return ordered.get((int) Math.ceil(ordered.size() * 0.95) - 1);
    }

    private static double median(List<Double> values) {
        List<Double> ordered = values.stream().sorted().toList();
        return ordered.get(ordered.size() / 2);
    }

    private record CandidateMeasurement(BacktestResult result, long elapsedNanos) {}

    private record Measurement(
            int run,
            int concurrency,
            int submitted,
            int completed,
            int timedOut,
            int duplicateResultIds,
            int duplicateCandidateIds,
            long computeNanos,
            long verificationNanos,
            long totalNanos,
            long candidateP95Nanos,
            double throughputPerSecond) {}

    private record Workload(
            DatasetSnapshot dataset,
            List<DatasetMembership> members,
            DatasetCandleReader reader,
            Strategy strategy) {
        private static Workload create() {
            Instant start = Instant.parse("2026-09-01T00:00:00Z");
            DatasetVersionId datasetId = new DatasetVersionId(id(1));
            Asset btc = new Asset(new AssetId(id(2)), new AssetSymbol("BTC"), Optional.empty(), true);
            Asset usdt =
                    new Asset(new AssetId(id(3)), new AssetSymbol("USDT"), Optional.empty(), true);
            TradingPair pair =
                    new TradingPair(new TradingPairId(id(4)), btc, usdt, true);
            List<DatasetMembership> members = new ArrayList<>(CANDLES);
            BigDecimal base = new BigDecimal("100000");
            for (int index = 0; index < CANDLES; index++) {
                Instant openTime = start.plusSeconds(index * 60L);
                BigDecimal drift = BigDecimal.valueOf((index % 200) - 100L);
                BigDecimal open = base.add(drift);
                Candle candle = new Candle(
                        new CandleKey(MarketProvider.BINANCE, pair, Timeframe.ONE_MINUTE, openTime),
                        openTime.plusSeconds(60),
                        open,
                        open.add(BigDecimal.TEN),
                        open.subtract(BigDecimal.TEN),
                        open.add(BigDecimal.ONE),
                        BigDecimal.valueOf(10 + index % 5L),
                        true);
                members.add(new DatasetMembership(
                        datasetId,
                        index,
                        new PersistedCandle(new CandleId(id(10_000 + index)), candle)));
            }
            List<DatasetMembership> frozenMembers = List.copyOf(members);
            DatasetSnapshot dataset = new DatasetSnapshot(
                    datasetId,
                    "f014-performance-v1",
                    MarketProvider.BINANCE,
                    pair,
                    Timeframe.ONE_MINUTE,
                    "ohlcv-v1",
                    start,
                    start.plusSeconds(CANDLES * 60L),
                    CANDLES,
                    "sha256:" + "a".repeat(64),
                    start);
            DatasetCandleReader reader = (requestedId, from, size) -> {
                assertThat(requestedId).isEqualTo(datasetId);
                int end = Math.min(from + size, frozenMembers.size());
                return new CandleBatch(
                        datasetId,
                        from,
                        frozenMembers.subList(from, end),
                        end,
                        end < frozenMembers.size());
            };
            var plugin = StrategyPlugins.trusted().stream()
                    .filter(value -> value.descriptor().reference().pluginId().value().equals("ma-crossover"))
                    .findFirst()
                    .orElseThrow();
            Strategy strategy = plugin.create(StrategyParameterSet.of(Map.of(
                    "fastPeriod", new StrategyParameterValue.IntegerValue(5),
                    "slowPeriod", new StrategyParameterValue.IntegerValue(25))));
            return new Workload(dataset, frozenMembers, reader, strategy);
        }

        private CandidateMeasurement runCandidate(
                int run, int concurrency, int candidateIndex) {
            int identityBase = 100_000 + run * 1_000 + concurrency * 100 + candidateIndex * 4;
            ResolvedBacktestRun command = new ResolvedBacktestRun(
                    new ExperimentId(id(identityBase)),
                    new CandidateId(id(identityBase + 1)),
                    new JobId(id(identityBase + 2)),
                    new AttemptId(id(identityBase + 3)),
                    dataset,
                    new BacktestProvenance(
                            "sha256:" + "b".repeat(64),
                            dataset.checksum(),
                            "sha256:" + "c".repeat(64)),
                    BacktestAssumptions.mvp(
                            new BigDecimal("10000"),
                            new BigDecimal("0.001"),
                            new BigDecimal("0.0005")),
                    BATCH_SIZE,
                    25);
            long started = System.nanoTime();
            BacktestResult result =
                    new DeterministicBacktestEngine().run(command, reader, strategy);
            return new CandidateMeasurement(result, System.nanoTime() - started);
        }

        private static String id(int value) {
            return String.format(Locale.ROOT, "%026d", value);
        }
    }
}
