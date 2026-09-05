package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import com.cryptostrategy.platform.search.api.model.SearchExperimentId;
import com.cryptostrategy.platform.search.api.model.SearchJobId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

final class SearchRows {
    SearchRun mapSearchRun(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SearchRun(
                new SearchRunId(resultSet.getString("search_run_id")),
                new SearchExperimentId(resultSet.getString("experiment_id")),
                new SearchJobId(resultSet.getString("search_job_id")),
                SearchRunMode.valueOf(resultSet.getString("mode")),
                resultSet.getString("source_experiment_id") == null ? null : new SearchExperimentId(resultSet.getString("source_experiment_id")),
                new GeneratorId(resultSet.getString("generator_id")),
                GeneratorVersion.parse(resultSet.getString("generator_version")),
                resultSet.getLong("seed"),
                resultSet.getString("search_space_fingerprint"),
                new GeneratorState(
                        resultSet.getString("generator_state_contract_version"),
                        resultSet.getString("generator_state"),
                        resultSet.getString("generator_state_fingerprint")),
                resultSet.getLong("next_generation_index"),
                new SearchStopConditions(
                        resultSet.getInt("maximum_candidates"),
                        Duration.ofMillis(resultSet.getLong("maximum_duration_ms")),
                        integer(resultSet, "maximum_without_improvement")),
                resultSet.getInt("max_in_flight"),
                SearchRunStatus.valueOf(resultSet.getString("status")),
                terminalReason(resultSet),
                resultSet.getLong("version"),
                instant(resultSet, "started_at"),
                instant(resultSet, "deadline_at"),
                instant(resultSet, "finished_at"),
                resultSet.getString("failure_code"),
                resultSet.getString("failure_message"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }

    private static java.time.Instant instant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Integer integer(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static com.cryptostrategy.platform.search.api.model.SearchTerminalReason terminalReason(
            ResultSet resultSet) throws SQLException {
        String reason = resultSet.getString("terminal_reason");
        if (reason != null) {
            return com.cryptostrategy.platform.search.api.model.SearchTerminalReason.valueOf(reason);
        }
        return switch (SearchRunStatus.valueOf(resultSet.getString("status"))) {
            case COMPLETED -> com.cryptostrategy.platform.search.api.model.SearchTerminalReason.MAXIMUM_CANDIDATES;
            case STOPPED, STOPPING -> com.cryptostrategy.platform.search.api.model.SearchTerminalReason.EXPLICIT_STOP;
            case FAILED -> com.cryptostrategy.platform.search.api.model.SearchTerminalReason.TERMINAL_FAILURE;
            default -> null;
        };
    }
}
