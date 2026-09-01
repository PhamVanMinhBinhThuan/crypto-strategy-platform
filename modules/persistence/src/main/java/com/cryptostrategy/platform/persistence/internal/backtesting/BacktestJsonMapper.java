package com.cryptostrategy.platform.persistence.internal.backtesting;
import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
