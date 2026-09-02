package com.cryptostrategy.platform.api.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.idempotency.IdempotencyService;
import com.cryptostrategy.platform.api.realtime.WorkEventBridge;
import com.cryptostrategy.platform.api.transport.PageResponseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Fast regression budgets for the in-process public boundary, not a production benchmark. */
class PublicApiPerformanceTest {
    @Test
    void boundedReadAndCommandAcceptanceP95StayBelowTwoSeconds() {
        PageResponseMapper pages = new PageResponseMapper();
        List<Integer> lookahead = java.util.stream.IntStream.rangeClosed(1, 101)
                .boxed()
                .toList();
        IdempotencyService idempotency = mock(IdempotencyService.class);
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(idempotency.canonicalRequestHash(owner, "START_BACKTEST", "request"))
                .thenReturn("sha256:" + "a".repeat(64));
        var commands = new IdempotencyCommandExecutor(idempotency, new ObjectMapper());

        List<Long> readLatencies = new ArrayList<>();
        List<Long> commandLatencies = new ArrayList<>();
        for (int index = 0; index < 200; index++) {
            long readStart = System.nanoTime();
            var page = pages.mapLookahead(lookahead, 100, Object::toString);
            readLatencies.add(System.nanoTime() - readStart);
            assertThat(page.items()).hasSize(100);

            long commandStart = System.nanoTime();
            String accepted = commands.execute(
                    owner,
                    "START_BACKTEST",
                    "key-" + index,
                    "request",
                    (key, hash) -> "QUEUED");
            commandLatencies.add(System.nanoTime() - commandStart);
            assertThat(accepted).isEqualTo("QUEUED");
        }

        assertThat(p95(readLatencies)).isLessThan(2_000_000_000L);
        assertThat(p95(commandLatencies)).isLessThan(2_000_000_000L);
    }

    @Test
    void realtimeFanOutP95StaysBelowOneSecond() {
        WorkEventBridge bridge = new WorkEventBridge();
        String experimentId = "01J00000000000000000000001";
        List<Long> latencies = new ArrayList<>();
        bridge.subscribe(
                WorkEventBridge.Kind.EXPERIMENT,
                experimentId,
                "correlation",
                "subscription",
                event -> latencies.add(System.nanoTime()));

        for (int index = 0; index < 200; index++) {
            long started = System.nanoTime();
            bridge.publishProgress(
                    experimentId,
                    "01J00000000000000000000002",
                    "RUNNING",
                    index,
                    0,
                    200,
                    null,
                    "correlation",
                    Instant.EPOCH);
            latencies.set(latencies.size() - 1, latencies.getLast() - started);
        }

        assertThat(p95(latencies)).isLessThan(1_000_000_000L);
    }

    private static long p95(List<Long> samples) {
        List<Long> ordered = samples.stream().sorted(Comparator.naturalOrder()).toList();
        return ordered.get((int) Math.ceil(ordered.size() * 0.95) - 1);
    }
}
