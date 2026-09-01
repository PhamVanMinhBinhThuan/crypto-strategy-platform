package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

public final class OutboxEvents {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OutboxEvents() {}

    public static OutboxEvent experimentQueued(Experiment experiment, ExperimentManifest manifest, Instant occurredAt) {
        String eventId = Ulids.generate();
        String messageId = Ulids.generate();
        Map<String, Object> payload = Map.of(
                "experimentId", experiment.experimentId().value(),
                "ownerUserId", experiment.ownerUserId().toString(),
                "fingerprint", manifest.fingerprint() != null ? manifest.fingerprint() : "",
                "status", "QUEUED",
                "queuedAt", occurredAt.toString()
        );
        return new OutboxEvent(
                eventId,
                messageId,
                "EXPERIMENT",
                experiment.experimentId().value(),
                "ExperimentQueued",
                "1.0.0",
                toJson(payload),
                Map.of("eventType", "ExperimentQueued"),
                occurredAt
        );
    }

    public static OutboxEvent experimentStopRequested(Experiment experiment, Instant occurredAt) {
        String eventId = Ulids.generate();
        String messageId = Ulids.generate();
        Map<String, Object> payload = Map.of(
                "experimentId", experiment.experimentId().value(),
                "ownerUserId", experiment.ownerUserId().toString(),
                "status", "STOP_REQUESTED",
                "requestedAt", occurredAt.toString()
        );
        return new OutboxEvent(
                eventId,
                messageId,
                "EXPERIMENT",
                experiment.experimentId().value(),
                "ExperimentStopRequested",
                "1.0.0",
                toJson(payload),
                Map.of("eventType", "ExperimentStopRequested"),
                occurredAt
        );
    }

    public static OutboxEvent jobQueued(Job job, Instant occurredAt) {
        String eventId = Ulids.generate();
        String messageId = Ulids.generate();
        Map<String, Object> payload = Map.of(
                "jobId", job.jobId().value(),
                "experimentId", job.experimentId().value(),
                "candidateId", job.candidateId() != null ? job.candidateId().value() : "",
                "jobType", job.jobType().name(),
                "status", "QUEUED",
                "correlationId", job.correlationId(),
                "queuedAt", occurredAt.toString()
        );
        return new OutboxEvent(
                eventId,
                messageId,
                "JOB",
                job.jobId().value(),
                "JobQueued",
                "1.0.0",
                toJson(payload),
                Map.of("eventType", "JobQueued"),
                occurredAt
        );
    }

    public static OutboxEvent jobCancelRequested(Job job, Instant occurredAt) {
        String eventId = Ulids.generate();
        String messageId = Ulids.generate();
        Map<String, Object> payload = Map.of(
                "jobId", job.jobId().value(),
                "experimentId", job.experimentId().value(),
                "status", "CANCEL_REQUESTED",
                "requestedAt", occurredAt.toString()
        );
        return new OutboxEvent(
                eventId,
                messageId,
                "JOB",
                job.jobId().value(),
                "JobCancelRequested",
                "1.0.0",
                toJson(payload),
                Map.of("eventType", "JobCancelRequested"),
                occurredAt
        );
    }

    public static OutboxEvent jobCancelled(Job job, Instant occurredAt) {
        String eventId = Ulids.generate();
        String messageId = Ulids.generate();
        Map<String, Object> payload = Map.of(
                "jobId", job.jobId().value(),
                "experimentId", job.experimentId().value(),
                "status", "CANCELLED",
                "cancelledAt", occurredAt.toString()
        );
        return new OutboxEvent(
                eventId,
                messageId,
                "JOB",
                job.jobId().value(),
                "JobCancelled",
                "1.0.0",
                toJson(payload),
                Map.of("eventType", "JobCancelled"),
                occurredAt
        );
    }

    private static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Outbox payload to JSON", e);
        }
    }
}
