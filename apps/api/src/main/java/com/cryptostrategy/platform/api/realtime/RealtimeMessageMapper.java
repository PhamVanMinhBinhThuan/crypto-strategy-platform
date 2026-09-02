package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Strict version-one command parser and safe server-envelope encoder. */
@Component
public final class RealtimeMessageMapper {
    private static final Set<String> ENVELOPE_FIELDS = Set.of(
            "eventType", "eventVersion", "eventId", "occurredAt",
            "correlationId", "subscriptionId", "payload");
    private static final Set<String> COMMANDS = Set.of(
            "SUBSCRIBE_CANDLES", "UNSUBSCRIBE_CANDLES",
            "SUBSCRIBE_EXPERIMENT", "UNSUBSCRIBE_EXPERIMENT",
            "SUBSCRIBE_LEADERBOARD", "UNSUBSCRIBE_LEADERBOARD", "PING");
    private final ObjectMapper json;

    public RealtimeMessageMapper(ObjectMapper json) {
        this.json = json.copy();
    }

    public ClientCommand read(String text) {
        if (text == null || text.isBlank()) {
            throw invalid("Message must be a JSON object");
        }
        try {
            JsonNode raw = json.readTree(text);
            if (!(raw instanceof ObjectNode object)) {
                throw invalid("Message must be a JSON object");
            }
            Iterator<String> fields = object.fieldNames();
            while (fields.hasNext()) {
                if (!ENVELOPE_FIELDS.contains(fields.next())) {
                    throw invalid("Unknown envelope field");
                }
            }
            String eventType = requiredText(object, "eventType");
            if (!COMMANDS.contains(eventType)) {
                throw invalid("Unsupported realtime command");
            }
            int version = object.path("eventVersion").asInt(-1);
            if (version != 1) {
                throw new RealtimeProtocolException(
                        "VERSION_CONFLICT", "Only eventVersion 1 is supported", false);
            }
            String eventId = requiredText(object, "eventId");
            String correlationId = requiredText(object, "correlationId");
            String subscriptionId = bounded(requiredText(object, "subscriptionId"), 128);
            Instant occurredAt;
            try {
                occurredAt = Instant.parse(requiredText(object, "occurredAt"));
            } catch (DateTimeParseException exception) {
                throw invalid("occurredAt must be UTC");
            }
            JsonNode payload = object.get("payload");
            if (payload == null || !payload.isObject()) {
                throw invalid("payload must be an object");
            }
            return new ClientCommand(
                    eventType,
                    version,
                    bounded(eventId, 128),
                    occurredAt,
                    bounded(correlationId, 128),
                    subscriptionId,
                    payload.deepCopy());
        } catch (RealtimeProtocolException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw invalid("Message is not valid JSON");
        }
    }

    public String write(ServerEvent event) {
        ObjectNode envelope = json.createObjectNode();
        envelope.put("eventType", event.eventType());
        envelope.put("eventVersion", 1);
        envelope.put("eventId", Ulids.generate());
        envelope.put("occurredAt", event.occurredAt().toString());
        envelope.put("correlationId", event.correlationId());
        envelope.put("subscriptionId", event.subscriptionId());
        envelope.set("payload", json.valueToTree(event.payload()));
        try {
            return json.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode realtime event", exception);
        }
    }

    static ServerEvent event(
            String type,
            String correlationId,
            String subscriptionId,
            Map<String, ?> payload,
            boolean coalescible,
            String coalescingKey) {
        return new ServerEvent(
                type, Instant.now(), correlationId, subscriptionId,
                Map.copyOf(payload), coalescible, coalescingKey);
    }

    private static String requiredText(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw invalid(field + " is required");
        }
        return value.textValue();
    }

    private static String bounded(String value, int maximum) {
        if (value.length() > maximum) {
            throw invalid("Identifier is too long");
        }
        return value;
    }

    private static RealtimeProtocolException invalid(String message) {
        return new RealtimeProtocolException("REQUEST_VALIDATION_FAILED", message, false);
    }

    public record ClientCommand(
            String eventType,
            int eventVersion,
            String eventReference,
            Instant occurredAt,
            String correlationId,
            String subscriptionKey,
            JsonNode payload) {
        String subscriptionId() { return subscriptionKey; }
    }

    public record ServerEvent(
            String eventType,
            Instant occurredAt,
            String correlationId,
            String subscriptionKey,
            Map<String, ?> payload,
            boolean coalescible,
            String coalescingKey) {
        String subscriptionId() { return subscriptionKey; }
    }
}
