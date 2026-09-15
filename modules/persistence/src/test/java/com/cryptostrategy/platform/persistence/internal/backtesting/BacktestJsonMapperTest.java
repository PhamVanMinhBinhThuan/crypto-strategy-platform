package com.cryptostrategy.platform.persistence.internal.backtesting;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestDecisionEvidence;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BacktestJsonMapperTest {
    @Test void assumptionsRoundTripWithoutHiddenOrLostValues() {
        BacktestAssumptions original = BacktestAssumptions.mvp(
                new BigDecimal("1000.00"), new BigDecimal("0.001"), new BigDecimal("0.0005"));
        BacktestJsonMapper mapper = new BacktestJsonMapper();
        assertEquals(original, mapper.read(mapper.write(original)));
    }

    @Test void malformedJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BacktestJsonMapper().read("{}"));
    }

    @Test void sentimentDecisionEvidenceRoundTripsInAuthoritativeOrder() {
        var first = new BacktestDecisionEvidence(Instant.parse("2026-09-09T01:00:00Z"),
                StrategySignal.HOLD, "SENTIMENT_BETWEEN_THRESHOLDS",
                Map.of("sentimentSnapshotId", "01J00000000000000000000010",
                        "sentimentEvidenceFingerprint", "sha256:" + "a".repeat(64)));
        var second = new BacktestDecisionEvidence(Instant.parse("2026-09-09T02:00:00Z"),
                StrategySignal.BUY, "SENTIMENT_BUY_THRESHOLD",
                Map.of("sentimentSnapshotId", "01J00000000000000000000010",
                        "sentimentEvidenceFingerprint", "sha256:" + "b".repeat(64)));
        BacktestJsonMapper mapper = new BacktestJsonMapper();

        assertEquals(List.of(first, second),
                mapper.readDecisionEvidence(mapper.writeDecisionEvidence(List.of(first, second))));
    }
}
