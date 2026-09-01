package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageContractSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void serializesAndDeserializesBacktestJobEnvelope() throws Exception {
        String ulid1 = "01J7K8M9N0P1Q2R3S4T5A6V7W8";
        String ulid2 = "01J7K8M9N0P1Q2R3S4T5A6V7W9";
        String ulid3 = "01J7K8M9N0P1Q2R3S4T5A6V7WA";
        String msgId = "01J7K8M9N0P1Q2R3S4T5A6V7WB";

        BacktestJobPayload payload = new BacktestJobPayload(ulid1, ulid2, ulid3);
        MessageEnvelope<BacktestJobPayload> envelope = new MessageEnvelope<>(
                msgId,
                1,
                MessageTypes.BACKTEST_JOB,
                Instant.parse("2026-09-01T12:00:00Z"),
                "corr-123",
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains(msgId, "BACKTEST_JOB", ulid1);

        MessageEnvelope<BacktestJobPayload> read = objectMapper.readValue(
                json,
                new TypeReference<MessageEnvelope<BacktestJobPayload>>() {}
        );

        assertThat(read.messageId()).isEqualTo(msgId);
        assertThat(read.messageVersion()).isEqualTo(1);
        assertThat(read.messageType()).isEqualTo("BACKTEST_JOB");
        assertThat(read.payload().experimentId().value()).isEqualTo(ulid1);
    }

    @Test
    void ignoresUnknownOptionalJsonProperties() throws Exception {
        String json = """
                {
                    "messageId": "01J7K8M9N0P1Q2R3S4T5A6V7WB",
                    "messageVersion": 1,
                    "messageType": "BACKTEST_JOB",
                    "occurredAt": "2026-09-01T12:00:00Z",
                    "correlationId": "corr-123",
                    "unknownEnvelopeField": "ignore-me",
                    "payload": {
                        "experimentId": "01J7K8M9N0P1Q2R3S4T5A6V7W8",
                        "jobId": "01J7K8M9N0P1Q2R3S4T5A6V7W9",
                        "candidateId": "01J7K8M9N0P1Q2R3S4T5A6V7WA",
                        "extraPayloadField": 12345
                    }
                }
                """;

        MessageEnvelope<BacktestJobPayload> read = objectMapper.readValue(
                json,
                new TypeReference<MessageEnvelope<BacktestJobPayload>>() {}
        );

        assertThat(read.messageId()).isEqualTo("01J7K8M9N0P1Q2R3S4T5A6V7WB");
        assertThat(read.payload().jobId().value()).isEqualTo("01J7K8M9N0P1Q2R3S4T5A6V7W9");
    }

    @Test
    void rejectsInvalidUlidOrVersion() {
        assertThatThrownBy(() -> new MessageEnvelope<>(
                "invalid-ulid",
                1,
                "TEST",
                Instant.now(),
                "corr-1",
                "payload"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MessageEnvelope<>(
                "01J7K8M9N0P1Q2R3S4T5A6V7WB",
                0,
                "TEST",
                Instant.now(),
                "corr-1",
                "payload"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serializesCandidateEvaluatedAndDeadLetter() throws Exception {
        String ulid = "01J7K8M9N0P1Q2R3S4T5A6V7WB";
        CandidateEvaluatedPayload eval = new CandidateEvaluatedPayload(
                ulid, ulid, ulid, ulid, ulid, new BigDecimal("1.234")
        );
        String evalJson = objectMapper.writeValueAsString(eval);
        assertThat(evalJson).contains("1.234");

        DeadLetterPayload dlq = new DeadLetterPayload(
                ulid, ulid, ulid, ulid, "WORKER_CRASHED", "EXECUTION_TIMEOUT", "diagnostic-ref", 3, Instant.now()
        );
        String dlqJson = objectMapper.writeValueAsString(dlq);
        assertThat(dlqJson).contains("WORKER_CRASHED", "EXECUTION_TIMEOUT");
    }
}
