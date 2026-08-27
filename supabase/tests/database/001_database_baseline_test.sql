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

-- V-01: ownership schemas and baseline tables.
select pg_temp.assert_true(
    (select count(*) = 5 from information_schema.schemata
     where schema_name in ('market', 'strategy', 'experiment', 'news', 'platform')),
    'five ownership schemas must exist'
);

select pg_temp.assert_true(
    (select count(*) = 24 from information_schema.tables
     where table_schema in ('market', 'strategy', 'experiment', 'news', 'platform')
       and table_type = 'BASE TABLE'),
    'twenty-four baseline tables must exist'
);

select pg_temp.assert_true(
    (select count(*) = 27
     from pg_constraint c
     join pg_namespace n on n.oid = c.connamespace
     where c.contype = 'f'
       and n.nspname in ('market', 'strategy', 'experiment', 'news', 'platform')),
    'all twenty-seven baseline foreign keys must exist'
);

select pg_temp.assert_true(
    (select count(*) = 2
     from pg_constraint c
     join pg_namespace n on n.oid = c.connamespace
     where c.contype = 'c'
       and n.nspname = 'market'
       and c.conrelid in ('market.candle'::regclass, 'market.dataset_version'::regclass)
       and position('1m' in pg_get_constraintdef(c.oid)) > 0
       and position('5m' in pg_get_constraintdef(c.oid)) > 0
       and position('15m' in pg_get_constraintdef(c.oid)) > 0
       and position('30m' in pg_get_constraintdef(c.oid)) > 0
       and position('1h' in pg_get_constraintdef(c.oid)) > 0
       and position('2h' in pg_get_constraintdef(c.oid)) > 0
       and position('4h' in pg_get_constraintdef(c.oid)) > 0
       and position('1d' in pg_get_constraintdef(c.oid)) > 0),
    'Candle and Dataset must enforce all eight canonical timeframes'
);

select pg_temp.assert_true(
    not exists (
        select 1
        from information_schema.columns
        where table_schema in ('market', 'strategy', 'experiment', 'news', 'platform')
          and (column_name like '%\_at' escape '\'
               or column_name like '%\_time' escape '\'
               or column_name in ('range_start', 'range_end'))
          and data_type <> 'timestamp with time zone'
    ),
    'all business instants must use timestamptz'
);

-- V-10: exact decimal semantics and critical indexes.
select pg_temp.assert_true(
    not exists (
        select 1
        from information_schema.columns
        where (table_schema, table_name, column_name) in (
            ('market', 'candle', 'open'), ('market', 'candle', 'high'),
            ('market', 'candle', 'low'), ('market', 'candle', 'close'),
            ('market', 'candle', 'volume'),
            ('experiment', 'trade', 'entry_price'),
            ('experiment', 'trade', 'exit_price'),
            ('experiment', 'trade', 'quantity'), ('experiment', 'trade', 'fee')
        ) and (data_type <> 'numeric' or numeric_precision <> 30 or numeric_scale <> 12)
    ),
    'price, quantity and fee columns must use numeric(30,12)'
);

select pg_temp.assert_true(
    not exists (
        select 1
        from information_schema.columns
        where (table_schema, table_name, column_name) in (
            ('news', 'sentiment_result', 'confidence'),
            ('news', 'sentiment_result', 'polarity_score'),
            ('experiment', 'evaluation_result', 'overall_score')
        ) and (data_type <> 'numeric' or numeric_precision <> 20 or numeric_scale <> 10)
    ),
    'confidence, polarity and metrics must use numeric(20,10)'
);

select pg_temp.assert_true(
    (select count(*) = 5 from pg_indexes where indexname in (
        'candle_range_idx', 'execution_attempt_recovery_idx',
        'outbox_unpublished_idx', 'processed_message_expiry_idx',
        'idempotency_expiry_idx')),
    'critical access and recovery indexes must exist'
);

-- V-11: browser-facing roles have no direct business access.
select pg_temp.assert_true(
    not exists (
        select 1
        from (values ('anon'::name), ('authenticated'::name)) as roles(role_name)
        cross join (values ('market'::name), ('strategy'::name), ('experiment'::name),
                           ('news'::name), ('platform'::name)) as schemas(schema_name)
        where has_schema_privilege(role_name, schema_name, 'USAGE')
           or has_schema_privilege(role_name, schema_name, 'CREATE')
    ),
    'anon and authenticated must not have business schema privileges'
);

