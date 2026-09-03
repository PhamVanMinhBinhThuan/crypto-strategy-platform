-- Migration: 20260903000100_f010_search_coordinator.sql
-- Description: Durable Search-owned orchestration state, decision audit and async reproduction verification.

begin;

create schema if not exists search;

alter table experiment.job
    add constraint job_id_experiment_unique unique (job_id, experiment_id);

create table search.search_run (
    search_run_id varchar(26) primary key
        check (search_run_id ~ '^[0-7][0-9A-HJKMNP-TV-Z]{25}$'),
    experiment_id varchar(26) not null unique,
    search_job_id varchar(26) not null unique,
    mode text not null check (mode in ('GENERATION', 'REPRODUCTION')),
    source_experiment_id varchar(26) references experiment.experiment(experiment_id),
    generator_id text not null check (generator_id ~ '^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$'),
    generator_version text not null check (generator_version ~ '^[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$'),
    seed bigint not null,
    search_space_fingerprint text not null check (search_space_fingerprint <> ''),
    generator_state_contract_version text not null check (generator_state_contract_version <> ''),
    generator_state jsonb not null,
    generator_state_fingerprint text not null check (generator_state_fingerprint <> ''),
    next_generation_index integer not null default 0 check (next_generation_index >= 0),
    maximum_candidates integer not null check (maximum_candidates > 0),
    maximum_duration_ms bigint not null check (maximum_duration_ms > 0),
    max_in_flight integer not null check (max_in_flight > 0),
    status text not null check (status in ('PENDING', 'RUNNING', 'STOPPING', 'COMPLETED', 'STOPPED', 'FAILED')),
    version bigint not null default 0 check (version >= 0),
    started_at timestamptz,
    deadline_at timestamptz,
    finished_at timestamptz,
    failure_code text,
    failure_message text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint search_run_experiment_fk
        foreign key (experiment_id) references experiment.experiment(experiment_id),
    constraint search_run_job_experiment_fk
        foreign key (search_job_id, experiment_id)
        references experiment.job(job_id, experiment_id),
    constraint search_run_mode_source_check check (
        (mode = 'GENERATION' and source_experiment_id is null)
        or (mode = 'REPRODUCTION' and source_experiment_id is not null and source_experiment_id <> experiment_id)
    ),
    constraint search_run_generation_bound_check
        check (next_generation_index <= maximum_candidates),
    constraint search_run_clock_check check (
        updated_at >= created_at
        and ((started_at is null and deadline_at is null) or
             (started_at is not null and deadline_at is not null and deadline_at >= started_at))
        and (finished_at is null or started_at is null or finished_at >= started_at)
    ),
    constraint search_run_lifecycle_check check (
        (status = 'PENDING' and started_at is null and deadline_at is null and finished_at is null)
        or (status in ('RUNNING', 'STOPPING') and finished_at is null)
        or (status in ('COMPLETED', 'STOPPED', 'FAILED') and finished_at is not null)
    ),
    constraint search_run_failure_check check (
        (status = 'FAILED' and failure_code ~ '^[A-Z][A-Z0-9_]*$' and failure_message <> '')
        or (status <> 'FAILED' and failure_code is null and failure_message is null)
    )
);

create index search_run_recovery_idx
    on search.search_run(status, updated_at, search_run_id)
    where status in ('PENDING', 'RUNNING', 'STOPPING');

create table search.coordination_decision (
    decision_id varchar(26) primary key
        check (decision_id ~ '^[0-7][0-9A-HJKMNP-TV-Z]{25}$'),
    search_run_id varchar(26) not null references search.search_run(search_run_id),
    sequence bigint not null check (sequence >= 0),
    decision_type text not null check (decision_type in ('ALLOCATED', 'DUPLICATE_SKIPPED', 'STOP_REACHED', 'FAILED')),
    candidate_id varchar(26) references experiment.candidate_definition(candidate_id),
    backtest_job_id varchar(26) references experiment.job(job_id),
    candidate_fingerprint text,
    state_before_fingerprint text not null check (state_before_fingerprint <> ''),
    state_after_fingerprint text not null check (state_after_fingerprint <> ''),
    reason_code text not null check (reason_code ~ '^[A-Z][A-Z0-9_]*$'),
    decided_at timestamptz not null,
    constraint coordination_decision_run_sequence_unique unique (search_run_id, sequence),
    constraint coordination_decision_payload_check check (
        (decision_type = 'ALLOCATED' and candidate_id is not null and backtest_job_id is not null
            and candidate_fingerprint is not null and candidate_fingerprint <> '')
        or (decision_type = 'DUPLICATE_SKIPPED' and candidate_id is null and backtest_job_id is null
            and candidate_fingerprint is not null and candidate_fingerprint <> '')
        or (decision_type in ('STOP_REACHED', 'FAILED') and candidate_id is null and backtest_job_id is null)
    )
);

