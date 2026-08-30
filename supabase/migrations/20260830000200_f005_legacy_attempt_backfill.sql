-- Migration: 20260830000200_f005_legacy_attempt_backfill.sql
-- Description: FR-028: Deterministic legacy execution_attempt -> job backfill.
-- Aborts if ambiguous mappings are detected; ensures zero orphan attempts remain.

begin;

-- 1. Assert no ambiguous candidate mappings
do $$
begin
    if exists (
        select ea.job_id
        from experiment.execution_attempt ea
        group by ea.job_id
        having count(distinct ea.candidate_id) > 1
    ) then
        raise exception 'Cannot migrate: a legacy job_id points to multiple candidates';
    end if;

    if exists (
        select ea.candidate_id
        from experiment.execution_attempt ea
        group by ea.candidate_id
        having count(distinct ea.job_id) > 1
    ) then
        raise exception 'Cannot migrate: a legacy candidate points to multiple job_ids';
    end if;
end;
$$;

-- 2. Backfill any execution_attempt rows that do not have a parent job row
insert into experiment.job (
    job_id, experiment_id, candidate_id, job_type, status, correlation_id,
    total_work, completed_work, failed_work, queued_at, started_at, finished_at,
    next_retry_at, created_at, updated_at
)
select
    ea.job_id,
    min(cd.experiment_id) as experiment_id,
    min(ea.candidate_id) as candidate_id,
    'BACKTEST' as job_type,
    case
        when bool_or(ea.status = 'SUCCEEDED') then 'SUCCEEDED'
        when (array_agg(ea.status order by ea.attempt_no desc))[1] in ('QUEUED', 'RUNNING', 'FAILED', 'CANCELLED')
            then (array_agg(ea.status order by ea.attempt_no desc))[1]
        else 'FAILED'
    end as status,
    ea.job_id as correlation_id,
    1 as total_work,
    case when bool_or(ea.status = 'SUCCEEDED') then 1 else 0 end as completed_work,
    case when not bool_or(ea.status = 'SUCCEEDED') and (array_agg(ea.status order by ea.attempt_no desc))[1] in ('FAILED', 'CANCELLED') then 1 else 0 end as failed_work,
    min(ea.created_at) as queued_at,
    min(ea.started_at) as started_at,
    max(ea.finished_at) as finished_at,
    max(ea.next_retry_at) as next_retry_at,
    min(ea.created_at) as created_at,
    greatest(min(ea.created_at), coalesce(max(ea.finished_at), min(ea.started_at), min(ea.created_at))) as updated_at
from experiment.execution_attempt ea
join experiment.candidate_definition cd on cd.candidate_id = ea.candidate_id
where not exists (
    select 1 from experiment.job j where j.job_id = ea.job_id
)
group by ea.job_id;

-- 3. Assert zero orphan attempts remain
do $$
declare
    orphan_count integer;
begin
    select count(*)
    into orphan_count
    from experiment.execution_attempt ea
    where not exists (
        select 1 from experiment.job j where j.job_id = ea.job_id
    );

    if orphan_count > 0 then
        raise exception 'Migration failed: % orphan execution attempts remain', orphan_count;
    end if;
end;
$$;

commit;