select pg_temp.assert_true(
    not exists (
        select 1
        from information_schema.role_table_grants
        where grantee in ('anon', 'authenticated')
          and table_schema in ('market', 'strategy', 'experiment', 'news', 'platform')
    ),
    'anon and authenticated must not have direct business table grants'
);

-- Deterministic fixtures. All changes are rolled back at the end.
insert into auth.users (id) values
    ('00000000-0000-4000-8000-000000000001'),
    ('00000000-0000-4000-8000-000000000002');

insert into market.asset (asset_id, symbol, name) values
    ('00000000000000000000000001', 'BTC', 'Bitcoin'),
    ('00000000000000000000000002', 'USDT', 'Tether');

insert into market.trading_pair
    (trading_pair_id, base_asset_id, quote_asset_id, symbol)
values
    ('00000000000000000000000003', '00000000000000000000000001',
     '00000000000000000000000002', 'BTCUSDT');

insert into market.candle
    (candle_id, provider, trading_pair_id, timeframe, open_time, close_time,
     open, high, low, close, volume)
values
    ('00000000000000000000000004', 'binance', '00000000000000000000000003',
     '1h', '2026-01-01T00:00:00Z', '2026-01-01T01:00:00Z',
     100, 120, 90, 110, 10);

insert into market.dataset_version
    (dataset_version_id, version, provider, trading_pair_id, timeframe,
     normalization_version, range_start, range_end, candle_count, checksum)
values
    ('00000000000000000000000005', 'v1', 'binance',
     '00000000000000000000000003', '1h', 'v1',
     '2026-01-01T00:00:00Z', '2026-01-01T01:00:00Z', 1,
     'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa');

insert into market.dataset_candle (dataset_version_id, sequence_no, candle_id)
values ('00000000000000000000000005', 0, '00000000000000000000000004');

insert into strategy.strategy_version
    (strategy_version_id, plugin_id, version, display_name, parameter_schema,
     default_parameters, supported_signals, fingerprint)
values
    ('00000000000000000000000006', 'sma-cross', '1.0.0', 'SMA Cross',
     '{}'::jsonb, '{}'::jsonb, '["BUY","SELL","HOLD"]'::jsonb, 'strategy-fp-1'),
    ('00000000000000000000000013', 'rsi', '1.0.0', 'RSI',
     '{}'::jsonb, '{}'::jsonb, '["BUY","SELL","HOLD"]'::jsonb, 'strategy-fp-2');

insert into strategy.composite_version
    (composite_version_id, composite_id, version, display_name, policy_id,
     policy_version, policy_parameters, fingerprint)
values
    ('00000000000000000000000014', 'sma-rsi', '1.0.0', 'SMA and RSI',
     'weighted-vote', '1.0.0', '{}', 'composite-fp-1');

insert into strategy.composite_component
    (composite_version_id, position, strategy_version_id, parameter_overrides, weight)
values
    ('00000000000000000000000014', 0, '00000000000000000000000006', '{}', 1),
    ('00000000000000000000000014', 1, '00000000000000000000000013', '{}', 1);

insert into experiment.experiment (experiment_id, owner_user_id, name, status)
values
    ('00000000000000000000000007', '00000000-0000-4000-8000-000000000001',
     'baseline fixture', 'CREATED'),
    ('00000000000000000000000008', '00000000-0000-4000-8000-000000000002',
     'reproduction fixture', 'CREATED');

insert into experiment.experiment_manifest
    (experiment_id, manifest_version, dataset_version_id, strategy_kind,
     strategy_ref_id, strategy_version, strategy_parameters, backtest_config,
     search_config, evaluation_config, software_version, git_commit, fingerprint)
values
    ('00000000000000000000000007', 'v1', '00000000000000000000000005',
     'SINGLE', 'sma-cross', '1.0.0', '{}', '{}', '{}', '{}', '1.0.0', 'abc123', 'same-manifest'),
    ('00000000000000000000000008', 'v1', '00000000000000000000000005',
     'SINGLE', 'sma-cross', '1.0.0', '{}', '{}', '{}', '{}', '1.0.0', 'abc123', 'same-manifest');

