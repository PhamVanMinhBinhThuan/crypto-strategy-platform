package com.cryptostrategy.platform.worker.consumer;

import com.cryptostrategy.platform.contracts.api.CandidateEvaluatedPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.TerminalWorkOutcome;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import com.cryptostrategy.platform.leaderboard.api.port.in.LeaderboardReconciliationUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.LifecycleNotificationPublisher;
import com.cryptostrategy.platform.worker.infra.redis.ProgressEventPublisher;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class CandidateEvaluatedHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(CandidateEvaluatedHandler.class);

    private final DualLayerIdempotencyGuard idempotencyGuard;
    private final TrustedWorkerExperimentUseCase experimentUseCase;
    private final LeaderboardReconciliationUseCase leaderboardReconciliationUseCase;
    private final ProgressEventPublisher progressEventPublisher;
    private final LifecycleNotificationPublisher lifecycleNotificationPublisher;
    private final RedisStreamMessageReader messageReader;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;

    public CandidateEvaluatedHandler(
            DualLayerIdempotencyGuard idempotencyGuard,
            TrustedWorkerExperimentUseCase experimentUseCase,
            LeaderboardReconciliationUseCase leaderboardReconciliationUseCase,
            ProgressEventPublisher progressEventPublisher,
            LifecycleNotificationPublisher lifecycleNotificationPublisher,
            RedisStreamMessageReader messageReader,
            WorkerProperties workerProperties,
            ObjectMapper objectMapper
    ) {
        this.idempotencyGuard = Objects.requireNonNull(idempotencyGuard, "idempotencyGuard cannot be null");
        this.experimentUseCase = Objects.requireNonNull(experimentUseCase, "experimentUseCase cannot be null");
        this.leaderboardReconciliationUseCase = Objects.requireNonNull(leaderboardReconciliationUseCase, "leaderboardReconciliationUseCase cannot be null");
        this.progressEventPublisher = Objects.requireNonNull(progressEventPublisher, "progressEventPublisher cannot be null");
        this.lifecycleNotificationPublisher = Objects.requireNonNull(lifecycleNotificationPublisher, "lifecycleNotificationPublisher cannot be null");
        this.messageReader = Objects.requireNonNull(messageReader, "messageReader cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    public boolean canHandle(String streamKey, String messageType) {
        String candidateStream = workerProperties.streams().getCandidateEvaluatedStream();
        return (streamKey != null && streamKey.equals(candidateStream))
                || MessageTypes.CANDIDATE_EVALUATED.equalsIgnoreCase(messageType);
    }

    @Override
    public void handle(MapRecord<String, String, String> record) {
        String streamKey = record.getStream();
        String consumerGroup = workerProperties.consumer().rankingGroup();
        String consumerName = workerProperties.consumer().consumerName();

        String rawMessageId = record.getValue().get("messageId");
        String rawPayload = record.getValue().get("payload");
        if (rawPayload == null) {
            rawPayload = record.getValue().toString();
        }

        CandidateEvaluatedPayload payload;
        String messageId = rawMessageId;
        String correlationId = null;

        try {
            if (rawPayload.contains("\"payload\"")) {
                MessageEnvelope<CandidateEvaluatedPayload> envelope = objectMapper.readValue(
                        rawPayload,
                        new TypeReference<MessageEnvelope<CandidateEvaluatedPayload>>() {}
                );
                payload = envelope.payload();
                messageId = envelope.messageId();
                correlationId = envelope.correlationId();
            } else {
                payload = objectMapper.readValue(rawPayload, CandidateEvaluatedPayload.class);
            }
        } catch (Exception ex) {
            log.error("Malformed payload on candidate evaluated stream '{}': {}", streamKey, ex.getMessage());
            messageReader.ack(streamKey, consumerGroup, record.getId());
            return;
        }

        if (messageId != null && idempotencyGuard.isAlreadyProcessed(consumerName, messageId)) {
            log.debug("Skipping already processed message '{}'", messageId);
            messageReader.ack(streamKey, consumerGroup, record.getId());
            return;
        }

        ExperimentId experimentId = new ExperimentId(payload.experimentId().value());
        JobId jobId = new JobId(payload.jobId().value());

        try {
            // 1. Reconcile leaderboard projection
            Optional<LeaderboardRevision> revision = leaderboardReconciliationUseCase.reconcileLeaderboard(
                    experimentId,
                    workerProperties.reconciliation().leaderboardBatchSize()
            );
            String revisionId = revision.map(r -> r.revisionId().value()).orElse(null);

            // 2. Record terminal progress on experiment aggregate
            experimentUseCase.recordTerminalProgress(jobId, TerminalWorkOutcome.SUCCEEDED, payload.overallScore());

            // 3. Publish progress event
            progressEventPublisher.publishProgress(
                    experimentId.value(),
                    jobId.value(),
                    1,
                    0,
                    1,
                    payload.overallScore(),
                    revisionId != null ? revisionId : "00000000000000000000000000",
                    "PROGRESS_UPDATED",
                    correlationId
            );

            // 4. Check experiment status for completion lifecycle notification
            ExperimentStatus status = experimentUseCase.getExperimentStatus(experimentId);
            if (status == ExperimentStatus.COMPLETED || status == ExperimentStatus.FAILED || status == ExperimentStatus.STOPPED) {
                lifecycleNotificationPublisher.publishLifecycleNotification(
                        "EXPERIMENT",
                        experimentId.value(),
                        experimentId.value(),
                        jobId.value(),
                        payload.candidateId().value(),
                        status.name(),
                        correlationId
                );
            }

            // 5. Mark processed & ACK
            if (messageId != null) {
                idempotencyGuard.markProcessed(consumerName, messageId, workerProperties.processedMessage().ttl());
            }
            messageReader.ack(streamKey, consumerGroup, record.getId());
            log.info("Successfully processed CandidateEvaluated event for job '{}'", jobId);

        } catch (Exception ex) {
            log.error("Error processing CandidateEvaluated event for job '{}': {}", jobId, ex.getMessage(), ex);
            messageReader.ack(streamKey, consumerGroup, record.getId());
        }
    }
}
