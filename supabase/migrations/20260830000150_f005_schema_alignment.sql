-- Migration: 20260830000150_f005_schema_alignment.sql
-- Description: F-005 schema alignment:
-- (a) Tighten execution_attempt.status to remove RETRY_SCHEDULED;
-- (b) Add reproduces_experiment_id self-reference to experiment.experiment;
-- (c) Make experiment_manifest.fingerprint nullable for CREATED phase;
-- (d) Align platform.idempotency_record for atomic claim lifecycle (state IN ('IN_PROGRESS','COMPLETED')).

begin;

-- (a) Tighten execution_attempt.status check constraint
alter table experiment.execution_attempt
    drop constraint if exists execution_attempt_status_check;

alter table experiment.execution_attempt
    add constraint execution_attempt_status_check
    check (status in ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'));

-- (b) Add reproduces_experiment_id self-reference to experiment.experiment
alter table experiment.experiment
    add column if not exists reproduces_experiment_id varchar(26)
    references experiment.experiment(experiment_id);

create index if not exists experiment_reproduces_idx
    on experiment.experiment (reproduces_experiment_id)
    where reproduces_experiment_id is not null;

-- (c) Make experiment_manifest.fingerprint nullable during CREATED status
alter table experiment.experiment_manifest
    alter column fingerprint drop not null;

alter table experiment.experiment_manifest
    drop constraint if exists experiment_manifest_fingerprint_check;

alter table experiment.experiment_manifest
    add constraint experiment_manifest_fingerprint_check
    check (fingerprint is null or fingerprint <> '');

-- (d) Align platform.idempotency_record for atomic claim lifecycle
alter table platform.idempotency_record
    add column if not exists state text not null default 'COMPLETED'
    check (state in ('IN_PROGRESS', 'COMPLETED'));

alter table platform.idempotency_record
    alter column response_status drop not null;

alter table platform.idempotency_record
    alter column response_body drop not null;

commit;