create unique index coordination_decision_allocated_candidate_unique
    on search.coordination_decision(candidate_id)
    where decision_type = 'ALLOCATED';

create index coordination_decision_run_idx
    on search.coordination_decision(search_run_id, sequence);

create table search.reproduction_verification (
    verification_id varchar(26) primary key
        check (verification_id ~ '^[0-7][0-9A-HJKMNP-TV-Z]{25}$'),
    source_experiment_id varchar(26) not null references experiment.experiment(experiment_id),
    reproduction_experiment_id varchar(26) not null unique references experiment.experiment(experiment_id),
    status text not null check (status in ('PENDING', 'RUNNING', 'MATCHED', 'MISMATCHED', 'FAILED')),
    version bigint not null default 0 check (version >= 0),
    trade_sequence_matched boolean,
    metrics_matched boolean,
    fingerprints_matched boolean,
    source_evidence_fingerprint text,
    reproduction_evidence_fingerprint text,
    safe_differences jsonb,
    failure_code text,
    failure_message text,
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint reproduction_verification_lineage_unique
        unique (source_experiment_id, reproduction_experiment_id),
    constraint reproduction_verification_distinct_check
        check (source_experiment_id <> reproduction_experiment_id),
    constraint reproduction_verification_lifecycle_check check (
        (status = 'PENDING' and started_at is null and finished_at is null)
        or (status = 'RUNNING' and started_at is not null and finished_at is null)
        or (status in ('MATCHED', 'MISMATCHED', 'FAILED') and finished_at is not null)
    ),
    constraint reproduction_verification_outcome_check check (
        (status in ('PENDING', 'RUNNING')
            and trade_sequence_matched is null and metrics_matched is null and fingerprints_matched is null
            and source_evidence_fingerprint is null and reproduction_evidence_fingerprint is null
            and safe_differences is null and failure_code is null and failure_message is null)
        or (status = 'MATCHED'
            and trade_sequence_matched is true and metrics_matched is true and fingerprints_matched is true
            and source_evidence_fingerprint is not null and reproduction_evidence_fingerprint is not null
            and safe_differences is not null and failure_code is null and failure_message is null)
        or (status = 'MISMATCHED'
            and trade_sequence_matched is not null and metrics_matched is not null and fingerprints_matched is not null
            and not (trade_sequence_matched and metrics_matched and fingerprints_matched)
            and source_evidence_fingerprint is not null and reproduction_evidence_fingerprint is not null
            and safe_differences is not null and failure_code is null and failure_message is null)
        or (status = 'FAILED' and failure_code ~ '^[A-Z][A-Z0-9_]*$' and failure_message <> '')
    )
);

create index reproduction_verification_recovery_idx
    on search.reproduction_verification(status, updated_at, verification_id)
    where status in ('PENDING', 'RUNNING');

create or replace function search.enforce_search_job_type()
returns trigger language plpgsql as $$
begin
    if not exists (
        select 1 from experiment.job
        where job_id = new.search_job_id
          and experiment_id = new.experiment_id
          and job_type = 'SEARCH'
    ) then
        raise exception 'SEARCH_RUN_JOB_TYPE_INVALID';
    end if;
    return new;
end;
$$;

create trigger search_run_job_type_guard
before insert or update of search_job_id, experiment_id on search.search_run
for each row execute function search.enforce_search_job_type();

revoke all on schema search from anon, authenticated;
revoke all on all tables in schema search from anon, authenticated;
revoke all on all functions in schema search from anon, authenticated;

commit;
