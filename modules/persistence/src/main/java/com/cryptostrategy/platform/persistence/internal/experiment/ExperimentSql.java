package com.cryptostrategy.platform.persistence.internal.experiment;

public final class ExperimentSql {

    private ExperimentSql() {}

    // Experiment queries
    public static final String INSERT_EXPERIMENT = """
            INSERT INTO experiment.experiment (
                experiment_id, owner_user_id, name, status, derived_from_experiment_id,
                reproduces_experiment_id, started_at, completed_at, failure_code, failure_message, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public static final String INSERT_MANIFEST = """
            INSERT INTO experiment.experiment_manifest (
                experiment_id, manifest_version, dataset_version_id, strategy_kind,
                strategy_ref_id, strategy_version, strategy_parameters, backtest_config,
                search_config, evaluation_config, sentiment_config, software_version,
                git_commit, fingerprint, created_at, source_user_strategy_version_id,
                dataset_provenance, strategy_provenance
            ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
            """;

    public static final String SELECT_EXPERIMENT_BY_ID = """
            SELECT experiment_id, owner_user_id, name, status, derived_from_experiment_id,
                   reproduces_experiment_id, started_at, completed_at, failure_code, failure_message, created_at
            FROM experiment.experiment
            WHERE experiment_id = ? AND owner_user_id = ?
            """;

    public static final String SELECT_MANIFEST_BY_EXPERIMENT_ID = """
            SELECT m.experiment_id, m.manifest_version, m.dataset_version_id, m.strategy_kind,
                   m.strategy_ref_id, m.strategy_version, m.strategy_parameters, m.backtest_config,
                   m.search_config, m.evaluation_config, m.sentiment_config, m.software_version,
                   m.git_commit, m.fingerprint, m.created_at, m.source_user_strategy_version_id,
                   m.dataset_provenance, m.strategy_provenance
            FROM experiment.experiment_manifest m
            JOIN experiment.experiment e ON e.experiment_id = m.experiment_id
            WHERE m.experiment_id = ? AND e.owner_user_id = ?
            """;

    public static final String UPDATE_MANIFEST = """
            UPDATE experiment.experiment_manifest m
            SET dataset_version_id = ?, strategy_kind = ?, strategy_ref_id = ?, strategy_version = ?,
                strategy_parameters = ?::jsonb, backtest_config = ?::jsonb, search_config = ?::jsonb,
                evaluation_config = ?::jsonb, sentiment_config = ?::jsonb, software_version = ?,
                git_commit = ?, source_user_strategy_version_id = ?, dataset_provenance = ?::jsonb,
                strategy_provenance = ?::jsonb
            FROM experiment.experiment e
            WHERE m.experiment_id = e.experiment_id AND m.experiment_id = ? AND e.owner_user_id = ?
            """;

    public static final String FREEZE_AND_QUEUE_EXPERIMENT = """
            UPDATE experiment.experiment
            SET status = 'QUEUED', started_at = ?
            WHERE experiment_id = ? AND owner_user_id = ? AND status = 'CREATED'
            """;

    public static final String UPDATE_MANIFEST_FINGERPRINT = """
            UPDATE experiment.experiment_manifest m
            SET fingerprint = ?
            FROM experiment.experiment e
            WHERE m.experiment_id = e.experiment_id AND m.experiment_id = ? AND e.owner_user_id = ?
            """;

    public static final String UPDATE_EXPERIMENT_STATUS = """
            UPDATE experiment.experiment
            SET status = ?, started_at = coalesce(started_at, ?), completed_at = ?
            WHERE experiment_id = ? AND owner_user_id = ?
            """;

    public static final String SELECT_OWNER_BY_EXPERIMENT_ID = """
            SELECT owner_user_id
            FROM experiment.experiment
            WHERE experiment_id = ?
            """;

    public static final String SELECT_STOP_COMPLETION_CANDIDATES = """
            SELECT experiment_id, completed_at
            FROM experiment.experiment
            WHERE status = 'STOP_REQUESTED'
            ORDER BY created_at ASC
            LIMIT ?
            """;

    // Candidate queries
    public static final String INSERT_CANDIDATE = """
            INSERT INTO experiment.candidate_definition (
                candidate_id, experiment_id, generation_index, definition, generator_state, fingerprint, created_at
            ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
            """;

    public static final String SELECT_CANDIDATE_BY_ID = """
            SELECT c.candidate_id, c.experiment_id, c.generation_index, c.definition,
                   c.generator_state, c.fingerprint, c.created_at
            FROM experiment.candidate_definition c
            JOIN experiment.experiment e ON e.experiment_id = c.experiment_id
            WHERE c.candidate_id = ? AND e.owner_user_id = ?
            """;

    public static final String SELECT_CANDIDATES_BY_EXPERIMENT_ID = """
            SELECT c.candidate_id, c.experiment_id, c.generation_index, c.definition,
                   c.generator_state, c.fingerprint, c.created_at
            FROM experiment.candidate_definition c
            JOIN experiment.experiment e ON e.experiment_id = c.experiment_id
            WHERE c.experiment_id = ? AND e.owner_user_id = ?
            ORDER BY c.generation_index ASC, c.candidate_id ASC
            """;

    public static final String SELECT_CANDIDATE_PAGE = """
            SELECT c.candidate_id, c.experiment_id, c.generation_index, c.definition,
                   c.generator_state, c.fingerprint, c.created_at
            FROM experiment.candidate_definition c
            JOIN experiment.experiment e ON e.experiment_id = c.experiment_id
            WHERE c.experiment_id = ? AND e.owner_user_id = ?
              AND (c.generation_index > ?
                   OR (c.generation_index = ? AND c.candidate_id > ?))
            ORDER BY c.generation_index ASC, c.candidate_id ASC
            LIMIT ?
            """;

    // Standalone Backtest queries
    public static final String INSERT_STANDALONE_BACKTEST = """
            INSERT INTO experiment.standalone_backtest (
                backtest_id, experiment_id, candidate_id, job_id, created_at
            ) VALUES (?, ?, ?, ?, ?)
            """;

    public static final String SELECT_STANDALONE_BACKTEST_BY_ID = """
            SELECT b.backtest_id, b.experiment_id, b.candidate_id, b.job_id, b.created_at
            FROM experiment.standalone_backtest b
            JOIN experiment.experiment e ON e.experiment_id = b.experiment_id
            WHERE b.backtest_id = ? AND e.owner_user_id = ?
            """;

    // Job queries
    public static final String INSERT_JOB = """
            INSERT INTO experiment.job (
                job_id, experiment_id, candidate_id, job_type, status, correlation_id,
                total_work, completed_work, failed_work, best_score, queued_at, started_at,
                finished_at, next_retry_at, failure_code, failure_message, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public static final String SELECT_JOB_BY_ID = """
            SELECT j.job_id, j.experiment_id, j.candidate_id, j.job_type, j.status, j.correlation_id,
                   j.total_work, j.completed_work, j.failed_work, j.best_score, j.queued_at, j.started_at,
                   j.finished_at, j.next_retry_at, j.failure_code, j.failure_message, j.created_at, j.updated_at
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE j.job_id = ? AND e.owner_user_id = ?
            """;

    public static final String SELECT_JOB_BY_ID_FOR_UPDATE = """
            SELECT j.job_id, j.experiment_id, j.candidate_id, j.job_type, j.status, j.correlation_id,
                   j.total_work, j.completed_work, j.failed_work, j.best_score, j.queued_at, j.started_at,
                   j.finished_at, j.next_retry_at, j.failure_code, j.failure_message, j.created_at, j.updated_at
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE j.job_id = ? AND e.owner_user_id = ?
            FOR UPDATE OF j
            """;

    public static final String SELECT_BACKTEST_JOB_BY_CANDIDATE = """
            SELECT j.job_id, j.experiment_id, j.candidate_id, j.job_type, j.status, j.correlation_id,
                   j.total_work, j.completed_work, j.failed_work, j.best_score, j.queued_at, j.started_at,
                   j.finished_at, j.next_retry_at, j.failure_code, j.failure_message, j.created_at, j.updated_at
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE j.candidate_id = ? AND j.job_type = 'BACKTEST' AND e.owner_user_id = ?
            """;

    public static final String SELECT_JOBS_BY_EXPERIMENT_ID = """
            SELECT j.job_id, j.experiment_id, j.candidate_id, j.job_type, j.status, j.correlation_id,
                   j.total_work, j.completed_work, j.failed_work, j.best_score, j.queued_at, j.started_at,
                   j.finished_at, j.next_retry_at, j.failure_code, j.failure_message, j.created_at, j.updated_at
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE j.experiment_id = ? AND e.owner_user_id = ?
            ORDER BY j.created_at ASC
            """;

    public static final String SELECT_ALL_JOBS_BY_EXPERIMENT_ID = """
            SELECT j.job_id, j.experiment_id, j.candidate_id, j.job_type, j.status, j.correlation_id,
                   j.total_work, j.completed_work, j.failed_work, j.best_score, j.queued_at, j.started_at,
                   j.finished_at, j.next_retry_at, j.failure_code, j.failure_message, j.created_at, j.updated_at
            FROM experiment.job j
            WHERE j.experiment_id = ?
            ORDER BY j.created_at ASC
            """;

    public static final String SELECT_UNFINISHED_JOBS = """
            SELECT job_id, experiment_id, candidate_id, job_type, status, correlation_id,
                   total_work, completed_work, failed_work, best_score, queued_at, started_at,
                   finished_at, next_retry_at, failure_code, failure_message, created_at, updated_at
            FROM experiment.job
            WHERE status IN ('QUEUED', 'RUNNING', 'RETRY_SCHEDULED', 'CANCEL_REQUESTED')
            ORDER BY created_at ASC
            """;

    public static final String SELECT_OWNER_BY_JOB_ID = """
            SELECT e.owner_user_id
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE j.job_id = ?
            """;

    public static final String UPDATE_JOB_STATUS = """
            UPDATE experiment.job j
            SET status = ?, started_at = coalesce(j.started_at, ?), finished_at = ?,
                next_retry_at = ?, failure_code = ?, failure_message = ?, updated_at = ?
            FROM experiment.experiment e
            WHERE j.experiment_id = e.experiment_id AND j.job_id = ? AND e.owner_user_id = ?
            """;

    public static final String UPDATE_JOB_STATUS_GUARDED = """
            UPDATE experiment.job j
            SET status = ?, started_at = coalesce(j.started_at, ?), finished_at = ?,
                next_retry_at = ?, failure_code = ?, failure_message = ?, updated_at = ?
            FROM experiment.experiment e
            WHERE j.experiment_id = e.experiment_id AND j.job_id = ? AND e.owner_user_id = ?
              AND j.status = 'RUNNING'
            """;

    public static final String UPDATE_JOB_STATUS_CANCEL_GUARDED = """
            UPDATE experiment.job j
            SET status = 'CANCELLED', finished_at = ?, updated_at = ?
            FROM experiment.experiment e
            WHERE j.experiment_id = e.experiment_id AND j.job_id = ? AND e.owner_user_id = ?
              AND j.status IN ('RUNNING', 'CANCEL_REQUESTED')
            """;

    public static final String UPDATE_JOB_PROGRESS = """
            UPDATE experiment.job j
            SET completed_work = ?, failed_work = ?, best_score = coalesce(?, best_score), updated_at = ?
            FROM experiment.experiment e
            WHERE j.experiment_id = e.experiment_id AND j.job_id = ? AND e.owner_user_id = ?
            """;

    public static final String SELECT_RECOVERABLE_QUEUED_JOBS = """
            SELECT j.job_id, j.experiment_id, j.candidate_id, j.queued_at
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE j.status = 'QUEUED' AND j.queued_at < ? AND e.status = 'QUEUED'
            ORDER BY j.queued_at ASC
            LIMIT ?
            """;

    public static final String SELECT_DUE_RETRIES = """
            SELECT j.job_id, j.experiment_id, j.candidate_id, j.next_retry_at
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE j.status = 'RETRY_SCHEDULED' AND j.next_retry_at <= ? AND e.status NOT IN ('CANCELLED', 'STOPPED')
            ORDER BY j.next_retry_at ASC
            LIMIT ?
            """;

    // Attempt queries
    public static final String SELECT_MAX_ATTEMPT_NO = """
            SELECT coalesce(max(attempt_no), 0)
            FROM experiment.execution_attempt
            WHERE job_id = ?
            """;

    public static final String INSERT_ATTEMPT = """
            INSERT INTO experiment.execution_attempt (
                attempt_id, job_id, candidate_id, attempt_no, status, worker_id,
                started_at, finished_at, next_retry_at, failure_code, failure_message,
                retryable, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public static final String UPDATE_ATTEMPT_STATUS = """
            UPDATE experiment.execution_attempt ea
            SET status = ?, finished_at = ?, next_retry_at = ?, failure_code = ?, failure_message = ?, retryable = ?
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE ea.job_id = j.job_id AND ea.attempt_id = ? AND j.job_id = ? AND e.owner_user_id = ?
            """;

    public static final String UPDATE_ATTEMPT_STATUS_GUARDED = """
            UPDATE experiment.execution_attempt ea
            SET status = ?, finished_at = ?, next_retry_at = ?, failure_code = ?, failure_message = ?, retryable = ?
            FROM experiment.job j
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE ea.job_id = j.job_id AND ea.attempt_id = ? AND j.job_id = ? AND e.owner_user_id = ?
              AND ea.status = 'RUNNING'
            """;

    public static final String SELECT_ATTEMPTS_BY_JOB_ID = """
            SELECT ea.attempt_id, ea.job_id, ea.candidate_id, ea.attempt_no, ea.status, ea.worker_id,
                   ea.started_at, ea.finished_at, ea.next_retry_at, ea.failure_code, ea.failure_message,
                   ea.retryable, ea.created_at
            FROM experiment.execution_attempt ea
            JOIN experiment.job j ON j.job_id = ea.job_id
            JOIN experiment.experiment e ON e.experiment_id = j.experiment_id
            WHERE ea.job_id = ? AND e.owner_user_id = ?
            ORDER BY ea.attempt_no ASC
            """;

    public static final String SELECT_STALE_RUNNING_ATTEMPTS = """
            SELECT ea.job_id, ea.attempt_id, j.experiment_id, ea.candidate_id, ea.worker_id, ea.started_at
            FROM experiment.execution_attempt ea
            JOIN experiment.job j ON j.job_id = ea.job_id
            WHERE ea.status = 'RUNNING' AND ea.started_at < ?
            ORDER BY ea.started_at ASC
            LIMIT ?
            """;

    // Idempotency queries
    public static final String INSERT_IDEMPOTENCY_CLAIM = """
            INSERT INTO platform.idempotency_record (
                user_id, scope, idempotency_key, request_hash, state, created_at, expires_at
            ) VALUES (?, ?, ?, ?, 'IN_PROGRESS', ?, ?)
            ON CONFLICT (user_id, scope, idempotency_key) DO NOTHING
            """;

    public static final String SELECT_IDEMPOTENCY_RECORD = """
            SELECT user_id, scope, idempotency_key, request_hash, state, response_status, response_body, created_at, expires_at
            FROM platform.idempotency_record
            WHERE user_id = ? AND scope = ? AND idempotency_key = ?
            """;

    public static final String COMPLETE_IDEMPOTENCY_RECORD = """
            UPDATE platform.idempotency_record
            SET state = 'COMPLETED', response_status = ?, response_body = ?::jsonb
            WHERE user_id = ? AND scope = ? AND idempotency_key = ?
            """;

    // Outbox queries
    public static final String INSERT_OUTBOX_EVENT = """
            INSERT INTO platform.outbox_event (
                outbox_event_id, message_id, aggregate_type, aggregate_id, event_type,
                event_version, payload, headers, occurred_at, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
            """;
}
