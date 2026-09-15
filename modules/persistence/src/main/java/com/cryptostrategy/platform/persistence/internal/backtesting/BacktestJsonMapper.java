package com.cryptostrategy.platform.persistence.internal.backtesting;
import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestDecisionEvidence;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.List;
import java.time.Instant;
import java.util.Objects;
public final class BacktestJsonMapper {
    private final ObjectMapper mapper = new ObjectMapper();
    public String write(BacktestAssumptions value) {
        try { return mapper.writeValueAsString(Objects.requireNonNull(value)); }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Cannot serialize assumptions", error); }
    }
    public BacktestAssumptions read(String json) {
        try { return mapper.readValue(Objects.requireNonNull(json), BacktestAssumptions.class); }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Cannot deserialize assumptions", error); }
    }
    public Map<String, Object> readMap(String value) {
        try { return mapper.readValue(Objects.requireNonNull(value), new TypeReference<>() {}); }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Cannot deserialize JSON map", error); }
    }
    public String writeDecisionEvidence(List<BacktestDecisionEvidence> values) {
        try {
            List<Map<String, Object>> rows = Objects.requireNonNull(values).stream()
                    .map(value -> Map.<String, Object>of(
                            "occurredAt", value.occurredAt().toString(),
                            "signal", value.signal().name(),
                            "reasonCode", value.reasonCode(),
                            "evidence", value.evidence()))
                    .toList();
            return mapper.writeValueAsString(rows);
        }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Cannot serialize decision evidence", error); }
    }
    public List<BacktestDecisionEvidence> readDecisionEvidence(String value) {
        try {
            List<Map<String, Object>> rows = mapper.readValue(Objects.requireNonNull(value), new TypeReference<>() {});
            return rows.stream().map(row -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> rawEvidence = (Map<String, Object>) row.getOrDefault("evidence", Map.of());
                Map<String, String> evidence = rawEvidence.entrySet().stream().collect(
                        java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                                entry -> String.valueOf(entry.getValue())));
                return new BacktestDecisionEvidence(
                        Instant.parse(String.valueOf(row.get("occurredAt"))),
                        StrategySignal.valueOf(String.valueOf(row.get("signal"))),
                        String.valueOf(row.get("reasonCode")), evidence);
            }).toList();
        } catch (JsonProcessingException | RuntimeException error) {
            throw new IllegalArgumentException("Cannot deserialize decision evidence", error);
        }
    }
}
