package com.cryptostrategy.platform.worker.scheduler;

import com.cryptostrategy.platform.worker.engine.RecoverySweeperEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ScheduledRecoverySweeper {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRecoverySweeper.class);

    private final RecoverySweeperEngine engine;

    public ScheduledRecoverySweeper(RecoverySweeperEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    }

    @Scheduled(fixedDelayString = "${worker.reconciliation.queue-interval-ms:30000}")
    public void sweepUnqueuedJobs() {
        try {
            int recovered = engine.sweepUnqueuedJobs();
            if (recovered > 0) {
                log.info("Recovery sweep: recovered {} unqueued jobs", recovered);
            }
        } catch (Exception ex) {
            log.error("Error in scheduled sweepUnqueuedJobs: {}", ex.getMessage(), ex);
        }
    }

    @Scheduled(fixedDelayString = "${worker.reconciliation.queue-interval-ms:30000}")
    public void sweepDueRetries() {
        try {
            int requeued = engine.sweepDueRetries();
            if (requeued > 0) {
                log.info("Recovery sweep: requeued {} due retry jobs", requeued);
            }
        } catch (Exception ex) {
            log.error("Error in scheduled sweepDueRetries: {}", ex.getMessage(), ex);
        }
    }

    @Scheduled(fixedDelayString = "${worker.reconciliation.stale-interval-ms:30000}")
    public void sweepStaleAttempts() {
        try {
            int cleaned = engine.sweepStaleAttempts();
            if (cleaned > 0) {
                log.info("Recovery sweep: cleaned {} stale running attempts", cleaned);
            }
        } catch (Exception ex) {
            log.error("Error in scheduled sweepStaleAttempts: {}", ex.getMessage(), ex);
        }
    }

    @Scheduled(fixedDelayString = "${worker.reconciliation.stop-completion-interval-ms:5000}")
    public void sweepStoppedExperiments() {
        try {
            int completed = engine.sweepStoppedExperiments();
            if (completed > 0) {
                log.info("Recovery sweep: finalized {} stopped experiments", completed);
            }
        } catch (Exception ex) {
            log.error("Error in scheduled sweepStoppedExperiments: {}", ex.getMessage(), ex);
        }
    }
}
