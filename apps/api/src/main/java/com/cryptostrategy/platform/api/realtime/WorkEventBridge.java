package com.cryptostrategy.platform.api.realtime;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Fan-out bridge for normalized F-007 progress/lifecycle notifications. */
@Component
public final class WorkEventBridge {
    private final Map<Key, Consumer<RealtimeMessageMapper.ServerEvent>> subscribers = new HashMap<>();

    public synchronized AutoCloseable subscribe(
            Kind kind,
            String experimentId,
            String correlationId,
            String subscriptionId,
            Consumer<RealtimeMessageMapper.ServerEvent> delivery) {
        Key key = new Key(kind, experimentId, subscriptionId);
        subscribers.put(key, delivery);
        return () -> remove(key, delivery);
    }

    public synchronized void publishProgress(
            String experimentId,
            String jobId,
            String status,
            int completedWork,
            int failedWork,
            int totalWork,
            String bestScore,
            String correlationId,
            Instant occurredAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("experimentId", experimentId);
        payload.put("jobId", jobId);
        payload.put("status", status);
        payload.put("completedWork", completedWork);
        payload.put("failedWork", failedWork);
        payload.put("totalWork", totalWork);
        if (bestScore != null) {
            payload.put("bestScore", bestScore);
        }
        emit(Kind.EXPERIMENT, experimentId, "EXPERIMENT_PROGRESS_UPDATED", payload,
                correlationId, occurredAt, true, "progress|" + experimentId);
    }

    public synchronized void publishLifecycle(
            String experimentId,
            String jobId,
            String status,
            String correlationId,
            Instant occurredAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("experimentId", experimentId);
        if (jobId != null) {
            payload.put("jobId", jobId);
        }
        payload.put("status", status);
        payload.put("snapshotUrl", "/api/v1/experiments/" + experimentId);
        emit(Kind.EXPERIMENT, experimentId, "EXPERIMENT_PROGRESS_UPDATED", payload,
                correlationId, occurredAt, false, null);
    }

    public synchronized void publishCompletion(
            String experimentId,
            String candidateId,
            String backtestResultId,
            String evaluationResultId,
            String correlationId,
            Instant occurredAt) {
        emit(Kind.EXPERIMENT, experimentId, "BACKTEST_COMPLETED", Map.of(
                        "experimentId", experimentId,
                        "candidateId", candidateId,
                        "backtestResultId", backtestResultId,
                        "evaluationResultId", evaluationResultId),
                correlationId, occurredAt, false, null);
    }

    public synchronized void publishLeaderboard(
            String experimentId,
            String revisionId,
            long revision,
            String correlationId,
            Instant occurredAt) {
        emit(Kind.LEADERBOARD, experimentId, "LEADERBOARD_UPDATED", Map.of(
                        "experimentId", experimentId,
                        "leaderboardId", revisionId,
                        "revision", revision,
                        "snapshotUrl", "/api/v1/experiments/" + experimentId + "/leaderboard"),
                correlationId, occurredAt, true, "leaderboard|" + experimentId);
    }

    private void emit(
            Kind kind,
            String experimentId,
            String type,
            Map<String, Object> payload,
            String correlationId,
            Instant occurredAt,
            boolean coalescible,
            String coalescingKey) {
        subscribers.forEach((key, delivery) -> {
            if (key.kind == kind && key.experimentKey.equals(experimentId)) {
                delivery.accept(new RealtimeMessageMapper.ServerEvent(
                        type,
                        occurredAt,
                        correlationId,
                        key.subscriptionKey,
                        Map.copyOf(payload),
                        coalescible,
                        coalescingKey));
            }
        });
    }

    private synchronized void remove(
            Key key, Consumer<RealtimeMessageMapper.ServerEvent> expected) {
        subscribers.remove(key, expected);
    }

    public enum Kind { EXPERIMENT, LEADERBOARD }

    private record Key(Kind kind, String experimentKey, String subscriptionKey) {
        private Key {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(experimentKey, "experimentId");
            Objects.requireNonNull(subscriptionKey, "subscriptionId");
        }
    }
}
