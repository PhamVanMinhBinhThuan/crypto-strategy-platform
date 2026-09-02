package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.contracts.api.CandidateEvaluatedPayload;
import com.cryptostrategy.platform.contracts.api.LifecycleNotificationPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.contracts.api.ProgressEventPayload;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

class WorkEventStreamConsumerTest {
    private static final String EXPERIMENT_ID = "01J00000000000000000000001";
    private static final String JOB_ID = "01J00000000000000000000002";
    private static final String CANDIDATE_ID = "01J00000000000000000000003";
    private static final String RESULT_ID = "01J00000000000000000000004";
    private static final String EVALUATION_ID = "01J00000000000000000000005";
    private static final String REVISION_ID = "01J00000000000000000000006";
    private static final Instant NOW = Instant.parse("2026-09-02T12:00:00Z");

    @Test
    void consumesNormalizedWorkEvents() throws Exception {
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
        WorkEventBridge bridge = new WorkEventBridge();
        GetLeaderboardUseCase leaderboards = mock(GetLeaderboardUseCase.class);
        LeaderboardSnapshot snapshot = mock(LeaderboardSnapshot.class);
        when(snapshot.revisionId()).thenReturn(new LeaderboardRevisionId(REVISION_ID));
        when(snapshot.revisionNumber()).thenReturn(7L);
        when(leaderboards.getLatest(new ExperimentId(EXPERIMENT_ID)))
                .thenReturn(Optional.of(snapshot));
        WorkEventStreamConsumer consumer = new WorkEventStreamConsumer(json, bridge, leaderboards);

        List<RealtimeMessageMapper.ServerEvent> experimentEvents = new ArrayList<>();
        List<RealtimeMessageMapper.ServerEvent> leaderboardEvents = new ArrayList<>();
        bridge.subscribe(
                WorkEventBridge.Kind.EXPERIMENT,
                EXPERIMENT_ID,
                "correlation",
                "experiment-subscription",
                experimentEvents::add);
        bridge.subscribe(
                WorkEventBridge.Kind.LEADERBOARD,
                EXPERIMENT_ID,
                "correlation",
                "leaderboard-subscription",
                leaderboardEvents::add);

        consumer.onMessage(streamRecord("progress.events.v1", json.writeValueAsString(
                envelope(MessageTypes.PROGRESS_EVENT, progressPayload()))));
        consumer.onMessage(streamRecord("candidate.evaluated.v1", json.writeValueAsString(
                envelope(MessageTypes.CANDIDATE_EVALUATED, candidatePayload()))));
        consumer.onMessage(streamRecord("lifecycle.events.v1", json.writeValueAsString(
                envelope(MessageTypes.LIFECYCLE_NOTIFICATION, lifecyclePayload()))));

        assertThat(experimentEvents)
                .extracting(RealtimeMessageMapper.ServerEvent::eventType)
                .containsExactly(
                        "EXPERIMENT_PROGRESS_UPDATED",
                        "BACKTEST_COMPLETED",
                        "EXPERIMENT_PROGRESS_UPDATED");
        assertThat(experimentEvents.getFirst().payload().get("bestScore"))
                .isEqualTo("0.8100");
        assertThat(experimentEvents.getFirst().payload().get("completedWork"))
                .isEqualTo(5);
        assertThat(experimentEvents.get(1).payload().get("backtestResultId"))
                .isEqualTo(RESULT_ID);
        assertThat(experimentEvents.get(1).payload().get("evaluationResultId"))
                .isEqualTo(EVALUATION_ID);
        assertThat(experimentEvents.getLast().payload().get("status"))
                .isEqualTo("COMPLETED");
        assertThat(experimentEvents.getLast().payload().get("jobId"))
                .isEqualTo(JOB_ID);
        assertThat(leaderboardEvents).hasSize(1);
        assertThat(leaderboardEvents.getFirst().payload().get("leaderboardId"))
                .isEqualTo(REVISION_ID);
        assertThat(leaderboardEvents.getFirst().payload().get("revision"))
                .isEqualTo(7L);
    }

    @Test
    void discardsMalformedOrUnsupportedMessages() {
        WorkEventStreamConsumer consumer = new WorkEventStreamConsumer(
                new ObjectMapper().registerModule(new JavaTimeModule()),
                new WorkEventBridge(),
                mock(GetLeaderboardUseCase.class));

        assertThatCode(() -> consumer.onMessage(streamRecord(
                        "progress.events.v1", "{\"messageVersion\":999}")))
                .doesNotThrowAnyException();
        assertThatCode(() -> consumer.onMessage(streamRecord(
                        "progress.events.v1", "not-json")))
                .doesNotThrowAnyException();
    }

    private static ProgressEventPayload progressPayload() {
        return new ProgressEventPayload(
                EXPERIMENT_ID,
                JOB_ID,
                5,
                1,
                10,
                new BigDecimal("0.8100"),
                REVISION_ID,
                "PROGRESS_UPDATED");
    }

    private static CandidateEvaluatedPayload candidatePayload() {
        return new CandidateEvaluatedPayload(
                EXPERIMENT_ID,
                JOB_ID,
                CANDIDATE_ID,
                RESULT_ID,
                EVALUATION_ID,
                new BigDecimal("0.8100"));
    }

    private static LifecycleNotificationPayload lifecyclePayload() {
        return new LifecycleNotificationPayload(
                "EXPERIMENT",
                EXPERIMENT_ID,
                EXPERIMENT_ID,
                JOB_ID,
                CANDIDATE_ID,
                "COMPLETED");
    }

    private static <T> MessageEnvelope<T> envelope(String type, T payload) {
        return new MessageEnvelope<>(
                Ulids.generate(),
                MessageTypes.CURRENT_VERSION,
                type,
                NOW,
                "F009-WORK-EVENT",
                payload);
    }

    private static MapRecord<String, String, String> streamRecord(
            String stream, String payload) {
        return MapRecord.create(stream, Map.of("payload", payload))
                .withId(RecordId.of("1-0"));
    }
}
