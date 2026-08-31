-- F-006 prerequisite integrity and immutable typed provenance.
-- Forward-only: abort rather than invent provenance or repair ambiguous result links.
begin;

alter table experiment.experiment_manifest
    add column dataset_provenance jsonb,
    add column strategy_provenance jsonb;

do $$
begin
    if exists (select 1 from experiment.experiment_manifest) then
        raise exception 'Cannot add typed provenance: existing manifests require an explicit audited backfill';
    end if;
end $$;

alter table experiment.experiment_manifest
    alter column dataset_provenance set not null,
    alter column strategy_provenance set not null;

alter table experiment.execution_attempt
    add constraint execution_attempt_id_candidate_unique unique (attempt_id, candidate_id);

alter table experiment.backtest_result add column experiment_id varchar(26);

update experiment.backtest_result result
set experiment_id = candidate.experiment_id
from experiment.candidate_definition candidate
where candidate.candidate_id = result.candidate_id;

do $$
begin
    if exists (
        select 1
        from experiment.backtest_result result
        left join experiment.execution_attempt attempt
          on attempt.attempt_id = result.successful_attempt_id
         and attempt.candidate_id = result.candidate_id
        where result.experiment_id is null or attempt.attempt_id is null
    ) then
        raise exception 'Cannot enforce Backtest Result integrity: Candidate/Attempt relationship is ambiguous';
    end if;
end $$;

alter table experiment.backtest_result
    alter column experiment_id set not null,
    add constraint backtest_result_candidate_experiment_fk
        foreign key (candidate_id, experiment_id)
        references experiment.candidate_definition(candidate_id, experiment_id),
    add constraint backtest_result_attempt_candidate_fk
        foreign key (successful_attempt_id, candidate_id)
        references experiment.execution_attempt(attempt_id, candidate_id),
    add constraint backtest_result_id_experiment_unique
        unique (backtest_result_id, experiment_id);

create index backtest_result_experiment_idx
    on experiment.backtest_result (experiment_id, completed_at desc);

alter table experiment.evaluation_result add column experiment_id varchar(26);

update experiment.evaluation_result evaluation
set experiment_id = result.experiment_id
from experiment.backtest_result result
where result.backtest_result_id = evaluation.backtest_result_id;

alter table experiment.evaluation_result
    alter column experiment_id set not null,
    add constraint evaluation_result_backtest_experiment_fk
        foreign key (backtest_result_id, experiment_id)
        references experiment.backtest_result(backtest_result_id, experiment_id),
    add constraint evaluation_result_id_experiment_unique
        unique (evaluation_result_id, experiment_id);

create index evaluation_result_experiment_score_idx
    on experiment.evaluation_result (experiment_id, overall_score desc);

alter table experiment.leaderboard_revision
    add constraint leaderboard_revision_id_experiment_unique
        unique (leaderboard_revision_id, experiment_id);

alter table experiment.leaderboard_entry add column experiment_id varchar(26);

update experiment.leaderboard_entry entry
set experiment_id = revision.experiment_id
from experiment.leaderboard_revision revision
where revision.leaderboard_revision_id = entry.leaderboard_revision_id;

do $$
begin
    if exists (
        select 1
        from experiment.leaderboard_entry entry
        left join experiment.evaluation_result evaluation
          on evaluation.evaluation_result_id = entry.evaluation_result_id
         and evaluation.experiment_id = entry.experiment_id
        where entry.experiment_id is null or evaluation.evaluation_result_id is null
    ) then
        raise exception 'Cannot enforce Leaderboard integrity: cross-Experiment Evaluation detected';
    end if;
end $$;

alter table experiment.leaderboard_entry
    alter column experiment_id set not null,
    add constraint leaderboard_entry_revision_experiment_fk
        foreign key (leaderboard_revision_id, experiment_id)
        references experiment.leaderboard_revision(leaderboard_revision_id, experiment_id),
    add constraint leaderboard_entry_evaluation_experiment_fk
        foreign key (evaluation_result_id, experiment_id)
        references experiment.evaluation_result(evaluation_result_id, experiment_id);

create index leaderboard_entry_experiment_rank_idx
    on experiment.leaderboard_entry (experiment_id, rank);

commit;
