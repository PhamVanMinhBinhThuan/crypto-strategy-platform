package com.cryptostrategy.platform.worker.engine;

import com.cryptostrategy.platform.contracts.api.BacktestJobPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.experiment.api.job.DueRetryJob;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.RecoverableQueuedJob;
import com.cryptostrategy.platform.experiment.api.job.StaleRunningAttempt;
import com.cryptostrategy.platform.experiment.api.job.StopCandidateExperiment;
import com.cryptostrategy.platform.experiment.api.port.in.CompleteStoppedExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerRecoveryQueryUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.LifecycleNotificationPublisher;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class RecoverySweeperEngine {

    private static final Logger log = LoggerFactory.getLogger(RecoverySweeperEngine.class);

    private final TrustedWorkerRecoveryQueryUseCase recoveryQueryUseCase;
    private final TrustedWorkerExperimentUseCase experimentUseCase;
    private final CompleteStoppedExperimentUseCase completeStoppedExperimentUseCase;
    private final RedisStreamPublisher streamPublisher;
    private final LifecycleNotificationPublisher lifecycleNotificationPublisher;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;

    public RecoverySweeperEngine(
            TrustedWorkerRecoveryQueryUseCase recoveryQueryUseCase,
            TrustedWorkerExperimentUseCase experimentUseCase,
            CompleteStoppedExperimentUseCase completeStoppedExperimentUseCase,
            RedisStreamPublisher streamPublisher,
            LifecycleNotificationPublisher lifecycleNotificationPublisher,
            WorkerProperties workerProperties,
            ObjectMapper objectMapper
    ) {
        this.recoveryQueryUseCase = Objects.requireNonNull(recoveryQueryUseCase, "recoveryQueryUseCase cannot be null");
        this.experimentUseCase = Objects.requireNonNull(experimentUseCase, "experimentUseCase cannot be null");
        this.completeStoppedExperimentUseCase = Objects.requireNonNull(completeStoppedExperimentUseCase, "completeStoppedExperimentUseCase cannot be null");
        this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher cannot be null");
        this.lifecycleNotificationPublisher = Objects.requireNonNull(lifecycleNotificationPublisher, "lifecycleNotificationPublisher cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public int sweepUnqueuedJobs() {
        Instant cutoff = Instant.now().minus(workerProperties.reconciliation().queueGracePeriod());
        int batchSize = workerProperties.reconciliation().outboxBatchSize();
        List<RecoverableQueuedJob> unqueued = recoveryQueryUseCase.findRecoverableQueuedJobs(cutoff, batchSize);
        int recovered = 0;

        for (RecoverableQueuedJob job : unqueued) {
            try {
                if (job.candidateId() != null) {
                    BacktestJobPayload payload = new BacktestJobPayload(
                            job.experimentId().value(),
                            job.jobId().value(),
                            job.candidateId().value()
                    );
                    MessageEnvelope<BacktestJobPayload> envelope = new MessageEnvelope<>(
                            recoveryMessageId(job),
                            1,
                            MessageTypes.BACKTEST_JOB,
                            Instant.now(),
                            job.jobId().value(),
                            payload
                    );
                    String serialized = objectMapper.writeValueAsString(envelope);
                    streamPublisher.publish(
                            workerProperties.streams().getBacktestJobsStream(),
                            envelope.messageId(),
                            serialized,
                            Map.of("messageType", MessageTypes.BACKTEST_JOB, "correlationId", job.jobId().value())
                    );
                    recovered++;
                }
            } catch (Exception ex) {
                log.error("Failed to republish unqueued job '{}': {}", job.jobId(), ex.getMessage());
            }
        }
        return recovered;
    }

    public int sweepDueRetries() {
        Instant now = Instant.now();
        int batchSize = workerProperties.reconciliation().outboxBatchSize();
        List<DueRetryJob> dueRetries = recoveryQueryUseCase.findDueRetries(now, batchSize);
        int requeued = 0;

        for (DueRetryJob due : dueRetries) {
            try {
                experimentUseCase.requeueDueRetry(due.jobId());
                // requeueDueRetry persists a new JobQueued outbox record in the same
                // transaction. Publishing here as well creates two queue messages.
                requeued++;
            } catch (Exception ex) {
                log.error("Failed to requeue due retry job '{}': {}", due.jobId(), ex.getMessage());
            }
        }
        return requeued;
    }

    public int sweepStaleAttempts() {
        Instant cutoff = Instant.now().minus(workerProperties.reconciliation().staleGracePeriod());
        int batchSize = workerProperties.reconciliation().outboxBatchSize();
        List<StaleRunningAttempt> staleAttempts = recoveryQueryUseCase.findStaleRunningAttempts(cutoff, batchSize);
        int cleaned = 0;

        for (StaleRunningAttempt stale : staleAttempts) {
            try {
                boolean exhausted = stale.attemptNo() >= workerProperties.retry().maxAttempts();
                Instant nextRetry = exhausted
                        ? null
                        : Instant.now().plus(retryDelay(stale.attemptNo()));
                experimentUseCase.finalizeFailure(
                        stale.jobId(),
                        stale.attemptId(),
                        exhausted ? "STALE_RETRY_EXHAUSTED" : "STALE_TIMEOUT",
                        exhausted
                                ? "Worker recovery retry budget exhausted"
                                : "Attempt timed out because the worker heartbeat became stale",
                        exhausted
                                ? FailureClassification.UNKNOWN_ERROR
                                : FailureClassification.WORKER_CRASHED,
                        nextRetry
                );
                cleaned++;
            } catch (Exception ex) {
                log.error("Failed to finalize stale attempt '{}' for job '{}': {}", stale.attemptId(), stale.jobId(), ex.getMessage());
            }
        }
        return cleaned;
    }

    public int sweepStoppedExperiments() {
        int batchSize = workerProperties.reconciliation().outboxBatchSize();
        List<StopCandidateExperiment> candidates = recoveryQueryUseCase.findStopCompletionCandidates(batchSize);
        int completed = 0;

        for (StopCandidateExperiment candidate : candidates) {
            try {
                boolean finished = completeStoppedExperimentUseCase.completeIfEligible(candidate.experimentId());
                if (finished) {
                    lifecycleNotificationPublisher.publishLifecycleNotification(
                            "EXPERIMENT",
                            candidate.experimentId().value(),
                            candidate.experimentId().value(),
                            null,
                            null,
                            "STOPPED",
                            candidate.experimentId().value()
                    );
                    completed++;
                }
            } catch (Exception ex) {
                log.error("Failed to complete stopped experiment candidate '{}': {}", candidate.experimentId(), ex.getMessage());
            }
        }
        return completed;
    }

    private static String recoveryMessageId(RecoverableQueuedJob job) {
        // Stable across repeated sweeps, so dual-layer dedup can recognize the same
        // abandoned business job even when Redis publication is retried.
        return job.jobId().value();
    }

    private java.time.Duration retryDelay(int attemptNo) {
        var retry = workerProperties.retry();
        double scaled = retry.baseDelay().toMillis()
                * Math.pow(retry.multiplier(), Math.max(0, attemptNo - 1));
        long millis = !Double.isFinite(scaled) || scaled >= retry.maxDelay().toMillis()
                ? retry.maxDelay().toMillis()
                : Math.max(retry.baseDelay().toMillis(), Math.round(scaled));
        return java.time.Duration.ofMillis(Math.min(millis, retry.maxDelay().toMillis()));
    }
}
