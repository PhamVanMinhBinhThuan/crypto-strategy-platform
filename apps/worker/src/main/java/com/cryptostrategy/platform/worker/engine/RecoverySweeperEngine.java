package com.cryptostrategy.platform.worker.engine;

import com.cryptostrategy.platform.contracts.api.BacktestJobPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
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
                            Ulids.generate(),
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
                if (due.candidateId() != null) {
                    BacktestJobPayload payload = new BacktestJobPayload(
                            due.experimentId().value(),
                            due.jobId().value(),
                            due.candidateId().value()
                    );
                    MessageEnvelope<BacktestJobPayload> envelope = new MessageEnvelope<>(
                            Ulids.generate(),
                            1,
                            MessageTypes.BACKTEST_JOB,
                            Instant.now(),
                            due.jobId().value(),
                            payload
                    );
                    String serialized = objectMapper.writeValueAsString(envelope);
                    streamPublisher.publish(
                            workerProperties.streams().getBacktestJobsStream(),
                            envelope.messageId(),
                            serialized,
                            Map.of("messageType", MessageTypes.BACKTEST_JOB, "correlationId", due.jobId().value())
                    );
                }
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
                Instant nextRetry = Instant.now().plus(workerProperties.retry().baseDelay());
                experimentUseCase.finalizeFailure(
                        stale.jobId(),
                        stale.attemptId(),
                        "STALE_TIMEOUT",
                        "Attempt timed out / worker heartbeat stale",
                        FailureClassification.WORKER_CRASHED,
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
}
