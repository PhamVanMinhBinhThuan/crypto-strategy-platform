package com.cryptostrategy.platform.persistence.internal.execution;

import com.cryptostrategy.platform.execution.api.ReproductionVerification;
import com.cryptostrategy.platform.execution.api.port.out.ReproductionVerificationStore;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.Objects;

public final class JdbcReproductionVerificationStore implements ReproductionVerificationStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    
    public JdbcReproductionVerificationStore(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.json = Objects.requireNonNull(json);
    }

    @Override
    public ReproductionVerification save(ReproductionVerification verification) {
        try {
            String differencesJson = json.writeValueAsString(verification.differences());
            jdbc.update("INSERT INTO experiment.reproduction_verification (" +
                    "reproduction_experiment_id, source_experiment_id, original_backtest_result_id, reproduced_backtest_result_id, " +
                    "original_evaluation_result_id, reproduced_evaluation_result_id, original_leaderboard_revision_id, " +
                    "reproduced_leaderboard_revision_id, outcome, differences, manifest_fingerprint, dataset_fingerprint, " +
                    "strategy_fingerprint, assumptions_fingerprint, metric_fingerprint, ranking_fingerprint, verified_at) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?,?,?,?)",
                    verification.reproductionExperimentId().value(), verification.sourceExperimentId().value(),
                    verification.original().backtest().resultId().value(), verification.reproduced().backtest().resultId().value(),
                    verification.original().evaluation().evaluationResultId().value(), verification.reproduced().evaluation().evaluationResultId().value(),
                    verification.original().leaderboard() != null ? verification.original().leaderboard().revisionId().value() : null,
                    verification.reproduced().leaderboard() != null ? verification.reproduced().leaderboard().revisionId().value() : null,
                    verification.outcome().name(), differencesJson,
                    verification.fingerprints().get("manifest_fingerprint"), verification.fingerprints().get("dataset_fingerprint"), verification.fingerprints().get("strategy_fingerprint"),
                    verification.fingerprints().get("assumptions_fingerprint"), verification.fingerprints().get("metric_fingerprint"), verification.fingerprints().get("ranking_fingerprint"),
                    Timestamp.from(verification.verifiedAt()));
            return verification;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save reproduction verification", e);
        }
    }
}
