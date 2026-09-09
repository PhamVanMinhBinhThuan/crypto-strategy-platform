-- Migration: 20260905000100_f015_composite_search.sql
-- Description: Add forward-compatible v2 Search/candidate definition discriminators and refill indexes.

begin;

create table market.dataset_access (
    owner_user_id uuid not null references auth.users(id) on delete cascade,
    dataset_version_id varchar(26) not null references market.dataset_version(dataset_version_id),
    granted_at timestamptz not null default now(),
    primary key (owner_user_id, dataset_version_id)
);

create index dataset_access_owner_recent_idx
    on market.dataset_access(owner_user_id, granted_at desc, dataset_version_id);

revoke all on market.dataset_access from anon, authenticated;

alter table search.search_run
    add column maximum_without_improvement integer
        check (maximum_without_improvement is null or maximum_without_improvement > 0),
    add column terminal_reason text
        check (terminal_reason is null or terminal_reason in (
            'MAXIMUM_CANDIDATES', 'SEARCH_SPACE_EXHAUSTED', 'MAXIMUM_DURATION',
            'NO_IMPROVEMENT', 'EXPLICIT_STOP', 'TERMINAL_FAILURE'
        ));

alter table experiment.experiment_manifest
    add column search_config_contract_version text
        generated always as (coalesce(search_config ->> 'contractVersion', 'search-config-v1')) stored,
    add constraint experiment_manifest_search_config_version_check
        check (search_config_contract_version in ('search-config-v1', 'search-config-v2'));

alter table experiment.candidate_definition
    add column definition_schema_version smallint
        generated always as (coalesce((definition ->> 'schemaVersion')::smallint, 1)) stored,
    add constraint candidate_definition_schema_version_check
        check (definition_schema_version in (1, 2)),
    add constraint candidate_definition_v2_shape_check check (
        definition_schema_version <> 2
        or (
            definition ->> 'kind' = 'COMPOSITE'
            and jsonb_typeof(definition -> 'components') = 'array'
            and jsonb_array_length(definition -> 'components') > 0
            and jsonb_typeof(definition -> 'combinationPolicy') = 'object'
        )
    );

create index experiment_manifest_search_config_version_idx
    on experiment.experiment_manifest(search_config_contract_version, created_at);

create index candidate_definition_v2_experiment_generation_idx
    on experiment.candidate_definition(experiment_id, generation_index, candidate_id)
    where definition_schema_version = 2;

create index search_run_refill_idx
    on search.search_run(status, deadline_at, updated_at, search_run_id)
    where status in ('PENDING', 'RUNNING');

commit;
