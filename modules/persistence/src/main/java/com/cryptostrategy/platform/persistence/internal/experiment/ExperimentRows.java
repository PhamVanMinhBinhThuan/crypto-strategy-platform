package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktest;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyComponentSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExperimentRows {

    private final ExperimentJsonMapper jsonMapper;

    public ExperimentRows(ExperimentJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Experiment mapExperiment(ResultSet rs, int rowNum) throws SQLException {
        String derivedFrom = rs.getString("derived_from_experiment_id");
        String reproduces = rs.getString("reproduces_experiment_id");
        return new Experiment(
                new ExperimentId(rs.getString("experiment_id")),
                (UUID) rs.getObject("owner_user_id"),
                rs.getString("name"),
                ExperimentStatus.valueOf(rs.getString("status")),
                derivedFrom != null ? new ExperimentId(derivedFrom) : null,
                reproduces != null ? new ExperimentId(reproduces) : null,
                toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("completed_at")),
                rs.getString("failure_code"),
                rs.getString("failure_message"),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    public ExperimentManifest mapManifest(ResultSet rs, int rowNum) throws SQLException {
        DatasetProvenanceSnapshot datasetProvenance =
                jsonMapper.readDatasetProvenance(rs.getString("dataset_provenance"));
        StrategyProvenanceSnapshot strategyProvenance =
                jsonMapper.readStrategyProvenance(rs.getString("strategy_provenance"));

        return new ExperimentManifest(
                new ExperimentId(rs.getString("experiment_id")),
                rs.getString("manifest_version"),
                datasetProvenance,
                strategyProvenance,
                jsonMapper.readMap(rs.getString("backtest_config")),
                jsonMapper.readSearchConfig(rs.getString("search_config")),
                jsonMapper.readMap(rs.getString("evaluation_config")),
                rs.getString("sentiment_config") != null ? jsonMapper.readMap(rs.getString("sentiment_config")) : null,
                rs.getString("software_version"),
                rs.getString("git_commit"),
                rs.getString("fingerprint"),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    public CandidateDefinition mapCandidate(ResultSet rs, int rowNum) throws SQLException {
        String generatorStateJson = rs.getString("generator_state");
        return new CandidateDefinition(
                new CandidateId(rs.getString("candidate_id")),
                new ExperimentId(rs.getString("experiment_id")),
                rs.getInt("generation_index"),
                jsonMapper.readCandidateDefinition(rs.getString("definition")),
                generatorStateJson != null ? jsonMapper.readMap(generatorStateJson) : null,
                rs.getString("fingerprint"),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    public Job mapJob(ResultSet rs, int rowNum) throws SQLException {
        String candidateIdStr = rs.getString("candidate_id");
        BigDecimal bestScore = rs.getBigDecimal("best_score");
        return new Job(
                new JobId(rs.getString("job_id")),
                new ExperimentId(rs.getString("experiment_id")),
                candidateIdStr != null ? new CandidateId(candidateIdStr) : null,
                JobType.valueOf(rs.getString("job_type")),
                JobStatus.valueOf(rs.getString("status")),
                rs.getString("correlation_id"),
                rs.getInt("total_work"),
                rs.getInt("completed_work"),
                rs.getInt("failed_work"),
                bestScore,
                toInstant(rs.getTimestamp("queued_at")),
                toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("finished_at")),
                toInstant(rs.getTimestamp("next_retry_at")),
                rs.getString("failure_code"),
                rs.getString("failure_message"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    public StandaloneBacktest mapStandaloneBacktest(ResultSet rs, int rowNum)
            throws SQLException {
        return new StandaloneBacktest(
                new BacktestId(rs.getString("backtest_id")),
                new ExperimentId(rs.getString("experiment_id")),
                new CandidateId(rs.getString("candidate_id")),
                new JobId(rs.getString("job_id")),
                toInstant(rs.getTimestamp("created_at")));
    }

    public ExecutionAttempt mapAttempt(ResultSet rs, int rowNum) throws SQLException {
        return new ExecutionAttempt(
                new AttemptId(rs.getString("attempt_id")),
                new JobId(rs.getString("job_id")),
                new CandidateId(rs.getString("candidate_id")),
                rs.getInt("attempt_no"),
                AttemptStatus.valueOf(rs.getString("status")),
                rs.getString("worker_id"),
                toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("finished_at")),
                toInstant(rs.getTimestamp("next_retry_at")),
                rs.getString("failure_code"),
                rs.getString("failure_message"),
                rs.getBoolean("retryable"),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
