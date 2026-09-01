package com.cryptostrategy.platform.persistence.internal.execution;

import com.cryptostrategy.platform.execution.api.ReproductionVerification;
import com.cryptostrategy.platform.execution.api.port.out.ReproductionVerificationStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcReproductionVerificationStore implements ReproductionVerificationStore {
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions; private final ObjectMapper json;
    public JdbcReproductionVerificationStore(JdbcTemplate jdbc,TransactionTemplate transactions,ObjectMapper json){this.jdbc=Objects.requireNonNull(jdbc);this.transactions=Objects.requireNonNull(transactions);this.json=Objects.requireNonNull(json);}
    @Override public ReproductionVerification save(ReproductionVerification v){return transactions.execute(status->{
        var o=v.original();var r=v.reproduced();var fp=v.fingerprints();
        jdbc.update("insert into experiment.reproduction_verification(reproduction_experiment_id,source_experiment_id,original_backtest_result_id,reproduced_backtest_result_id,original_evaluation_result_id,reproduced_evaluation_result_id,original_leaderboard_revision_id,reproduced_leaderboard_revision_id,outcome,differences,manifest_fingerprint,dataset_fingerprint,strategy_fingerprint,assumptions_fingerprint,metric_fingerprint,ranking_fingerprint,verified_at) values (?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?,?,?,?)",v.reproductionExperimentId().value(),v.sourceExperimentId().value(),o.backtest().resultId().value(),r.backtest().resultId().value(),o.evaluation().evaluationResultId().value(),r.evaluation().evaluationResultId().value(),o.leaderboard().revisionId().value(),r.leaderboard().revisionId().value(),v.outcome().name(),write(v.differences()),r.backtest().provenance().manifestFingerprint(),r.backtest().provenance().datasetFingerprint(),r.backtest().provenance().strategyFingerprint(),r.backtest().assumptions().contractVersion(),fp.get("evaluation"),fp.get("leaderboard"),Timestamp.from(v.verifiedAt()));return v;});}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalArgumentException("Cannot serialize reproduction differences",e);}}
}