insert into experiment.candidate_definition
    (candidate_id, experiment_id, generation_index, definition, fingerprint)
values
    ('00000000000000000000000009', '00000000000000000000000007', 0, '{}', 'candidate-fp-1'),
    ('0000000000000000000000000A', '00000000000000000000000008', 0, '{}', 'candidate-fp-2');

insert into experiment.execution_attempt
    (attempt_id, job_id, candidate_id, attempt_no, status)
values
    ('0000000000000000000000000B', '0000000000000000000000000C',
     '00000000000000000000000009', 1, 'SUCCEEDED'),
    ('0000000000000000000000000D', '0000000000000000000000000E',
     '0000000000000000000000000A', 1, 'SUCCEEDED');

insert into experiment.backtest_result
    (backtest_result_id, candidate_id, successful_attempt_id, initial_capital,
     final_capital, result_fingerprint, completed_at, reproduces_result_id)
values
    ('0000000000000000000000000F', '00000000000000000000000009',
     '0000000000000000000000000B', 1000, 1100, 'result-fp-1', '2026-01-02T00:00:00Z', null),
    ('0000000000000000000000000G', '0000000000000000000000000A',
     '0000000000000000000000000D', 1000, 1100, 'result-fp-2', '2026-01-02T00:00:00Z',
     '0000000000000000000000000F');

insert into experiment.trade
    (trade_id, backtest_result_id, sequence_no, side, entry_time, exit_time,
     entry_price, exit_price, quantity, fee, profit_loss)
values
    ('0000000000000000000000000H', '0000000000000000000000000F', 0, 'BUY',
     '2026-01-01T00:00:00Z', '2026-01-01T01:00:00Z', 100, 110, 1, 0.1, 9.9);

insert into experiment.evaluation_result
    (evaluation_result_id, backtest_result_id, metric_version, ranking_version,
     total_return, win_rate, maximum_drawdown, number_of_trades, overall_score, evaluated_at)
values
    ('0000000000000000000000000J', '0000000000000000000000000F',
     'metrics-v1', 'ranking-v1', 0.1, 1, 0, 1, 0.9, '2026-01-02T00:00:00Z');

insert into experiment.leaderboard_revision
    (leaderboard_revision_id, experiment_id, revision_no, top_k)
values ('0000000000000000000000000K', '00000000000000000000000007', 1, 10);

insert into experiment.leaderboard_entry
    (leaderboard_revision_id, rank, evaluation_result_id, score)
values ('0000000000000000000000000K', 1, '0000000000000000000000000J', 0.9);

insert into news.news_item
    (news_item_id, source, source_item_id, url, title, content_hash,
     published_at, crawled_at, analysis_status)
values
    ('0000000000000000000000000M', 'reuters', 'item-1',
     'https://example.test/news/1', 'Fixture news', 'content-v1',
     '2026-01-01T00:00:00Z', '2026-01-01T00:01:00Z', 'ANALYZED');

insert into news.sentiment_result
    (sentiment_result_id, news_item_id, content_hash, model_version, label,
     confidence, polarity_score, analyzed_at)
values
    ('0000000000000000000000000N', '0000000000000000000000000M',
     'content-v1', 'model-v1', 'POSITIVE', 0.8, 0.6, '2026-01-01T00:02:00Z');

insert into platform.idempotency_record
    (user_id, scope, idempotency_key, request_hash, response_status, response_body, expires_at)
values
    ('00000000-0000-4000-8000-000000000001', 'experiment:create', 'same-key', 'hash-1', 201, '{}', now() + interval '1 day'),
    ('00000000-0000-4000-8000-000000000002', 'experiment:create', 'same-key', 'hash-2', 201, '{}', now() + interval '1 day'),
    ('00000000-0000-4000-8000-000000000001', 'experiment:stop', 'same-key', 'hash-3', 200, '{}', now() + interval '1 day');

insert into platform.outbox_event
    (outbox_event_id, message_id, aggregate_type, aggregate_id, event_type,
     event_version, payload, occurred_at)
values
    ('0000000000000000000000000P', '0000000000000000000000000Q',
     'Experiment', '00000000000000000000000007', 'ExperimentCreated', 'v1', '{}', now());

