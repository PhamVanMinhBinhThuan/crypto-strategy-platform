package com.cryptostrategy.platform.worker.search.reconciliation;

import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.worker.search.coordination.SearchCoordinator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import com.cryptostrategy.platform.execution.api.port.in.SearchReproductionVerificationUseCase;

/** Phục hồi intent bị mất từ durable Search Run; queue/cache không phải source of truth. */
public final class SearchReconciler {
    private static final Logger log = LoggerFactory.getLogger(SearchReconciler.class);
    private final SearchRunStore runs;
    private final SearchCoordinator coordinator;
    private final Clock clock;
    private final Duration staleAfter;
    private final int batchSize;
    private final SearchReproductionVerificationUseCase reproductions;

    public SearchReconciler(
            SearchRunStore runs,
            SearchCoordinator coordinator,
            Clock clock,
            Duration staleAfter,
            int batchSize) {
        this(runs, coordinator, clock, staleAfter, batchSize, null);
    }

    public SearchReconciler(SearchRunStore runs, SearchCoordinator coordinator,
            Clock clock, Duration staleAfter, int batchSize,
            SearchReproductionVerificationUseCase reproductions) {
        this.runs = Objects.requireNonNull(runs, "runs");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.staleAfter = Objects.requireNonNull(staleAfter, "staleAfter");
        if (staleAfter.isNegative() || staleAfter.isZero()) throw new IllegalArgumentException("staleAfter must be positive");
        if (batchSize <= 0) throw new IllegalArgumentException("batchSize must be positive");
        this.batchSize = batchSize;
        this.reproductions = reproductions;
    }

    @Scheduled(fixedDelayString = "${worker.search.recovery-interval-ms:5000}")
    public void reconcile() {
        Instant observedAt = clock.instant();
        runs.findRecoverable(observedAt.minus(staleAfter), batchSize).forEach(run -> {
            try {
                coordinator.reconcile(run.experimentId().value(), observedAt,
                        "search-recovery:" + run.searchRunId().value());
            } catch (RuntimeException failure) {
                log.warn("Search reconciliation failed for run '{}': {}",
                        run.searchRunId().value(), failure.getMessage());
            }
        });
        if (reproductions != null) reproductions.reconcile(batchSize);
    }
}
