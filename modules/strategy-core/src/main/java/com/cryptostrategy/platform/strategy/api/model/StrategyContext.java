package com.cryptostrategy.platform.strategy.api.model;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record StrategyContext(TradingPair tradingPair, Timeframe timeframe, List<Candle> candles,
        Instant evaluationTime, Map<String, StrategySupplementalInput> supplementalInputs) {
    public StrategyContext {
        Objects.requireNonNull(tradingPair); Objects.requireNonNull(timeframe); Objects.requireNonNull(candles); Objects.requireNonNull(evaluationTime);
        candles = List.copyOf(candles);
        supplementalInputs = Collections.unmodifiableMap(new TreeMap<>(
                Objects.requireNonNull(supplementalInputs, "supplementalInputs")));
        supplementalInputs.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null || !key.equals(value.inputType())) {
                throw new IllegalArgumentException("Supplemental input key must match input type");
            }
        });
        Instant previous = null;
        for (Candle candle : candles) {
            if (!candle.closed()) throw new IllegalArgumentException("Strategy requires closed Candles");
            if (!candle.key().tradingPair().tradingPairId().equals(tradingPair.tradingPairId()) || candle.key().timeframe() != timeframe) throw new IllegalArgumentException("Mixed Candle context");
            if (previous != null && !candle.key().openTime().isAfter(previous)) throw new IllegalArgumentException("Candles must be strictly ordered");
            previous = candle.key().openTime();
        }
        if (!candles.isEmpty() && !evaluationTime.equals(candles.getLast().closeTime())) throw new IllegalArgumentException("Evaluation time must match last Candle close");
    }

    public StrategyContext(TradingPair tradingPair, Timeframe timeframe, List<Candle> candles,
            Instant evaluationTime) {
        this(tradingPair, timeframe, candles, evaluationTime, Map.of());
    }

    public StrategySupplementalInput requireSupplementalInput(String inputType) {
        StrategySupplementalInput input = supplementalInputs.get(inputType);
        if (input == null) throw new IllegalArgumentException("Missing supplemental input: " + inputType);
        return input;
    }
}
