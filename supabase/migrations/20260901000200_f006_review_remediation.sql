-- F-006 review remediation: frozen execution invariants and reproduction audit.

create table experiment.reproduction_verification (
    reproduction_experiment_id varchar(26) primary key references experiment.experiment(experiment_id),
    source_experiment_id varchar(26) not null references experiment.experiment(experiment_id),
    original_backtest_result_id varchar(26) not null references experiment.backtest_result(backtest_result_id),
    reproduced_backtest_result_id varchar(26) not null references experiment.backtest_result(backtest_result_id),
    original_evaluation_result_id varchar(26) not null references experiment.evaluation_result(evaluation_result_id),
    reproduced_evaluation_result_id varchar(26) not null references experiment.evaluation_result(evaluation_result_id),
    original_leaderboard_revision_id varchar(26) not null references experiment.leaderboard_revision(leaderboard_revision_id),
    reproduced_leaderboard_revision_id varchar(26) not null references experiment.leaderboard_revision(leaderboard_revision_id),
    outcome text not null check (outcome in ('MATCHED','MISMATCHED')),
    differences jsonb not null,
    manifest_fingerprint text not null,
    dataset_fingerprint text not null,
    strategy_fingerprint text not null,
    assumptions_fingerprint text not null,
    metric_fingerprint text not null,
    ranking_fingerprint text not null,
    verified_at timestamptz not null default now(),
    check (reproduction_experiment_id <> source_experiment_id)
);

create index reproduction_verification_source_idx
    on experiment.reproduction_verification(source_experiment_id, verified_at desc);

create trigger reproduction_verification_immutable
before update or delete on experiment.reproduction_verification
for each row execute function experiment.reject_f006_evidence_mutation();

create or replace function experiment.validate_backtest_result_lineage()
returns trigger language plpgsql as $$
declare
    valid_count integer;
begin
    select count(*) into valid_count
    from experiment.job j
    join experiment.execution_attempt a
      on a.job_id = j.job_id and a.candidate_id = j.candidate_id
    where j.job_id = new.job_id
      and j.experiment_id = new.experiment_id
      and j.candidate_id = new.candidate_id
      and j.job_type = 'BACKTEST'
      and a.attempt_id = new.successful_attempt_id
      and a.status = 'SUCCEEDED';
    if valid_count <> 1 then
        raise exception 'INVALID_BACKTEST_LINEAGE: Attempt must be SUCCEEDED and belong to the same BACKTEST Job/Candidate/Experiment';
    end if;
    return new;
end $$;

create trigger backtest_result_validate_lineage
before insert on experiment.backtest_result
for each row execute function experiment.validate_backtest_result_lineage();

create or replace function experiment.validate_leaderboard_entry()
returns trigger language plpgsql as $$
declare
    expected record;
    configured_top_k integer;
begin
    select top_k into configured_top_k
      from experiment.leaderboard_revision
     where leaderboard_revision_id = new.leaderboard_revision_id
       and experiment_id = new.experiment_id;
    if configured_top_k is null or new.rank < 1 or new.rank > configured_top_k then
        raise exception 'INVALID_LEADERBOARD_RANK: rank must be within Revision Top-K';
    end if;
    select overall_score, maximum_drawdown, evaluation_fingerprint, leaderboard_eligible
      into expected
      from experiment.evaluation_result
     where evaluation_result_id = new.evaluation_result_id
       and experiment_id = new.experiment_id;
    if expected is null or not expected.leaderboard_eligible
       or new.score is distinct from expected.overall_score
       or new.maximum_drawdown is distinct from expected.maximum_drawdown
       or new.evaluation_fingerprint is distinct from expected.evaluation_fingerprint then
        raise exception 'INVALID_LEADERBOARD_ENTRY: entry must exactly snapshot an eligible Evaluation';
    end if;
    return new;
end $$;

create trigger leaderboard_entry_validate_source
before insert on experiment.leaderboard_entry
for each row execute function experiment.validate_leaderboard_entry();

create or replace function experiment.validate_contiguous_leaderboard_ranks()
returns trigger language plpgsql as $$
declare
    entry_count integer;
    maximum_rank integer;
begin
    select count(*), coalesce(max(rank), 0) into entry_count, maximum_rank
      from experiment.leaderboard_entry
     where leaderboard_revision_id = new.leaderboard_revision_id;
    if entry_count <> maximum_rank then
        raise exception 'INVALID_LEADERBOARD_RANKS: ranks must be contiguous from 1';
    end if;
    return null;
end $$;

create constraint trigger leaderboard_entry_contiguous_ranks
after insert on experiment.leaderboard_entry
deferrable initially deferred
for each row execute function experiment.validate_contiguous_leaderboard_ranks();