insert into platform.processed_message (consumer_name, message_id, expires_at)
values ('backtest-worker', '0000000000000000000000000Q', now() + interval '1 day');

-- V-02 through V-08: expected constraint failures.
do $$
begin
    begin
        insert into market.asset (asset_id, symbol) values ('invalid', 'ETH');
        raise exception 'invalid ULID was accepted';
    exception when check_violation then null; end;

    begin
        insert into experiment.experiment (experiment_id, owner_user_id, name, status)
        values ('0000000000000000000000000R', '00000000-0000-4000-8000-999999999999', 'bad owner', 'CREATED');
        raise exception 'missing Auth owner was accepted';
    exception when foreign_key_violation then null; end;

    begin
        insert into market.candle
            (candle_id, provider, trading_pair_id, timeframe, open_time, close_time,
             open, high, low, close, volume)
        values ('0000000000000000000000000S', 'binance', '00000000000000000000000003',
                '3h', '2026-01-02T00:00:00Z', '2026-01-02T03:00:00Z', 1, 1, 1, 1, 1);
        raise exception 'non-canonical timeframe was accepted';
    exception when check_violation then null; end;

    begin
        insert into market.candle
            (candle_id, provider, trading_pair_id, timeframe, open_time, close_time,
             open, high, low, close, volume)
        values ('00000000000000000000000012', 'binance', '00000000000000000000000003',
                '1h', '2026-01-02T01:00:00Z', '2026-01-02T00:00:00Z',
                -1, 1, 0, 1, 1);
        raise exception 'invalid Candle time/value range was accepted';
    exception when check_violation then null; end;

    begin
        insert into experiment.experiment (experiment_id, owner_user_id, name, status)
        values ('00000000000000000000000010',
                '00000000-0000-4000-8000-000000000001', 'bad status', 'UNKNOWN');
        raise exception 'invalid Experiment lifecycle value was accepted';
    exception when check_violation then null; end;

    begin
        insert into market.candle
            (candle_id, provider, trading_pair_id, timeframe, open_time, close_time,
             open, high, low, close, volume)
        values ('0000000000000000000000000T', 'binance', '00000000000000000000000003',
                '1h', '2026-01-01T00:00:00Z', '2026-01-01T01:00:00Z', 100, 120, 90, 110, 10);
        raise exception 'duplicate Candle identity was accepted';
    exception when unique_violation then null; end;

    begin
        insert into market.dataset_candle (dataset_version_id, sequence_no, candle_id)
        values ('00000000000000000000000005', 1, '00000000000000000000000004');
        raise exception 'duplicate Dataset Candle membership was accepted';
    exception when unique_violation then null; end;

    begin
        insert into experiment.candidate_definition
            (candidate_id, experiment_id, generation_index, definition, fingerprint)
        values ('0000000000000000000000000V', '00000000000000000000000007', 1, '{}', 'candidate-fp-1');
        raise exception 'duplicate Candidate fingerprint was accepted';
    exception when unique_violation then null; end;

    begin
        insert into strategy.composite_component
            (composite_version_id, position, strategy_version_id, parameter_overrides, weight)
        values ('00000000000000000000000014', 2,
                '00000000000000000000000006', '{}', 1);
        raise exception 'duplicate Strategy in Composite was accepted';
    exception when unique_violation then null; end;

    begin
        insert into experiment.backtest_result
            (backtest_result_id, candidate_id, successful_attempt_id, initial_capital,
             final_capital, result_fingerprint, completed_at)
        values ('0000000000000000000000000W', '00000000000000000000000009',
                '0000000000000000000000000B', 1, 1, 'duplicate-result', now());
        raise exception 'second Result for Candidate was accepted';
    exception when unique_violation then null; end;

    begin
        insert into experiment.evaluation_result
            (evaluation_result_id, backtest_result_id, metric_version, ranking_version,
             total_return, win_rate, maximum_drawdown, number_of_trades, overall_score, evaluated_at)
        values ('0000000000000000000000000X', '0000000000000000000000000F',
                'metrics-v1', 'ranking-v1', 0, 0, 0, 0, 0, now());
        raise exception 'duplicate Evaluation metric version was accepted';
    exception when unique_violation then null; end;

    begin
        insert into experiment.leaderboard_entry
            (leaderboard_revision_id, rank, evaluation_result_id, score)
        values ('0000000000000000000000000K', 1, '0000000000000000000000000J', 0.9);
        raise exception 'duplicate Leaderboard rank was accepted';
    exception when unique_violation then null; end;

    begin
        insert into news.news_item
            (news_item_id, source, source_item_id, url, title, content_hash,
             published_at, crawled_at, analysis_status)
        values ('0000000000000000000000000Y', 'reuters', 'item-1',
                'https://example.test/news/2', 'Duplicate', 'content-v2', now(), now(), 'PENDING');
        raise exception 'duplicate News source identity was accepted';
    exception when unique_violation then null; end;

    begin
        insert into news.sentiment_result
            (sentiment_result_id, news_item_id, content_hash, model_version, label,
             confidence, polarity_score, analyzed_at)
        values ('0000000000000000000000000Z', '0000000000000000000000000M',
                'content-v1', 'model-v1', 'POSITIVE', 0.8, 0.6, now());
        raise exception 'duplicate Sentiment model/input was accepted';
    exception when unique_violation then null; end;

    begin
        insert into news.sentiment_result
            (sentiment_result_id, news_item_id, content_hash, model_version, label,
             confidence, polarity_score, analyzed_at)
        values ('00000000000000000000000011', '0000000000000000000000000M',
                'content-v2', 'model-v1', 'POSITIVE', 1.1, -1.1, now());
        raise exception 'out-of-range Sentiment scores were accepted';
    exception when check_violation then null; end;

    begin
        insert into platform.processed_message (consumer_name, message_id, expires_at)
        values ('backtest-worker', '0000000000000000000000000Q', now() + interval '1 day');
        raise exception 'duplicate processed message was accepted';
    exception when unique_violation then null; end;

    begin
        insert into platform.outbox_event
            (outbox_event_id, message_id, aggregate_type, aggregate_id, event_type,
             event_version, payload, occurred_at)
        values ('00000000000000000000000015', '0000000000000000000000000Q',
                'Experiment', '00000000000000000000000007',
                'ExperimentCreated', 'v1', '{}', now());
        raise exception 'duplicate Outbox message identity was accepted';
    exception when unique_violation then null; end;

    begin
        insert into platform.idempotency_record
            (user_id, scope, idempotency_key, request_hash, response_status, response_body, expires_at)
        values ('00000000-0000-4000-8000-000000000001', 'experiment:create',
                'same-key', 'hash-4', 201, '{}', now() + interval '1 day');
        raise exception 'duplicate scoped idempotency key was accepted';
    exception when unique_violation then null; end;
