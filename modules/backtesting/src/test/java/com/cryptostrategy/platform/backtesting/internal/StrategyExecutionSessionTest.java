package com.cryptostrategy.platform.backtesting.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservation;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservationId;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategySupplementalInput;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class StrategyExecutionSessionTest {
    @Test
    void injectsTheSameFrozenSupplementalInputIntoEachRollingContext() {
        var fixture = new BacktestTestFixture();
        var observedSnapshot = new AtomicReference<String>();
        Strategy strategy = context -> {
            observedSnapshot.set(context.requireSupplementalInput("sentiment-polarity").snapshotId().value());
            return new StrategyDecision(StrategySignal.HOLD, context.evaluationTime(), fixture.reference,
                    "HOLD", "test", Map.of());
        };
        var input = new StrategySupplementalInput("sentiment-polarity", "sentiment-snapshot-v1",
                new StrategyInputSnapshotId("01J00000000000000000000050"), "sha256:fixed", Map.of("modelVersion", "v1"),
                List.of(new StrategyObservation(new StrategyObservationId("01J00000000000000000000051"), "result-1",
                        fixture.members.getFirst().candle().candle().closeTime(), BigDecimal.ONE,
                        BigDecimal.ONE, Map.of())));
        var session = new StrategyExecutionSession(fixture.dataset, strategy, 1,
                Map.of(input.inputType(), input));

        session.evaluate(fixture.members.getFirst().candle().candle());

        assertEquals("01J00000000000000000000050", observedSnapshot.get());
    }
}
