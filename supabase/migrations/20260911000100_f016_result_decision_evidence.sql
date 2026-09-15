-- F016: retain authoritative Sentiment decision evidence with immutable Backtest Results.

begin;

alter table experiment.backtest_result
    add column strategy_decision_evidence jsonb not null default '[]'::jsonb;

alter table experiment.backtest_result
    add constraint backtest_result_decision_evidence_array
    check (jsonb_typeof(strategy_decision_evidence) = 'array');

commit;
