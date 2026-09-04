package com.cryptostrategy.platform.worker.consumer;

import com.cryptostrategy.platform.backtesting.api.PreparedBacktestOutcome;
import com.cryptostrategy.platform.backtesting.api.model.BacktestRunCommand;
import com.cryptostrategy.platform.backtesting.api.port.in.PrepareBacktestUseCase;
import com.cryptostrategy.platform.contracts.api.BacktestJobPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.execution.api.BacktestCompletionOutcome;
import com.cryptostrategy.platform.execution.api.port.in.CompleteBacktestAttemptUseCase;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.CandidateEvaluatedPublisher;
import com.cryptostrategy.platform.worker.infra.redis.DeadLetterPublisher;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class BacktestJobHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(BacktestJobHandler.class);

    private final DualLayerIdempotencyGuard idempotencyGuard;
    private final TrustedWorkerExperimentUseCase experimentUseCase;
    private final PrepareBacktestUseCase prepareBacktestUseCase;
    private final CompleteBacktestAttemptUseCase completeBacktestAttemptUseCase;
    private final CandidateEvaluatedPublisher candidateEvaluatedPublisher;
    private final DeadLetterPublisher deadLetterPublisher;
    private final RedisStreamMessageReader messageReader;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;

    public BacktestJobHandler(
            DualLayerIdempotencyGuard idempotencyGuard,
            TrustedWorkerExperimentUseCase experimentUseCase,
            PrepareBacktestUseCase prepareBacktestUseCase,
            CompleteBacktestAttemptUseCase completeBacktestAttemptUseCase,
            CandidateEvaluatedPublisher candidateEvaluatedPublisher,
            DeadLetterPublisher deadLetterPublisher,
            RedisStreamMessageReader messageReader,
            WorkerProperties workerProperties,
            ObjectMapper objectMapper
    ) {
        this.idempotencyGuard = Objects.requireNonNull(idempotencyGuard, "idempotencyGuard cannot be null");
        this.experimentUseCase = Objects.requireNonNull(experimentUseCase, "experimentUseCase cannot be null");
        this.prepareBacktestUseCase = Objects.requireNonNull(prepareBacktestUseCase, "prepareBacktestUseCase cannot be null");
        this.completeBacktestAttemptUseCase = Objects.requireNonNull(completeBacktestAttemptUseCase, "completeBacktestAttemptUseCase cannot be null");
        this.candidateEvaluatedPublisher = Objects.requireNonNull(candidateEvaluatedPublisher, "candidateEvaluatedPublisher cannot be null");
        this.deadLetterPublisher = Objects.requireNonNull(deadLetterPublisher, "deadLetterPublisher cannot be null");
        this.messageReader = Objects.requireNonNull(messageReader, "messageReader cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    public boolean canHandle(String streamKey, String messageType) {
        String backtestStream = workerProperties.streams().getBacktestJobsStream();
        return (streamKey != null && streamKey.equals(backtestStream))
                || MessageTypes.BACKTEST_JOB.equalsIgnoreCase(messageType)
                || "EXPERIMENT_QUEUED".equalsIgnoreCase(messageType);
    }

    @Override
    public void handle(MapRecord<String, String, String> record) {
        String streamKey = record.getStream();
        String consumerGroup = workerProperties.consumer().backtestGroup();
        String consumerName = workerProperties.consumer().consumerName();
        // The group is the stable logical consumer identity. Instance names change
        // after failover and therefore cannot be used as the durable dedup key.
        String idempotencyConsumer = consumerGroup;

        String rawMessageId = record.getValue().get("messageId");
        String rawPayload = record.getValue().get("payload");
        if (rawPayload == null) {
            rawPayload = record.getValue().toString();
        }

        BacktestJobPayload payload;
        String messageId = rawMessageId;
        String correlationId = null;

        try {
            if (rawPayload.contains("\"payload\"")) {
                MessageEnvelope<BacktestJobPayload> envelope = objectMapper.readValue(
                        rawPayload,
                        new TypeReference<MessageEnvelope<BacktestJobPayload>>() {}
                );
                payload = envelope.payload();
                messageId = envelope.messageId();
                correlationId = envelope.correlationId();
            } else {
                payload = objectMapper.readValue(rawPayload, BacktestJobPayload.class);
            }
        } catch (Exception ex) {
            log.error("Poison pill detected on stream '{}', publishing to dead letter: {}", streamKey, ex.getMessage());
            deadLetterPublisher.publishDeadLetter(
                    "00000000000000000000000000",
                    "00000000000000000000000000",
                    null,
                    rawMessageId != null ? rawMessageId : "00000000000000000000000000",
                    "PERMANENT_LOGIC_ERROR",
                    "MALFORMED_PAYLOAD",
                    ex.getMessage(),
                    1
            );
            messageReader.ack(streamKey, consumerGroup, record.getId());
            return;
        }

        if (messageId != null && idempotencyGuard.isAlreadyProcessed(idempotencyConsumer, messageId)) {
            log.debug("Skipping already processed message '{}'", messageId);
            messageReader.ack(streamKey, consumerGroup, record.getId());
            return;
        }

        JobId jobId = new JobId(payload.jobId().value());
        ExperimentId experimentId = new ExperimentId(payload.experimentId().value());
        CandidateId candidateId = new CandidateId(payload.candidateId().value());

        ExecutionAttempt attempt = null;
        try {
            attempt = experimentUseCase.startNextAttempt(jobId, new com.cryptostrategy.platform.experiment.api.job.WorkerId(consumerName));
            UUID ownerUserId = experimentUseCase.getFrozenExecution(jobId)
                    .experiment()
                    .ownerUserId();

            BacktestRunCommand command = new BacktestRunCommand(
                    ownerUserId,
                    experimentId,
                    candidateId,
                    jobId,
                    attempt.attemptId(),
                    1000
            );

            // Step 1: Pure compute outside DB transaction
            PreparedBacktestOutcome prepared = prepareBacktestUseCase.prepare(command);

            // Step 2: Atomic commit inside single short DB transaction
            BacktestCompletionOutcome outcome = completeBacktestAttemptUseCase.completeAttempt(
                    jobId,
                    attempt.attemptId(),
                    prepared
            );

            // Step 3: Publish evaluated event to stream
            candidateEvaluatedPublisher.publishCandidateEvaluated(
                    outcome.experimentId().value(),
                    outcome.jobId().value(),
                    outcome.candidateId().value(),
                    outcome.backtestResultId().value(),
                    outcome.evaluationResultId().value(),
                    outcome.overallScore(),
                    correlationId
            );

            // Step 4: Dual-layer idempotency mark & ACK
            if (messageId != null) {
                idempotencyGuard.markProcessed(idempotencyConsumer, messageId, workerProperties.processedMessage().ttl());
            }
            messageReader.ack(streamKey, consumerGroup, record.getId());
            log.info("Successfully completed backtest job '{}' for candidate '{}'", jobId, candidateId);

        } catch (Exception ex) {
            log.error("Execution failed for job '{}': {}", jobId, ex.getMessage(), ex);
            if (attempt != null) {
                FailureClassification classification = FailureClassification.PERMANENT_LOGIC_ERROR;
                experimentUseCase.finalizeFailure(
                        jobId,
                        attempt.attemptId(),
                        "EXECUTION_FAILED",
                        ex.getMessage(),
                        classification,
                        null
                );
                deadLetterPublisher.publishDeadLetter(
                        experimentId.value(),
                        jobId.value(),
                        candidateId.value(),
                        messageId != null ? messageId : "00000000000000000000000000",
                        classification.name(),
                        "EXECUTION_FAILED",
                        ex.getMessage(),
                        attempt.attemptNo()
                );
            }
            messageReader.ack(streamKey, consumerGroup, record.getId());
        }
    }
}
