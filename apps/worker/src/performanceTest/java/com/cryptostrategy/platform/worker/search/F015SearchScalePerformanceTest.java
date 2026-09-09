package com.cryptostrategy.platform.worker.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.search.api.model.CompositeSearchSpace;
import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchStrategyPoolEntry;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class F015SearchScalePerformanceTest {
    private static final long SEED = 15015L;

    @Test
    void oneThousandCandidatesRemainDeterministicAcrossWorkerCapacities() throws Exception {
        Profile narrow = profile(1_000, 2, 4, true);
        Profile wider = profile(1_000, 8, 16, true);
        report("1000-workers-2", narrow);
        report("1000-workers-8", wider);

        assertThat(narrow.outcomes()).containsExactlyEntriesOf(wider.outcomes());
        assertThat(narrow.generated()).isEqualTo(1_000);
        assertThat(wider.generated()).isEqualTo(1_000);
        assertThat(narrow.peakActive()).isLessThanOrEqualTo(2);
        assertThat(wider.peakActive()).isLessThanOrEqualTo(8);
    }

    @Test
    void tenThousandCandidatesUseBoundedActiveAndPendingWork() throws Exception {
        Profile result = profile(10_000, 8, 32, false);
        report("10000-backpressure", result);

        assertThat(result.generated()).isEqualTo(10_000);
        assertThat(result.completed()).isEqualTo(10_000);
        assertThat(result.peakActive()).isLessThanOrEqualTo(8);
        assertThat(result.peakPending()).isLessThanOrEqualTo(32);
        assertThat(result.peakHeapBytes() - result.baselineHeapBytes())
                .isLessThan(256L * 1024 * 1024);
        assertThat(result.outcomes()).isEmpty();
        assertThat(result.elapsed()).isLessThan(Duration.ofMinutes(2));
    }

    private static Profile profile(
            int budget, int workers, int queueCapacity, boolean retainEvidence) throws Exception {
        var runStore = mock(SearchRunStore.class);
        var generation = SearchModuleFactory.baseline(runStore).generation();
        var descriptor = SearchModuleFactory.baselineDefinition(SEED);
        CompositeSearchSpace space = space();
        Optional<GeneratorState> state = Optional.empty();
        Map<Integer, String> outcomes = retainEvidence
                ? new ConcurrentHashMap<>() : Map.of();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peakActive = new AtomicInteger();
        AtomicInteger peakPending = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        AtomicLong rollingEvidence = new AtomicLong();
        long baselineHeap = usedHeap();
        AtomicLong peakHeap = new AtomicLong(baselineHeap);
        CountDownLatch finished = new CountDownLatch(budget);
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                workers, workers, 0L, TimeUnit.MILLISECONDS, queue,
                (task, rejected) -> {
                    try {
                        rejected.getQueue().put(task);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while applying bounded backpressure", interrupted);
                    }
                });
        long started = System.nanoTime();
        try {
            for (int index = 0; index < budget; index++) {
                var outcome = generation.generate(
                        descriptor.descriptor().generatorId(), descriptor.descriptor().generatorVersion(),
                        GenerationRequest.composite(space, SEED, state, index, Set.of(), 1));
                var generated = (GenerationOutcome.Generated) outcome;
                state = Optional.of(generated.nextState());
                String fingerprint = generated.candidate().fingerprint();
                int generationIndex = index;
                executor.execute(() -> {
                    int current = active.incrementAndGet();
                    peakActive.accumulateAndGet(current, Math::max);
                    try {
                        long evidence = deterministicBacktestEvidence(fingerprint);
                        rollingEvidence.accumulateAndGet(evidence, (left, right) -> left ^ right);
                        if (retainEvidence) {
                            outcomes.put(generationIndex, fingerprint + ':'
                                    + Long.toUnsignedString(evidence, 16));
                        }
                        completed.incrementAndGet();
                        peakHeap.accumulateAndGet(usedHeap(), Math::max);
                    } finally {
                        active.decrementAndGet();
                        finished.countDown();
                    }
                });
                peakPending.accumulateAndGet(queue.size(), Math::max);
                peakHeap.accumulateAndGet(usedHeap(), Math::max);
            }
            assertThat(finished.await(2, TimeUnit.MINUTES)).isTrue();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
        assertThat(rollingEvidence.get()).isNotZero();
        Map<Integer, String> ordered = retainEvidence
                ? Map.copyOf(new LinkedHashMap<>(outcomes.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue,
                                (left, right) -> left, LinkedHashMap::new))))
                : Map.of();
        return new Profile(budget, completed.get(), peakActive.get(),
                peakPending.get(), elapsed, rollingEvidence.get(), baselineHeap,
                peakHeap.get(), ordered);
    }

    private static CompositeSearchSpace space() {
        List<StrategyParameterValue> values = new ArrayList<>(100);
        for (long value = 0; value < 100; value++) {
            values.add(new StrategyParameterValue.IntegerValue(value));
        }
        var entry = new SearchStrategyPoolEntry(
                new StrategyReference(new StrategyVersionId("01J00000000000000000000991"),
                        new StrategyPluginId("scale-profile"), SemanticVersion.parse("1.0.0")),
                Map.of(
                        "fastPeriod", new SearchParameterDomain(ParameterType.INTEGER, values),
                        "slowPeriod", new SearchParameterDomain(ParameterType.INTEGER, values)));
        return new CompositeSearchSpace(
                List.of(entry), 1, 1, SearchCombinationPolicy.majorityVote(), List.of());
    }

    private static long deterministicBacktestEvidence(String fingerprint) {
        long result = 0xcbf29ce484222325L;
        for (int round = 0; round < 16; round++) {
            for (int index = 0; index < fingerprint.length(); index++) {
                result ^= fingerprint.charAt(index);
                result *= 0x100000001b3L;
            }
        }
        return result;
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void report(String name, Profile profile) {
        double throughput = profile.generated() / Math.max(0.001, profile.elapsed().toNanos() / 1_000_000_000.0);
        System.out.printf(
                "F015_PROFILE name=%s generated=%d completed=%d workersPeak=%d pendingPeak=%d elapsedMs=%d throughputPerSecond=%.2f evidence=%s heapBaselineBytes=%d heapPeakBytes=%d heapDeltaBytes=%d retained=%d%n",
                name, profile.generated(), profile.completed(), profile.peakActive(), profile.peakPending(),
                profile.elapsed().toMillis(), throughput,
                Long.toUnsignedString(profile.rollingEvidence(), 16), profile.baselineHeapBytes(),
                profile.peakHeapBytes(), profile.peakHeapBytes() - profile.baselineHeapBytes(),
                profile.outcomes().size());
    }

    private record Profile(
            int generated,
            int completed,
            int peakActive,
            int peakPending,
            Duration elapsed,
            long rollingEvidence,
            long baselineHeapBytes,
            long peakHeapBytes,
            Map<Integer, String> outcomes) {}
}
