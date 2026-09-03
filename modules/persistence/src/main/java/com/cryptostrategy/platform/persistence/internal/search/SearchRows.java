package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

final class SearchRows {
    SearchRun mapSearchRun(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SearchRun(
                new SearchRunId(resultSet.getString("search_run_id")),
                resultSet.getString("experiment_id"),
                resultSet.getString("search_job_id"),
                SearchRunMode.valueOf(resultSet.getString("mode")),
                resultSet.getString("source_experiment_id"),
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
                        Duration.ofMillis(resultSet.getLong("maximum_duration_ms"))),
                resultSet.getInt("max_in_flight"),
                SearchRunStatus.valueOf(resultSet.getString("status")),
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
}
