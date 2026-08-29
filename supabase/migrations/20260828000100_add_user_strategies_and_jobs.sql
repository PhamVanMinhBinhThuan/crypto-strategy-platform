begin;

create table strategy.user_strategy (
    user_strategy_id varchar(26) primary key
        check (user_strategy_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    owner_user_id uuid not null references auth.users(id),
    strategy_kind text not null check (strategy_kind in ('SINGLE', 'COMPOSITE')),
    name text not null check (btrim(name) <> ''),
    description text,
    status text not null default 'ACTIVE' check (status in ('ACTIVE', 'ARCHIVED')),
    archived_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint user_strategy_archive_state_valid check (
        (status = 'ACTIVE' and archived_at is null)
        or (status = 'ARCHIVED' and archived_at is not null)
    )
);
create unique index user_strategy_owner_active_name_unique
    on strategy.user_strategy (owner_user_id, lower(btrim(name)))
    where status = 'ACTIVE';
create index user_strategy_owner_status_updated_idx
    on strategy.user_strategy (owner_user_id, status, updated_at desc);

create table strategy.user_strategy_version (
    user_strategy_version_id varchar(26) primary key
        check (user_strategy_version_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    user_strategy_id varchar(26) not null
        references strategy.user_strategy(user_strategy_id),
    version_no integer not null check (version_no > 0),
    strategy_kind text not null check (strategy_kind in ('SINGLE', 'COMPOSITE')),
    strategy_version_id varchar(26)
        references strategy.strategy_version(strategy_version_id),
    parameters jsonb not null default '{}'::jsonb,
    policy_id text,
    policy_version text,
    policy_parameters jsonb,
    lifecycle_status text not null default 'DRAFT'
        check (lifecycle_status in ('DRAFT', 'PUBLISHED')),
    fingerprint text not null check (fingerprint <> ''),
    published_at timestamptz,
    created_at timestamptz not null default now(),
    constraint user_strategy_version_number_unique
        unique (user_strategy_id, version_no),
    constraint user_strategy_version_fingerprint_unique
        unique (user_strategy_id, fingerprint),
    constraint user_strategy_version_source_valid check (
        (
            strategy_kind = 'SINGLE'
            and strategy_version_id is not null
            and policy_id is null
            and policy_version is null
            and policy_parameters is null
        )
        or
        (
            strategy_kind = 'COMPOSITE'
            and strategy_version_id is null
            and policy_id is not null and btrim(policy_id) <> ''
            and policy_version is not null and btrim(policy_version) <> ''
            and policy_parameters is not null
        )
    ),
    constraint user_strategy_version_publish_state_valid check (
        (lifecycle_status = 'DRAFT' and published_at is null)
        or (lifecycle_status = 'PUBLISHED' and published_at is not null)
    )
);
create index user_strategy_version_strategy_created_idx
    on strategy.user_strategy_version (user_strategy_id, version_no desc);

create table strategy.user_strategy_component (
    user_strategy_version_id varchar(26) not null
        references strategy.user_strategy_version(user_strategy_version_id),
    position integer not null check (position >= 0),
    strategy_version_id varchar(26) not null
        references strategy.strategy_version(strategy_version_id),
    parameters jsonb not null default '{}'::jsonb,
    weight numeric(20,10) check (weight is null or weight > 0),
    primary key (user_strategy_version_id, position),
    constraint user_strategy_component_plugin_unique
        unique (user_strategy_version_id, strategy_version_id)
);

create function strategy.enforce_user_strategy_root_transition()
returns trigger
language plpgsql
as $$
begin
    if old.owner_user_id <> new.owner_user_id then
        raise exception 'User Strategy owner is immutable';
    end if;

    if old.status = 'ARCHIVED' and new.status <> 'ARCHIVED' then
        raise exception 'Archived User Strategy cannot be reactivated';
    end if;

    if old.strategy_kind <> new.strategy_kind
       and exists (
           select 1
           from strategy.user_strategy_version v
           where v.user_strategy_id = old.user_strategy_id
       ) then
        raise exception 'User Strategy kind cannot change after a version exists';
    end if;

    return new;
end;
$$;

create trigger user_strategy_root_transition_guard
before update on strategy.user_strategy
for each row execute function strategy.enforce_user_strategy_root_transition();

create function strategy.enforce_user_strategy_version_lifecycle()
returns trigger
language plpgsql
as $$
declare
    parent_kind text;
    parent_status text;
    component_count integer;
begin
    if tg_op = 'DELETE' then
        if old.lifecycle_status = 'PUBLISHED' then
            raise exception 'Published User Strategy version is immutable';
        end if;
        return old;
    end if;

    if tg_op = 'UPDATE' and old.lifecycle_status = 'PUBLISHED' then
        raise exception 'Published User Strategy version is immutable';
    end if;

    select s.strategy_kind, s.status
      into parent_kind, parent_status
      from strategy.user_strategy s
     where s.user_strategy_id = new.user_strategy_id;

    if parent_kind is distinct from new.strategy_kind then
        raise exception 'User Strategy version kind must match its parent';
    end if;

    if parent_status = 'ARCHIVED' then
        raise exception 'Archived User Strategy cannot receive a version';
    end if;

    if new.lifecycle_status = 'PUBLISHED'
       and (tg_op = 'INSERT' or old.lifecycle_status = 'DRAFT') then
        select count(*)
          into component_count
          from strategy.user_strategy_component c
         where c.user_strategy_version_id = new.user_strategy_version_id;

        if new.strategy_kind = 'SINGLE' and component_count <> 0 then
            raise exception 'Single User Strategy version cannot have components';
        end if;

        if new.strategy_kind = 'COMPOSITE' and component_count < 2 then
            raise exception 'Composite User Strategy version needs at least two components';
        end if;
    end if;

    return new;
end;
$$;

create trigger user_strategy_version_lifecycle_guard
before insert or update or delete on strategy.user_strategy_version
for each row execute function strategy.enforce_user_strategy_version_lifecycle();

create function strategy.enforce_user_strategy_component_lifecycle()
returns trigger
language plpgsql
as $$
declare
    version_id varchar(26);
    version_kind text;
    version_status text;
begin
    version_id := case when tg_op = 'DELETE'
        then old.user_strategy_version_id
        else new.user_strategy_version_id
    end;

    select v.strategy_kind, v.lifecycle_status
      into version_kind, version_status
      from strategy.user_strategy_version v
     where v.user_strategy_version_id = version_id;

    if version_kind <> 'COMPOSITE' then
        raise exception 'Only composite User Strategy versions can have components';
    end if;

    if version_status = 'PUBLISHED' then
        raise exception 'Published User Strategy components are immutable';
    end if;

    return case when tg_op = 'DELETE' then old else new end;
end;
$$;

create trigger user_strategy_component_lifecycle_guard
before insert or update or delete on strategy.user_strategy_component
for each row execute function strategy.enforce_user_strategy_component_lifecycle();

alter table experiment.experiment_manifest
    add column source_user_strategy_version_id varchar(26)
        references strategy.user_strategy_version(user_strategy_version_id);
create index experiment_manifest_user_strategy_version_idx
    on experiment.experiment_manifest (source_user_strategy_version_id)
    where source_user_strategy_version_id is not null;

create function experiment.enforce_manifest_user_strategy_owner()
returns trigger
language plpgsql
as $$
declare
    experiment_owner uuid;
    strategy_owner uuid;
    version_status text;
begin
    if new.source_user_strategy_version_id is null then
        return new;
    end if;

    select e.owner_user_id
      into experiment_owner
      from experiment.experiment e
     where e.experiment_id = new.experiment_id;

    select s.owner_user_id, v.lifecycle_status
      into strategy_owner, version_status
      from strategy.user_strategy_version v
      join strategy.user_strategy s
        on s.user_strategy_id = v.user_strategy_id
     where v.user_strategy_version_id = new.source_user_strategy_version_id;

    if version_status <> 'PUBLISHED' then
        raise exception 'Experiment Manifest must reference a published User Strategy version';
    end if;

    if experiment_owner is distinct from strategy_owner then
        raise exception 'Experiment and User Strategy must have the same owner';
    end if;

    return new;
end;
$$;

create trigger experiment_manifest_user_strategy_owner_guard
before insert or update of source_user_strategy_version_id
on experiment.experiment_manifest
for each row execute function experiment.enforce_manifest_user_strategy_owner();

alter table experiment.candidate_definition
    add constraint candidate_id_experiment_unique
        unique (candidate_id, experiment_id);

create table experiment.job (
    job_id varchar(26) primary key
        check (job_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    experiment_id varchar(26) not null
        references experiment.experiment(experiment_id),
    candidate_id varchar(26),
    job_type text not null check (job_type in ('SEARCH', 'BACKTEST')),
    status text not null check (status in (
        'QUEUED', 'RUNNING', 'RETRY_SCHEDULED', 'SUCCEEDED', 'FAILED',
        'CANCEL_REQUESTED', 'CANCELLED'
    )),
    correlation_id varchar(26) not null
        check (correlation_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    total_work integer not null check (total_work > 0),
    completed_work integer not null default 0 check (completed_work >= 0),
    failed_work integer not null default 0 check (failed_work >= 0),
    best_score numeric(20,10),
    queued_at timestamptz,
    started_at timestamptz,
    finished_at timestamptz,
    next_retry_at timestamptz,
    failure_code text,
    failure_message text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint job_candidate_experiment_fk
        foreign key (candidate_id, experiment_id)
        references experiment.candidate_definition(candidate_id, experiment_id),
    constraint job_type_candidate_valid check (
        (job_type = 'SEARCH' and candidate_id is null)
        or (job_type = 'BACKTEST' and candidate_id is not null)
    ),
    constraint job_progress_valid check (
        completed_work + failed_work <= total_work
    ),
    constraint job_identity_candidate_unique unique (job_id, candidate_id)
);
create unique index job_search_experiment_unique
    on experiment.job (experiment_id)
    where job_type = 'SEARCH';
create unique index job_backtest_candidate_unique
    on experiment.job (candidate_id)
    where job_type = 'BACKTEST';
create index job_experiment_created_idx
    on experiment.job (experiment_id, created_at desc);
create index job_recovery_idx
    on experiment.job (status, next_retry_at, created_at)
    where status in ('QUEUED', 'RUNNING', 'RETRY_SCHEDULED', 'CANCEL_REQUESTED');

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

with legacy_job as (
    select
        ea.job_id,
        min(ea.candidate_id) as candidate_id,
        min(cd.experiment_id) as experiment_id,
        (array_agg(ea.status order by ea.attempt_no desc))[1] as latest_status,
        bool_or(ea.status = 'SUCCEEDED') as has_succeeded,
        min(ea.created_at) as created_at,
        min(ea.started_at) as started_at,
        max(ea.finished_at) as finished_at,
        max(ea.next_retry_at) as next_retry_at
    from experiment.execution_attempt ea
    join experiment.candidate_definition cd
      on cd.candidate_id = ea.candidate_id
    group by ea.job_id
)
insert into experiment.job (
    job_id, experiment_id, candidate_id, job_type, status, correlation_id,
    total_work, completed_work, failed_work, queued_at, started_at, finished_at,
    next_retry_at, created_at, updated_at
)
select
    job_id,
    experiment_id,
    candidate_id,
    'BACKTEST',
    case
        when has_succeeded then 'SUCCEEDED'
        when latest_status in ('QUEUED', 'RUNNING', 'RETRY_SCHEDULED', 'FAILED', 'CANCELLED')
            then latest_status
        else 'FAILED'
    end,
    job_id,
    1,
    case when has_succeeded then 1 else 0 end,
    case when not has_succeeded and latest_status in ('FAILED', 'CANCELLED') then 1 else 0 end,
    created_at,
    started_at,
    finished_at,
    next_retry_at,
    created_at,
    greatest(created_at, coalesce(finished_at, started_at, created_at))
from legacy_job;

alter table experiment.execution_attempt
    add constraint execution_attempt_job_candidate_fk
        foreign key (job_id, candidate_id)
        references experiment.job(job_id, candidate_id);

comment on table strategy.user_strategy is
    'Private user-owned Strategy aggregate; credentials remain in Supabase Auth.';
comment on table strategy.user_strategy_version is
    'Versioned user Strategy configuration; published rows are immutable.';
comment on table strategy.user_strategy_component is
    'Ordered plugin components of a composite user Strategy version.';
comment on table experiment.job is
    'Durable Search/Backtest work identity; execution_attempt stores Worker retries.';
comment on column experiment.experiment_manifest.source_user_strategy_version_id is
    'Optional provenance link; frozen manifest fields remain authoritative for reproduction.';

revoke all on table
    strategy.user_strategy,
    strategy.user_strategy_version,
    strategy.user_strategy_component,
    experiment.job
from anon, authenticated;

commit;
