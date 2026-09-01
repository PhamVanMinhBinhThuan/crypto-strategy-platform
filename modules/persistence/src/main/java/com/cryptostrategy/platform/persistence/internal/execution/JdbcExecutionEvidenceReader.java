package com.cryptostrategy.platform.persistence.internal.execution;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.execution.api.ExecutionEvidence;
import com.cryptostrategy.platform.execution.api.port.out.ExecutionEvidenceReader;
import com.cryptostrategy.platform.persistence.internal.backtesting.JdbcBacktestEvidenceReader;
import com.cryptostrategy.platform.persistence.internal.backtesting.BacktestJsonMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.Collections;

public final class JdbcExecutionEvidenceReader implements ExecutionEvidenceReader {
    private final JdbcTemplate jdbc;
    private final JdbcBacktestEvidenceReader backtestReader;

    public JdbcExecutionEvidenceReader(JdbcTemplate jdbc) { 
        this.jdbc = Objects.requireNonNull(jdbc);
        this.backtestReader = new JdbcBacktestEvidenceReader(jdbc, new BacktestJsonMapper());
    }

    @Override
    public ExecutionEvidence load(UUID ownerUserId, ExperimentId experimentId) {
        String resultId = jdbc.queryForObject(
            "SELECT backtest_result_id FROM experiment.backtest_result WHERE experiment_id = ? ORDER BY completed_at DESC LIMIT 1",
            String.class, experimentId.value()
        );
        BacktestResult backtest = backtestReader.findById(new BacktestResultId(resultId))
            .orElseThrow(() -> new IllegalArgumentException("No backtest result found for " + experimentId));

        List<EvaluationResult> evaluations = jdbc.query(
            "SELECT evaluation_result_id, experiment_id, backtest_result_id, metric_version, ranking_version, total_return, win_rate, maximum_drawdown, number_of_trades, overall_score, leaderboard_eligible, evaluation_fingerprint, evaluated_at FROM experiment.evaluation_result WHERE experiment_id = ? ORDER BY evaluated_at DESC LIMIT 1",
            (rs, n) -> new EvaluationResult(new EvaluationResultId(rs.getString(1)), new ExperimentId(rs.getString(2)), new BacktestResultId(rs.getString(3)),
                    new MetricVersion(rs.getString(4)), new RankingVersion(rs.getString(5)), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8),
                    rs.getInt(9), rs.getBigDecimal(10), rs.getBoolean(11), rs.getString(12), rs.getTimestamp(13).toInstant()),
            experimentId.value()
        );
        if (evaluations.isEmpty()) throw new IllegalArgumentException("No evaluation result found for " + experimentId);
        EvaluationResult evaluation = evaluations.getFirst();

        List<LeaderboardRevision> leaderboards = jdbc.query(
            "SELECT leaderboard_revision_id, revision_no, top_k, ranking_version, revision_fingerprint, created_at FROM experiment.leaderboard_revision WHERE experiment_id = ? ORDER BY revision_no DESC LIMIT 1",
            (rs, n) -> new LeaderboardRevision(new LeaderboardRevisionId(rs.getString(1)), new ExperimentId(experimentId.value()), rs.getLong(2),
                    rs.getInt(3), new RankingVersion(rs.getString(4)), Collections.emptyList(), rs.getString(5), rs.getTimestamp(6).toInstant()),
            experimentId.value()
        );
        LeaderboardRevision leaderboard = leaderboards.isEmpty() ? null : leaderboards.getFirst();

        return new ExecutionEvidence(backtest, evaluation, leaderboard);
    }
}
