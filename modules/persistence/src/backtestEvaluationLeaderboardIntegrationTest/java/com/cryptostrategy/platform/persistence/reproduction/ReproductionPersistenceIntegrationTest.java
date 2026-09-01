package com.cryptostrategy.platform.persistence.reproduction;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.backtesting.api.model.*;
import com.cryptostrategy.platform.backtesting.internal.BacktestReproductionVerifier;
import com.cryptostrategy.platform.persistence.api.BacktestingPersistenceFactory;
import com.cryptostrategy.platform.persistence.support.F006DatabaseFixture;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ReproductionPersistenceIntegrationTest {

    /**
     * T075: Proves the Experiment-owned linked Reproduction Run does not overwrite original
     * evidence. The read-back equals the original (MATCHED), a mutated reproduction produces
     * a structured mismatch, and the original row count remains exactly 1 after all operations.
     */
    @Test
    void linkedReproductionRunDoesNotOverwriteOriginalEvidenceAndMismatchIsDetected() {
        var source = F006DatabaseFixture.dataSource();
        F006DatabaseFixture.transaction(source).executeWithoutResult(status -> {
            status.setRollbackOnly(); // test is always rolled back — no permanent DB state

            var jdbc = new JdbcTemplate(source);
            F006DatabaseFixture.seed(jdbc);

            var factory = new BacktestingPersistenceFactory(source);
            var store  = factory.createResultStore();
            var reader = factory.createResultReader();

            // 1. Persist the original result
            var original = F006DatabaseFixture.result();
            var savedOriginal = store.save(original);
            assertEquals(original.resultId(), savedOriginal.resultId(),
                    "Saving original must return the canonical ID");

            // 2. Read back — must match exactly (proves MATCHED reproduction)
            var reloaded = reader.findById(original.resultId()).orElseThrow(
                    () -> new AssertionError("Original result must be readable after persist"));
            var matchReport = new BacktestReproductionVerifier().verify(original, reloaded);
            assertTrue(matchReport.matched(),
                    "Reloaded original must produce MATCHED: " + matchReport.differences());

            // 3. Create a reproduction result with a DIFFERENT ID but same fingerprint (idempotent retry)
            var reproId = new BacktestResultId("6000000000000000000000000R");
            var reproResult = new BacktestResult(
                    reproId,
                    original.experimentId(),
                    original.candidateId(),
                    original.jobId(),
                    original.successfulAttemptId(),
                    original.provenance(),
                    original.assumptions(),
                    original.initialCapital(),
                    original.finalCapital(),
                    original.totalFees(),
                    original.trades().stream()
                            .map(t -> new Trade(
                                    new TradeId("7000000000000000000000000R"),
                                    reproId,
                                    t.sequence(), t.side(), t.entryTime(), t.exitTime(),
                                    t.entryPrice(), t.exitPrice(), t.quantity(),
                                    t.entryFee(), t.exitFee(), t.totalFee(),
                                    t.realizedPnl(), t.postTradeCash(), t.exitReason()))
                            .toList(),
                    original.equityCurveSummary(),
                    original.fingerprint(), // same fingerprint → idempotent
                    original.completedAt()
            );
            // Idempotent save must return the ORIGINAL canonical ID, not the reproduction ID
            var idempotentReturn = store.save(reproResult);
            assertEquals(original.resultId(), idempotentReturn.resultId(),
                    "Idempotent retry with same fingerprint must return canonical persisted ID");

            // 4. Exactly ONE row must exist for this candidate — original not duplicated
            var count = jdbc.queryForObject(
                    "select count(*) from experiment.backtest_result where candidate_id = ?",
                    Integer.class, F006DatabaseFixture.CANDIDATE);
            assertEquals(1, count,
                    "Original evidence must not be duplicated by reproduction retry");

            // 5. A result with DIFFERENT fingerprint (mutation) is detected as MISMATCHED
            var mutatedResult = new BacktestResult(
                    new BacktestResultId("6000000000000000000000000M"),
                    original.experimentId(),
                    original.candidateId(),
                    original.jobId(),
                    original.successfulAttemptId(),
                    original.provenance(),
                    original.assumptions(),
                    original.initialCapital(),
                    Money.of(BigDecimal.valueOf(999)), // different final capital → different fingerprint
                    original.totalFees(),
                    List.of(), // no trades → different fingerprint
                    new EquityCurveSummary(0, original.initialCapital(), original.initialCapital(),
                            0, 0, "sha256:" + "F".repeat(64)),
                    "sha256:" + "A".repeat(64), // deliberate mismatch
                    Instant.now()
            );
            var mismatchReport = new BacktestReproductionVerifier().verify(original, mutatedResult);
            assertFalse(mismatchReport.matched(),
                    "Mutated reproduction fingerprint must be MISMATCHED");
            assertFalse(mismatchReport.differences().isEmpty(),
                    "MISMATCHED report must contain structured differences");

            // 6. Original row is still exactly as persisted — mismatch detection did not mutate it
            var afterMismatch = reader.findById(original.resultId()).orElseThrow();
            assertEquals(original.fingerprint(), afterMismatch.fingerprint(),
                    "Original fingerprint must be unchanged after mismatch detection");
        });
    }
}

