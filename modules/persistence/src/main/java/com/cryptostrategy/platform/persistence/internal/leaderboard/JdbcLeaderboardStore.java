package com.cryptostrategy.platform.persistence.internal.leaderboard;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationEligibilityPolicy;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardEntry;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardCandidateEvidence;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardBacktestResultId;
import com.cryptostrategy.platform.leaderboard.api.model.CandidatePipelineEntry;
import com.cryptostrategy.platform.leaderboard.api.port.out.LeaderboardStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcLeaderboardStore implements LeaderboardStore {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private static final String EVIDENCE_SELECT = """
            select c.experiment_id,c.candidate_id,c.generation_index,c.definition::text,
                c.generator_state::text,c.fingerprint,br.backtest_result_id,j.status,
                er.evaluation_result_id,er.metric_version,er.ranking_version,er.total_return,
                er.win_rate,er.maximum_drawdown,er.number_of_trades,er.overall_score,
                er.leaderboard_eligible,er.evaluation_fingerprint,er.evaluated_at
            from experiment.candidate_definition c
            left join experiment.job j on j.candidate_id=c.candidate_id
                and j.experiment_id=c.experiment_id and j.job_type='BACKTEST'
            left join experiment.backtest_result br on br.job_id=j.job_id
            left join experiment.evaluation_result er on er.backtest_result_id=br.backtest_result_id
                and er.experiment_id=c.experiment_id
            """;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public JdbcLeaderboardStore(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.tx = Objects.requireNonNull(tx);
    }

    @Override
    public Optional<LeaderboardRevision> latest(ExperimentId experiment) {
        try {
            LeaderboardRevision revision = jdbc.queryForObject(
                    "select leaderboard_revision_id,revision_no,top_k,ranking_version,revision_fingerprint,created_at from experiment.leaderboard_revision where experiment_id=? order by revision_no desc limit 1",
                    (rs, n) -> {
                        LeaderboardRevisionId id = new LeaderboardRevisionId(rs.getString(1));
                        List<LeaderboardEntry> entries = jdbc.query(
                                "select rank,evaluation_result_id,score,maximum_drawdown,evaluation_fingerprint from experiment.leaderboard_entry where leaderboard_revision_id=? order by rank",
                                (er, en) -> new LeaderboardEntry(id, experiment, er.getInt(1), new EvaluationResultId(er.getString(2)), er.getBigDecimal(3), er.getBigDecimal(4), er.getString(5)),
                                id.value()
                        );
                        return new LeaderboardRevision(id, experiment, rs.getLong(2), rs.getInt(3), new RankingVersion(rs.getString(4)), entries, rs.getString(5), rs.getTimestamp(6).toInstant());
                    },
                    experiment.value()
            );
            return Optional.ofNullable(revision);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public LeaderboardRevision save(LeaderboardRevision r) {
        return tx.execute(s -> {
            jdbc.query("select pg_advisory_xact_lock(hashtextextended(?,0))", (org.springframework.jdbc.core.RowCallbackHandler) rs -> {}, r.experimentId().value());
            List<String> same = jdbc.query(
                    "select revision_fingerprint from experiment.leaderboard_revision where experiment_id=? and ranking_version=? and revision_fingerprint=?",
                    (rs, n) -> rs.getString(1),
                    r.experimentId().value(),
                    r.rankingVersion().value(),
                    r.fingerprint()
            );
            if (!same.isEmpty()) return latest(r.experimentId()).orElseThrow();
            Long expected = jdbc.queryForObject(
                    "select coalesce(max(revision_no),0)+1 from experiment.leaderboard_revision where experiment_id=?",
                    Long.class,
                    r.experimentId().value()
            );
            if (expected == null || expected != r.revisionNumber()) throw new IllegalStateException("Concurrent leaderboard revision");
            jdbc.update(
                    "insert into experiment.leaderboard_revision(leaderboard_revision_id,experiment_id,revision_no,top_k,ranking_version,revision_fingerprint,created_at) values (?,?,?,?,?,?,?)",
                    r.revisionId().value(),
                    r.experimentId().value(),
                    r.revisionNumber(),
                    r.topK(),
                    r.rankingVersion().value(),
                    r.fingerprint(),
                    Timestamp.from(r.createdAt())
            );
            for (var e : r.entries()) {
                Integer valid = jdbc.queryForObject(
                        "select count(*) from experiment.evaluation_result where evaluation_result_id=? and experiment_id=?",
                        Integer.class,
                        e.evaluationResultId().value(),
                        r.experimentId().value()
                );
                if (valid == null || valid != 1) throw new IllegalArgumentException("Cross-Experiment leaderboard entry");
                jdbc.update(
                        "insert into experiment.leaderboard_entry(leaderboard_revision_id,experiment_id,rank,evaluation_result_id,score,maximum_drawdown,evaluation_fingerprint) values (?,?,?,?,?,?,?)",
                        r.revisionId().value(),
                        r.experimentId().value(),
                        e.rank(),
                        e.evaluationResultId().value(),
                        e.score(),
                        e.maximumDrawdown(),
                        e.evaluationFingerprint()
                );
            }
            return r;
        });
    }

    @Override
    public List<EvaluationResult> listEvaluationsForExperiment(ExperimentId experimentId, int limit) {
        return jdbc.query(
                "select evaluation_result_id, experiment_id, backtest_result_id, metric_version, ranking_version, " +
                        "total_return, win_rate, maximum_drawdown, number_of_trades, overall_score, leaderboard_eligible, " +
                        "evaluation_fingerprint, evaluated_at " +
                        "from experiment.evaluation_result " +
                        "where experiment_id = ? and leaderboard_eligible = true " +
                        "order by overall_score desc, evaluated_at asc limit ?",
                (rs, n) -> new EvaluationResult(
                        new EvaluationResultId(rs.getString(1)),
                        new ExperimentId(rs.getString(2)),
                        new BacktestResultId(rs.getString(3)),
                        new MetricVersion(rs.getString(4)),
                        new RankingVersion(rs.getString(5)),
                        rs.getBigDecimal(6),
                        rs.getBigDecimal(7),
                        rs.getBigDecimal(8),
                        rs.getInt(9),
                        rs.getBigDecimal(10),
                        rs.getBoolean(11),
                        rs.getString(12),
                        rs.getTimestamp(13).toInstant()
                ),
                experimentId.value(),
                limit
        );
    }

    @Override
    public Optional<EvaluationResult> findEvaluation(EvaluationResultId evaluationResultId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "select evaluation_result_id, experiment_id, backtest_result_id, metric_version, ranking_version, " +
                            "total_return, win_rate, maximum_drawdown, number_of_trades, overall_score, leaderboard_eligible, " +
                            "evaluation_fingerprint, evaluated_at from experiment.evaluation_result where evaluation_result_id = ?",
                    (rs, n) -> new EvaluationResult(
                            new EvaluationResultId(rs.getString(1)),
                            new ExperimentId(rs.getString(2)),
                            new BacktestResultId(rs.getString(3)),
                            new MetricVersion(rs.getString(4)),
                            new RankingVersion(rs.getString(5)),
                            rs.getBigDecimal(6),
                            rs.getBigDecimal(7),
                            rs.getBigDecimal(8),
                            rs.getInt(9),
                            rs.getBigDecimal(10),
                            rs.getBoolean(11),
                            rs.getString(12),
                            rs.getTimestamp(13).toInstant()),
                    evaluationResultId.value()));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LeaderboardCandidateEvidence> findCandidateEvidence(
            ExperimentId experimentId, EvaluationResultId evaluationResultId) {
        return evidence(EVIDENCE_SELECT + """
                where c.experiment_id=? and er.evaluation_result_id=?
                order by er.evaluated_at desc limit 1
                """, experimentId.value(), evaluationResultId.value());
    }

    @Override
    public List<CandidatePipelineEntry> listCandidatePipeline(ExperimentId experimentId) {
        return queryCandidatePipeline(experimentId, null);
    }

    @Override
    public Optional<CandidatePipelineEntry> findCandidatePipelineEntry(
            ExperimentId experimentId, CandidateId candidateId) {
        return queryCandidatePipeline(experimentId, candidateId).stream().findFirst();
    }

    private List<CandidatePipelineEntry> queryCandidatePipeline(
            ExperimentId experimentId, CandidateId candidateId) {
        String candidateFilter = candidateId == null ? "" : " and c.candidate_id=?";
        String sql = """
                with latest_revision as (
                    select leaderboard_revision_id, ranking_version, created_at
                    from experiment.leaderboard_revision
                    where experiment_id=? order by revision_no desc limit 1
                )
                select c.candidate_id,c.generation_index,c.definition::text,c.fingerprint,c.created_at,
                    j.job_id,j.status job_status,
                    coalesce(sa.started_at,j.started_at) started_at,
                    case when br.backtest_result_id is not null
                        then coalesce(sa.finished_at,br.completed_at) else j.finished_at end finished_at,
                    j.failure_code,j.failure_message,
                    coalesce(j.next_retry_at,ea.next_retry_at) next_retry_at,
                    case when er.evaluation_result_id is not null
                        then coalesce(sa.attempt_no,ea.attempt_no)
                        else coalesce(ea.attempt_no,sa.attempt_no) end attempt_no,
                    ea.retryable attempt_retryable,
                    br.backtest_result_id,
                    er.evaluation_result_id,er.overall_score,er.total_return,er.win_rate,
                    er.maximum_drawdown,er.number_of_trades,er.metric_version,
                    coalesce(lr.ranking_version,er.ranking_version) ranking_version,
                    er.leaderboard_eligible,er.evaluated_at,
                    le.rank,lr.created_at revision_created_at
                from experiment.candidate_definition c
                left join experiment.job j on j.experiment_id=c.experiment_id
                    and j.candidate_id=c.candidate_id and j.job_type='BACKTEST'
                left join experiment.backtest_result br on br.job_id=j.job_id
                left join experiment.execution_attempt sa
                    on sa.attempt_id=br.successful_attempt_id
                left join lateral (
                    select attempt_no,retryable,next_retry_at
                    from experiment.execution_attempt candidate_attempt
                    where candidate_attempt.job_id=j.job_id
                    order by candidate_attempt.attempt_no desc
                    limit 1
                ) ea on true
                left join lateral (
                    select candidate_evaluation.*
                    from experiment.evaluation_result candidate_evaluation
                    where candidate_evaluation.experiment_id=c.experiment_id
                        and candidate_evaluation.backtest_result_id=br.backtest_result_id
                    order by candidate_evaluation.evaluated_at desc,
                        candidate_evaluation.evaluation_result_id desc
                    limit 1
                ) er on true
                left join latest_revision lr on true
                left join experiment.leaderboard_entry le
                    on le.leaderboard_revision_id=lr.leaderboard_revision_id
                    and le.evaluation_result_id=er.evaluation_result_id
                where c.experiment_id=?
                """ + candidateFilter + """
                order by c.generation_index,c.candidate_id
                """;
        Object[] arguments = candidateId == null
                ? new Object[] {experimentId.value(), experimentId.value()}
                : new Object[] {experimentId.value(), experimentId.value(), candidateId.value()};
        return List.copyOf(jdbc.query(sql, (rs, row) -> {
            String jobStatus = rs.getString("job_status");
            String backtestId = rs.getString("backtest_result_id");
            String evaluationId = rs.getString("evaluation_result_id");
            // A committed immutable result is authoritative. A later duplicate
            // delivery must not make the candidate look failed after success.
            boolean backtestFailure = "FAILED".equals(jobStatus) && backtestId == null;
            boolean evaluationFailure = "FAILED".equals(jobStatus)
                    && backtestId != null && evaluationId == null;
            String backtestStatus = backtestId != null ? "SUCCEEDED"
                    : jobStatus == null ? "QUEUED" : jobStatus;
            String evaluationStatus = evaluationId != null ? "SUCCEEDED"
                    : evaluationFailure ? "FAILED"
                    : backtestFailure || "CANCELLED".equals(jobStatus)
                            ? "NOT_STARTED" : "PENDING";
            Integer rank = rs.getObject("rank", Integer.class);
            Boolean eligible = evaluationId == null ? null : rs.getBoolean("leaderboard_eligible");
            Integer numberOfTrades = evaluationId == null
                    ? null : rs.getInt("number_of_trades");
            CandidatePipelineEntry.EligibilityReason eligibilityReason = null;
            if (evaluationId != null && !Boolean.TRUE.equals(eligible)) {
                eligibilityReason = numberOfTrades < EvaluationEligibilityPolicy.MINIMUM_TRADES
                        ? new CandidatePipelineEntry.EligibilityReason(
                                "MINIMUM_TRADES_NOT_MET", numberOfTrades,
                                EvaluationEligibilityPolicy.MINIMUM_TRADES)
                        : new CandidatePipelineEntry.EligibilityReason(
                                "UNSPECIFIED", numberOfTrades, null);
            }
            java.sql.Timestamp evaluated = rs.getTimestamp("evaluated_at");
            java.sql.Timestamp revisionCreated = rs.getTimestamp("revision_created_at");
            String rankingStatus;
            if (evaluationId == null) rankingStatus = "PENDING".equals(evaluationStatus)
                    ? "PENDING" : "NOT_STARTED";
            else if (rank != null) rankingStatus = "RANKED";
            else if (!Boolean.TRUE.equals(eligible)) rankingStatus = "INELIGIBLE";
            else if (revisionCreated != null && evaluated != null
                    && !revisionCreated.toInstant().isBefore(evaluated.toInstant()))
                rankingStatus = "NOT_IN_TOP_K";
            else rankingStatus = "PENDING";
            String failureStage = backtestFailure ? "BACKTEST"
                    : evaluationFailure ? "EVALUATION" : null;
            String failureCode = evaluationId == null ? rs.getString("failure_code") : null;
            String failureMessage = rs.getString("failure_message");
            CandidatePipelineEntry.Failure failure = failureCode == null ? null
                    : new CandidatePipelineEntry.Failure(
                            failureCode, failureMessage == null ? "" : failureMessage);
            return new CandidatePipelineEntry(experimentId,
                    new CandidateId(rs.getString("candidate_id")),
                    rs.getInt("generation_index"), map(rs.getString("definition")),
                    rs.getString("fingerprint"), rs.getTimestamp("created_at").toInstant(),
                    new CandidatePipelineEntry.BacktestStage(
                            rs.getString("job_id") == null ? null : new JobId(rs.getString("job_id")),
                            backtestStatus,
                            backtestId == null ? null : new LeaderboardBacktestResultId(backtestId),
                            instant(rs.getTimestamp("started_at")),
                            instant(rs.getTimestamp("finished_at")),
                            rs.getObject("attempt_no", Integer.class),
                            evaluationId == null
                                    ? instant(rs.getTimestamp("next_retry_at")) : null,
                            evaluationId == null && ("RETRY_SCHEDULED".equals(jobStatus)
                                    || rs.getBoolean("attempt_retryable")), failure),
                    new CandidatePipelineEntry.EvaluationStage(
                            evaluationStatus,
                            evaluationId == null ? null : new EvaluationResultId(evaluationId),
                            rs.getBigDecimal("overall_score"), rs.getBigDecimal("total_return"),
                            rs.getBigDecimal("win_rate"), rs.getBigDecimal("maximum_drawdown"),
                            numberOfTrades, rs.getString("metric_version"), eligible,
                            eligibilityReason, instant(evaluated)),
                    new CandidatePipelineEntry.RankingStage(rankingStatus, rank,
                            rs.getString("ranking_version")), failureStage);
        }, arguments));
    }

    @Override
    public Optional<LeaderboardCandidateEvidence> findCandidateEvidence(
            ExperimentId experimentId, CandidateId candidateId) {
        return evidence(EVIDENCE_SELECT + """
                where c.experiment_id=? and c.candidate_id=?
                order by er.evaluated_at desc nulls last limit 1
                """, experimentId.value(), candidateId.value());
    }

    private Optional<LeaderboardCandidateEvidence> evidence(String sql, Object... arguments) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, (rs, row) -> {
                EvaluationResult evaluation = rs.getString("evaluation_result_id") == null ? null
                        : new EvaluationResult(
                                new EvaluationResultId(rs.getString("evaluation_result_id")),
                                new ExperimentId(rs.getString("experiment_id")),
                                new BacktestResultId(rs.getString("backtest_result_id")),
                                new MetricVersion(rs.getString("metric_version")),
                                new RankingVersion(rs.getString("ranking_version")),
                                rs.getBigDecimal("total_return"), rs.getBigDecimal("win_rate"),
                                rs.getBigDecimal("maximum_drawdown"), rs.getInt("number_of_trades"),
                                rs.getBigDecimal("overall_score"), rs.getBoolean("leaderboard_eligible"),
                                rs.getString("evaluation_fingerprint"),
                                rs.getTimestamp("evaluated_at").toInstant());
                return new LeaderboardCandidateEvidence(
                        new ExperimentId(rs.getString("experiment_id")),
                        new CandidateId(rs.getString("candidate_id")), rs.getInt("generation_index"),
                        map(rs.getString("definition")), map(rs.getString("generator_state")),
                        rs.getString("fingerprint"), rs.getString("backtest_result_id") == null ? null
                                : new LeaderboardBacktestResultId(rs.getString("backtest_result_id")),
                        rs.getString("backtest_result_id") != null ? "SUCCEEDED"
                                : rs.getString("status") == null ? "QUEUED" : rs.getString("status"),
                        evaluation);
            }, arguments));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private static java.util.Map<String, Object> map(String value) {
        if (value == null) return java.util.Map.of();
        try {
            return JSON.readValue(value, new TypeReference<>() {});
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Stored candidate evidence is invalid", failure);
        }
    }

    private static java.time.Instant instant(java.sql.Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
