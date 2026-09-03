package com.cryptostrategy.platform.persistence.internal.search;

final class SearchSql {
    static final String COLUMNS = """
            search_run_id, experiment_id, search_job_id, mode, source_experiment_id,
            generator_id, generator_version, seed, search_space_fingerprint,
            generator_state_contract_version, generator_state::text as generator_state,
            generator_state_fingerprint, next_generation_index, maximum_candidates,
            maximum_duration_ms, max_in_flight, status, version, started_at, deadline_at,
            finished_at, failure_code, failure_message, created_at, updated_at
            """;

    static final String INSERT_RUN = """
            insert into search.search_run (
                search_run_id, experiment_id, search_job_id, mode, source_experiment_id,
                generator_id, generator_version, seed, search_space_fingerprint,
                generator_state_contract_version, generator_state, generator_state_fingerprint,
                next_generation_index, maximum_candidates, maximum_duration_ms, max_in_flight,
                status, version, started_at, deadline_at, finished_at, failure_code,
                failure_message, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    static final String SELECT_BY_ID = "select " + COLUMNS
            + " from search.search_run where search_run_id = ?";
    static final String SELECT_BY_EXPERIMENT = "select " + COLUMNS
            + " from search.search_run where experiment_id = ?";
    static final String SELECT_BY_SEARCH_JOB = "select " + COLUMNS
            + " from search.search_run where search_job_id = ?";

    static final String UPDATE_FENCED = """
            update search.search_run set
                generator_state_contract_version = ?, generator_state = ?::jsonb,
                generator_state_fingerprint = ?, next_generation_index = ?, status = ?,
                version = ?, started_at = ?, deadline_at = ?, finished_at = ?,
                failure_code = ?, failure_message = ?, updated_at = ?
            where search_run_id = ? and version = ?
              and status not in ('COMPLETED', 'STOPPED', 'FAILED')
            """;

    static final String INSERT_DECISION_FENCED = """
            insert into search.coordination_decision (
                decision_id, search_run_id, sequence, decision_type, candidate_id,
                backtest_job_id, candidate_fingerprint, state_before_fingerprint,
                state_after_fingerprint, reason_code, decided_at
            )
            select ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            from search.search_run
            where search_run_id = ? and version = ?
            on conflict do nothing
            """;

    static final String SELECT_RECOVERABLE = "select " + COLUMNS + """
            from search.search_run
            where status in ('PENDING', 'RUNNING', 'STOPPING') and updated_at <= ?
            order by updated_at, search_run_id
            limit ?
            """;

    private SearchSql() {
    }
}
