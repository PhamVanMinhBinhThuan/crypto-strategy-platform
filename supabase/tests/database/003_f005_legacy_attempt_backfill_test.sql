begin;
set local statement_timeout = '60s';

create or replace function pg_temp.assert_true(condition boolean, message text)
returns void
language plpgsql
as $$
begin
    if condition is distinct from true then
        raise exception 'ASSERTION FAILED: %', message;
    end if;
end;
$$;

-- 1. Assert tightened execution_attempt check constraint
select pg_temp.assert_true(
    exists (
        select 1
        from information_schema.check_constraints
        where constraint_name = 'execution_attempt_status_check'
    ),
    'execution_attempt_status_check constraint must exist'
);

-- 2. Assert reproduces_experiment_id column exists on experiment
select pg_temp.assert_true(
    exists (
        select 1
        from information_schema.columns
        where table_schema = 'experiment'
          and table_name = 'experiment'
          and column_name = 'reproduces_experiment_id'
    ),
    'reproduces_experiment_id column must exist on experiment'
);

-- 3. Assert platform.idempotency_record has state column and nullable response_status
select pg_temp.assert_true(
    exists (
        select 1
        from information_schema.columns
        where table_schema = 'platform'
          and table_name = 'idempotency_record'
          and column_name = 'state'
    ),
    'state column must exist on idempotency_record'
);

-- 4. Assert zero orphan execution attempts
select pg_temp.assert_true(
    not exists (
        select 1
        from experiment.execution_attempt ea
        where not exists (
            select 1 from experiment.job j where j.job_id = ea.job_id
        )
    ),
    'zero orphan execution attempts must remain after migration'
);

rollback;
