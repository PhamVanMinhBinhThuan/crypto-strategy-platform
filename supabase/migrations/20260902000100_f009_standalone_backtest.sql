-- Migration: 20260902000100_f009_standalone_backtest.sql
-- Description: F-009/F-005 standalone Backtest identity and single-run graph invariant.

begin;

create table experiment.standalone_backtest (
    backtest_id varchar(26) primary key
        check (backtest_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    experiment_id varchar(26) not null unique
        references experiment.experiment(experiment_id),
    candidate_id varchar(26) not null unique,
    job_id varchar(26) not null unique,
    created_at timestamptz not null default now(),
    constraint standalone_backtest_candidate_experiment_fk
        foreign key (candidate_id, experiment_id)
        references experiment.candidate_definition(candidate_id, experiment_id),
    constraint standalone_backtest_job_candidate_fk
        foreign key (job_id, candidate_id)
        references experiment.job(job_id, candidate_id),
    constraint standalone_backtest_public_identity_distinct check (
        backtest_id <> experiment_id
        and backtest_id <> candidate_id
        and backtest_id <> job_id
    )
);

comment on table experiment.standalone_backtest is
    'F-005 owner-scoped standalone Backtest mapped to one immutable single-run Experiment, Candidate, and Job.';

revoke all on table experiment.standalone_backtest from anon, authenticated;

-- F-009 correlation identifiers are opaque bounded values, not necessarily ULIDs.
alter table experiment.job
    drop constraint if exists job_correlation_id_check;

alter table experiment.job
    add constraint job_correlation_id_check
    check (correlation_id <> '' and length(correlation_id) <= 128);

commit;
