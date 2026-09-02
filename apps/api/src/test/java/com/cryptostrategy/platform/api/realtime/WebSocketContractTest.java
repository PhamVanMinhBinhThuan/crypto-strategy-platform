package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebSocketContractTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final RealtimeMessageMapper mapper = new RealtimeMessageMapper(json);

    @Test
    void parsesStrictVersionOneEnvelope() {
        var command = mapper.read(command("SUBSCRIBE_CANDLES", """
                {"pair":"BTC/USDT","timeframe":"5m"}
                """));

        assertThat(command.eventType()).isEqualTo("SUBSCRIBE_CANDLES");
        assertThat(command.eventVersion()).isEqualTo(1);
        assertThat(command.subscriptionId()).isEqualTo("chart-1");
    }

    @Test
    void rejectsUnknownFieldsAndUnsupportedVersions() {
        assertThatThrownBy(() -> mapper.read(command("PING", "{}")
                        .replace("\"payload\"", "\"unexpected\":true,\"payload\"")))
                .isInstanceOf(RealtimeProtocolException.class)
                .extracting(exception -> ((RealtimeProtocolException) exception).code())
                .isEqualTo("REQUEST_VALIDATION_FAILED");

        assertThatThrownBy(() -> mapper.read(command("PING", "{}")
                        .replace("\"eventVersion\":1", "\"eventVersion\":2")))
                .isInstanceOf(RealtimeProtocolException.class)
                .extracting(exception -> ((RealtimeProtocolException) exception).code())
                .isEqualTo("VERSION_CONFLICT");
    }

    @Test
    void writesUtcVersionedEnvelopeWithoutChangingDecimalText() throws Exception {
        String encoded = mapper.write(new RealtimeMessageMapper.ServerEvent(
                "CANDLE_UPDATED",
                Instant.parse("2026-09-02T00:00:00Z"),
                "corr-1",
                "chart-1",
                Map.of("close", "59275.800", "closed", true),
                false,
                null));

        var root = json.readTree(encoded);
        assertThat(root.path("eventVersion").asInt()).isEqualTo(1);
        assertThat(root.path("occurredAt").asText()).endsWith("Z");
        assertThat(root.path("payload").path("close").asText()).isEqualTo("59275.800");
    }

    private static String command(String type, String payload) {
        return """
                {
                  "eventType":"%s",
                  "eventVersion":1,
                  "eventId":"client-event-1",
                  "occurredAt":"2026-09-02T00:00:00Z",
                  "correlationId":"corr-1",
                  "subscriptionId":"chart-1",
                  "payload":%s
                }
                """.formatted(type, payload);
    }
}
