package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.contracts.api.CandidateEvaluatedPayload;
import com.cryptostrategy.platform.contracts.api.LifecycleNotificationPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.contracts.api.ProgressEventPayload;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

/** Parses normalized F-007 envelopes and fans safe public hints into local subscriptions. */
@Component
public final class WorkEventStreamConsumer
        implements StreamListener<String, MapRecord<String, String, String>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkEventStreamConsumer.class);
    private static final String EMPTY_REVISION = "00000000000000000000000000";

    private final ObjectMapper json;
    private final WorkEventBridge bridge;
    private final GetLeaderboardUseCase leaderboards;

    public WorkEventStreamConsumer(
            ObjectMapper json,
            WorkEventBridge bridge,
            GetLeaderboardUseCase leaderboards) {
        this.json = Objects.requireNonNull(json, "json");
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.leaderboards = Objects.requireNonNull(leaderboards, "leaderboards");
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        String raw = record.getValue().get("payload");
        if (raw == null || raw.isBlank()) {
            warn(record, "missing payload");
            return;
        }
        try {
            JsonNode root = json.readTree(raw);
            int version = root.path("messageVersion").asInt(-1);
            String type = root.path("messageType").asText("");
            if (version != MessageTypes.CURRENT_VERSION) {
                warn(record, "unsupported message version");
                return;
            }
            switch (type) {
                case MessageTypes.PROGRESS_EVENT -> progress(read(
                        raw, new TypeReference<MessageEnvelope<ProgressEventPayload>>() {}));
                case MessageTypes.LIFECYCLE_NOTIFICATION -> lifecycle(read(
                        raw, new TypeReference<MessageEnvelope<LifecycleNotificationPayload>>() {}));
                case MessageTypes.CANDIDATE_EVALUATED -> completion(read(
                        raw, new TypeReference<MessageEnvelope<CandidateEvaluatedPayload>>() {}));
                default -> warn(record, "unsupported message type");
            }
        } catch (RuntimeException | JsonProcessingException exception) {
            warn(record, "invalid normalized envelope");
        }
    }

    void onConsumerError(Throwable exception) {
        LOGGER.warn("Realtime work notification stream is temporarily unavailable");
    }

    private void progress(MessageEnvelope<ProgressEventPayload> envelope) {
        var payload = envelope.payload();
        bridge.publishProgress(
                payload.experimentId().value(),
                payload.jobId().value(),
                "RUNNING",
                payload.completedWork(),
                payload.failedWork(),
                payload.totalWork(),
                payload.bestScore() == null ? null : payload.bestScore().toPlainString(),
                envelope.correlationId(),
                envelope.occurredAt());

        String revisionId = payload.leaderboardRevisionId() == null
                ? null
                : payload.leaderboardRevisionId().value();
        if (revisionId == null || EMPTY_REVISION.equals(revisionId)) {
            return;
        }
        leaderboards.getLatest(new ExperimentId(payload.experimentId().value()))
                .filter(snapshot -> revisionId.equals(snapshot.revisionId().value()))
                .ifPresent(snapshot -> bridge.publishLeaderboard(
                        payload.experimentId().value(),
                        revisionId,
                        snapshot.revisionNumber(),
                        envelope.correlationId(),
                        envelope.occurredAt()));
    }

    private void lifecycle(MessageEnvelope<LifecycleNotificationPayload> envelope) {
        var payload = envelope.payload();
        bridge.publishLifecycle(
                payload.experimentId().value(),
                payload.jobId() == null ? null : payload.jobId().value(),
                payload.lifecycleEventType(),
                envelope.correlationId(),
                envelope.occurredAt());
    }

    private void completion(MessageEnvelope<CandidateEvaluatedPayload> envelope) {
        var payload = envelope.payload();
        bridge.publishCompletion(
                payload.experimentId().value(),
                payload.candidateId().value(),
                payload.backtestResultId().value(),
                payload.evaluationResultId().value(),
                envelope.correlationId(),
                envelope.occurredAt());
    }

    private <T> MessageEnvelope<T> read(
            String raw, TypeReference<MessageEnvelope<T>> type) throws JsonProcessingException {
        return json.readValue(raw, type);
    }

    private static void warn(MapRecord<String, String, String> record, String reason) {
        LOGGER.warn(
                "Ignored realtime notification record {} from stream {}: {}",
                record.getId(),
                record.getStream(),
                reason);
    }
}
