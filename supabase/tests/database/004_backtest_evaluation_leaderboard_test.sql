begin;
set local search_path = experiment, public;

create or replace function pg_temp.assert_true(condition boolean, message text)
returns void language plpgsql as $$ begin if not condition then raise exception 'ASSERTION FAILED: %', message; end if; end $$;

select pg_temp.assert_true(
    (select count(*) = 2 from information_schema.columns where table_schema='experiment' and table_name='trade' and column_name in ('entry_fee','exit_fee')),
    'Trade must store entry and exit fees explicitly');
select pg_temp.assert_true(
    (select count(*) = 4 from information_schema.columns where table_schema='experiment' and table_name='trade'
      and column_name in ('entry_fee','exit_fee','post_trade_cash','exit_reason') and is_nullable='NO'),
    'Trade execution evidence must be complete and non-null');
select pg_temp.assert_true(
    exists(select 1 from pg_constraint where conname='trade_fee_components_check'),
    'total fee must equal entry plus exit fee');
select pg_temp.assert_true(
    exists(select 1 from information_schema.columns where table_schema='experiment' and table_name='backtest_result' and column_name='equity_curve_fingerprint'),
    'Backtest Result must persist equity digest');
select pg_temp.assert_true(
    (select count(*) = 13 from information_schema.columns where table_schema='experiment' and table_name='backtest_result'
      and column_name in ('job_id','manifest_fingerprint','dataset_fingerprint','strategy_fingerprint',
      'assumptions_version','assumptions_json','total_fees','equity_point_count','equity_peak','equity_trough',
      'equity_peak_sequence','equity_trough_sequence','equity_curve_fingerprint') and is_nullable='NO'),
    'Backtest Result must persist complete typed lineage, assumptions and streaming equity evidence');
select pg_temp.assert_true(
    (select count(*) = 4 from pg_constraint where conname in (
      'backtest_result_candidate_experiment_fk','backtest_result_attempt_candidate_fk',
      'backtest_result_job_lineage_fk','evaluation_result_backtest_experiment_fk')),
    'Result and Evaluation must reject cross Candidate/Attempt/Job/Experiment lineage');
select pg_temp.assert_true(
    exists(select 1 from pg_constraint where conname='evaluation_metric_ranking_unique'),
    'Evaluation identity must include metric and ranking versions');
select pg_temp.assert_true(
    (select count(*) = 5 from information_schema.columns where table_schema='experiment' and table_name='evaluation_result'
      and column_name in ('return_score','win_rate_score','drawdown_score','leaderboard_eligible','evaluation_fingerprint')
      and is_nullable='NO'),
    'Evaluation must persist normalized scoring, eligibility and fingerprint evidence');
select pg_temp.assert_true(
    (select count(*) = 2 from pg_constraint where conname in (
      'leaderboard_entry_revision_experiment_fk','leaderboard_entry_evaluation_experiment_fk')),
    'Leaderboard entries must use Revision and Evaluation from the same Experiment');
select pg_temp.assert_true(
    exists(select 1 from pg_constraint where conname='leaderboard_revision_fingerprint_unique'),
    'unchanged Leaderboard content must be idempotent per ranking version');
select pg_temp.assert_true(
    (select count(*) = 2 from pg_indexes where schemaname='experiment' and indexname in (
      'backtest_result_job_attempt_idx','evaluation_experiment_ranking_idx')),
    'F-006 lineage and projection indexes must exist');
select pg_temp.assert_true(
    (select count(*) = 5 from pg_trigger where not tgisinternal and tgname in ('backtest_result_immutable','trade_immutable','evaluation_result_immutable','leaderboard_revision_immutable','leaderboard_entry_immutable')),
    'all accepted F-006 evidence must be immutable');

rollback;
