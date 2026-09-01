-- F-006 deterministic Backtest, Evaluation and Leaderboard evidence.
-- Forward-only: never infer entry/exit fee from the legacy aggregate fee.

do $$
begin
    if exists (select 1 from experiment.trade limit 1) then
        raise exception 'F006_MIGRATION_BLOCKED: legacy Trade rows contain only total fee; entry_fee and exit_fee cannot be inferred safely';
    end if;
    if exists (select 1 from experiment.backtest_result limit 1) then
        raise exception 'F006_MIGRATION_BLOCKED: legacy Backtest Result rows lack typed assumptions/equity provenance';
    end if;
end $$;

alter table experiment.backtest_result
    add column job_id varchar(26) not null references experiment.job(job_id),
    add column manifest_fingerprint text not null check (manifest_fingerprint <> ''),
    add column dataset_fingerprint text not null check (dataset_fingerprint <> ''),
    add column strategy_fingerprint text not null check (strategy_fingerprint <> ''),
    add column assumptions_version text not null check (assumptions_version <> ''),
    add column assumptions_json jsonb not null,
    add column total_fees numeric(30,12) not null check (total_fees >= 0),
    add column equity_point_count bigint not null check (equity_point_count > 0),
    add column equity_peak numeric(30,12) not null check (equity_peak >= 0),
    add column equity_trough numeric(30,12) not null check (equity_trough >= 0),
    add column equity_peak_sequence bigint not null check (equity_peak_sequence >= 0),
    add column equity_trough_sequence bigint not null,
    add column equity_curve_fingerprint text not null check (equity_curve_fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    add constraint backtest_equity_sequence_check check (
        equity_trough_sequence >= equity_peak_sequence and equity_trough_sequence < equity_point_count
    );

alter table experiment.job
    add constraint job_id_candidate_experiment_unique unique (job_id, candidate_id, experiment_id);
alter table experiment.backtest_result
    add constraint backtest_result_job_lineage_fk
        foreign key (job_id, candidate_id, experiment_id)
        references experiment.job(job_id, candidate_id, experiment_id);

alter table experiment.trade
    add column entry_fee numeric(30,12) not null check (entry_fee >= 0),
    add column exit_fee numeric(30,12) not null check (exit_fee >= 0),
    add column post_trade_cash numeric(30,12) not null check (post_trade_cash >= 0),
    add column exit_reason text not null check (exit_reason in ('STRATEGY_SELL','FORCED_FINAL_CLOSE')),
    add constraint trade_fee_components_check check (fee = entry_fee + exit_fee);

alter table experiment.evaluation_result
    drop constraint evaluation_metric_version_unique,
    add column return_score numeric(20,10) not null check (return_score between 0 and 1),
    add column win_rate_score numeric(20,10) not null check (win_rate_score between 0 and 1),
    add column drawdown_score numeric(20,10) not null check (drawdown_score between 0 and 1),
    add column leaderboard_eligible boolean not null,
    add column evaluation_fingerprint text not null check (evaluation_fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    add constraint evaluation_metric_ranking_unique unique (backtest_result_id, metric_version, ranking_version),
    add constraint evaluation_score_range check (overall_score between 0 and 1);

alter table experiment.leaderboard_revision
    add column ranking_version text not null check (ranking_version <> ''),
    add column revision_fingerprint text not null check (revision_fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    add constraint leaderboard_revision_fingerprint_unique unique (experiment_id, ranking_version, revision_fingerprint);

alter table experiment.leaderboard_entry
    add column maximum_drawdown numeric(20,10) not null check (maximum_drawdown >= 0),
    add column evaluation_fingerprint text not null check (evaluation_fingerprint ~ '^sha256:[0-9a-f]{64}$');

create index backtest_result_job_attempt_idx on experiment.backtest_result(job_id, successful_attempt_id);
create index evaluation_experiment_ranking_idx on experiment.evaluation_result(experiment_id, ranking_version, overall_score desc);

create or replace function experiment.reject_f006_evidence_mutation()
returns trigger language plpgsql as $$
begin
    raise exception 'IMMUTABLE_EVIDENCE: % cannot be changed', tg_table_name;
end $$;

create trigger backtest_result_immutable before update or delete on experiment.backtest_result
for each row execute function experiment.reject_f006_evidence_mutation();
create trigger trade_immutable before update or delete on experiment.trade
for each row execute function experiment.reject_f006_evidence_mutation();
create trigger evaluation_result_immutable before update or delete on experiment.evaluation_result
for each row execute function experiment.reject_f006_evidence_mutation();
create trigger leaderboard_revision_immutable before update or delete on experiment.leaderboard_revision
for each row execute function experiment.reject_f006_evidence_mutation();
create trigger leaderboard_entry_immutable before update or delete on experiment.leaderboard_entry
for each row execute function experiment.reject_f006_evidence_mutation();