end;
$$;

-- V-06 and provenance/ownership traversal.
select pg_temp.assert_true(
    (select count(*) = 2 from experiment.experiment_manifest where fingerprint = 'same-manifest'),
    'same manifest fingerprint must be allowed for reproduction runs'
);

select pg_temp.assert_true(
    (select e.owner_user_id = '00000000-0000-4000-8000-000000000001'::uuid
     from experiment.trade t
     join experiment.backtest_result br using (backtest_result_id)
     join experiment.candidate_definition c using (candidate_id)
     join experiment.experiment e using (experiment_id)
     where t.trade_id = '0000000000000000000000000H'),
    'Trade must resolve to exactly one Experiment owner'
);

select pg_temp.assert_true(
    (select count(*) = 1
     from experiment.backtest_result br
     join experiment.candidate_definition c using (candidate_id)
     join experiment.experiment_manifest em using (experiment_id)
     join market.dataset_version dv using (dataset_version_id)
     join market.dataset_candle dc using (dataset_version_id)
     join strategy.strategy_version sv
       on sv.plugin_id = em.strategy_ref_id and sv.version = em.strategy_version
     join experiment.evaluation_result er using (backtest_result_id)
     where br.backtest_result_id = '0000000000000000000000000F'),
    'Result must resolve complete Dataset, Strategy, manifest and Evaluation provenance'
);

select pg_temp.assert_true(
    (select count(*) = 1 from platform.outbox_event where published_at is null),
    'unpublished Outbox Event must remain queryable'
);

select 'database baseline verification passed' as result;

rollback;
