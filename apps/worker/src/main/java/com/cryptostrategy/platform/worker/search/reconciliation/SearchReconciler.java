package com.cryptostrategy.platform.worker.search.reconciliation;

import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
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
    private final TrustedSearchCoordinationUseCase coordination;
    private final Clock clock;
    private final Duration staleAfter;
    private final int batchSize;
    private final SearchReproductionVerificationUseCase reproductions;

    public SearchReconciler(
            SearchRunStore runs,
            TrustedSearchCoordinationUseCase coordination,
            Clock clock,
            Duration staleAfter,
            int batchSize) {
        this(runs, coordination, clock, staleAfter, batchSize, null);
    }

    public SearchReconciler(SearchRunStore runs, TrustedSearchCoordinationUseCase coordination,
            Clock clock, Duration staleAfter, int batchSize,
            SearchReproductionVerificationUseCase reproductions) {
        this.runs = Objects.requireNonNull(runs, "runs");
        this.coordination = Objects.requireNonNull(coordination, "coordination");
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
                coordination.reconcileRun(new TrustedSearchCoordinationUseCase.ReconciliationTrigger(
                        new com.cryptostrategy.platform.experiment.api.ExperimentId(run.experimentRef()), observedAt, "search-recovery:" + run.searchRunId().value()));
            } catch (RuntimeException failure) {
                log.warn("Search reconciliation failed for run '{}': {}",
                        run.searchRunId().value(), failure.getMessage());
            }
        });
        if (reproductions != null) reproductions.reconcile(batchSize);
    }
}
