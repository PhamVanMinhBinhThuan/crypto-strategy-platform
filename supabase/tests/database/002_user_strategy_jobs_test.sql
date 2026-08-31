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

-- V2-01 and V2-02: new schema exists without duplicating authentication secrets.
select pg_temp.assert_true(
    (select count(*) = 4
     from information_schema.tables
     where table_schema || '.' || table_name in (
         'strategy.user_strategy',
         'strategy.user_strategy_version',
         'strategy.user_strategy_component',
         'experiment.job'
     )),
    'three User Strategy tables and one Job table must exist'
);

select pg_temp.assert_true(
    not exists (
        select 1
        from information_schema.columns
        where table_schema in ('market', 'strategy', 'experiment', 'news', 'platform')
          and lower(column_name) in (
              'password', 'password_hash', 'hashed_password', 'session_token',
              'refresh_token', 'access_token'
          )
    ),
    'business schemas must not duplicate passwords or session tokens'
);

-- Transactional fixtures.
insert into auth.users (id) values
    ('10000000-0000-4000-8000-000000000001'),
    ('10000000-0000-4000-8000-000000000002');

insert into strategy.strategy_version (
    strategy_version_id, plugin_id, version, display_name, parameter_schema,
    default_parameters, supported_signals, fingerprint
) values
    ('20000000000000000000000001', 'macd', '1.0.0', 'MACD',
     '{}'::jsonb, '{}'::jsonb, '["BUY","SELL","HOLD"]'::jsonb, 'v2-plugin-macd'),
    ('20000000000000000000000002', 'rsi', '1.0.0', 'RSI',
     '{}'::jsonb, '{}'::jsonb, '["BUY","SELL","HOLD"]'::jsonb, 'v2-plugin-rsi');

insert into strategy.user_strategy (
    user_strategy_id, owner_user_id, strategy_kind, name
) values
    ('30000000000000000000000001', '10000000-0000-4000-8000-000000000001',
     'SINGLE', 'My Strategy'),
    ('30000000000000000000000002', '10000000-0000-4000-8000-000000000002',
     'SINGLE', 'my strategy'),
    ('30000000000000000000000003', '10000000-0000-4000-8000-000000000001',
     'COMPOSITE', 'MACD and RSI');

-- V2-03: owner-scoped, case-insensitive active names.
do $$
begin
    begin
        insert into strategy.user_strategy (
            user_strategy_id, owner_user_id, strategy_kind, name
        ) values (
            '30000000000000000000000004',
            '10000000-0000-4000-8000-000000000001', 'SINGLE', ' MY STRATEGY '
        );
        raise exception 'duplicate active Strategy name was accepted for one owner';
    exception when unique_violation then null; end;

    begin
        insert into strategy.user_strategy (
            user_strategy_id, owner_user_id, strategy_kind, name
        ) values (
            '30000000000000000000000005',
            '10000000-0000-4000-8000-999999999999', 'SINGLE', 'Unknown owner'
        );
        raise exception 'missing Auth owner was accepted';
    exception when foreign_key_violation then null; end;
end;
$$;

insert into strategy.user_strategy_version (
    user_strategy_version_id, user_strategy_id, version_no, strategy_kind,
    strategy_version_id, parameters, lifecycle_status, fingerprint, published_at
) values
    ('40000000000000000000000001', '30000000000000000000000001', 1,
     'SINGLE', '20000000000000000000000001',
     '{"fastPeriod":12,"slowPeriod":26}'::jsonb,
     'PUBLISHED', 'user-a-macd-v1', '2026-08-28T00:00:00Z'),
    ('40000000000000000000000002', '30000000000000000000000002', 1,
     'SINGLE', '20000000000000000000000001',
     '{"fastPeriod":12,"slowPeriod":26}'::jsonb,
     'PUBLISHED', 'user-b-macd-v1', '2026-08-28T00:00:00Z');

insert into strategy.user_strategy_version (
    user_strategy_version_id, user_strategy_id, version_no, strategy_kind,
    parameters, policy_id, policy_version, policy_parameters,
    lifecycle_status, fingerprint
) values (
    '40000000000000000000000003', '30000000000000000000000003', 1,
    'COMPOSITE', '{}'::jsonb, 'weighted-vote', '1.0.0', '{}'::jsonb,
    'DRAFT', 'user-a-composite-v1'
);

insert into strategy.user_strategy_component (
    user_strategy_version_id, position, strategy_version_id, parameters, weight
) values
    ('40000000000000000000000003', 0, '20000000000000000000000001',
     '{"fastPeriod":12,"slowPeriod":26}'::jsonb, 1),
    ('40000000000000000000000003', 1, '20000000000000000000000002',
     '{"period":14}'::jsonb, 1);

update strategy.user_strategy_version
set lifecycle_status = 'PUBLISHED', published_at = '2026-08-28T00:00:00Z'
where user_strategy_version_id = '40000000000000000000000003';

-- V2-04 and V2-05: kind rules and published immutability.
do $$
begin
    begin
        update strategy.user_strategy_version
        set parameters = '{"fastPeriod":9,"slowPeriod":21}'::jsonb
        where user_strategy_version_id = '40000000000000000000000001';
        raise exception 'published Strategy version was mutable';
    exception when raise_exception then
        if sqlerrm = 'published Strategy version was mutable' then raise; end if;
    end;

    begin
        delete from strategy.user_strategy_component
        where user_strategy_version_id = '40000000000000000000000003'
          and position = 0;
        raise exception 'published Strategy component was mutable';
    exception when raise_exception then
        if sqlerrm = 'published Strategy component was mutable' then raise; end if;
    end;

    begin
        insert into strategy.user_strategy_version (
            user_strategy_version_id, user_strategy_id, version_no, strategy_kind,
            parameters, lifecycle_status, fingerprint
        ) values (
            '40000000000000000000000004', '30000000000000000000000001', 2,
            'SINGLE', '{}'::jsonb, 'DRAFT', 'invalid-single-without-plugin'
        );
        raise exception 'single Strategy version without plugin was accepted';
    exception when check_violation then null; end;

    begin
        insert into strategy.user_strategy_component (
            user_strategy_version_id, position, strategy_version_id, parameters, weight
        ) values (
            '40000000000000000000000001', 0,
            '20000000000000000000000001', '{}'::jsonb, 1
        );
        raise exception 'single Strategy version accepted a component';
    exception when raise_exception then
        if sqlerrm = 'single Strategy version accepted a component' then raise; end if;
    end;
end;
$$;

insert into market.asset (asset_id, symbol, name) values
    ('50000000000000000000000001', 'V2BTC', 'V2 Bitcoin fixture'),
    ('50000000000000000000000002', 'V2USDT', 'V2 Tether fixture');

insert into market.trading_pair (
    trading_pair_id, base_asset_id, quote_asset_id, symbol
) values (
    '50000000000000000000000003', '50000000000000000000000001',
    '50000000000000000000000002', 'V2BTCUSDT'
);

insert into market.candle (
    candle_id, provider, trading_pair_id, timeframe, open_time, close_time,
    open, high, low, close, volume
) values (
    '50000000000000000000000004', 'fixture-v2',
    '50000000000000000000000003', '1h',
    '2026-08-01T00:00:00Z', '2026-08-01T01:00:00Z', 100, 110, 90, 105, 5
);

insert into market.dataset_version (
    dataset_version_id, version, provider, trading_pair_id, timeframe,
    normalization_version, range_start, range_end, candle_count, checksum
) values (
    '50000000000000000000000005', 'v2-test', 'fixture-v2',
    '50000000000000000000000003', '1h', 'v1',
    '2026-08-01T00:00:00Z', '2026-08-01T01:00:00Z', 1,
    'sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'
);

insert into market.dataset_candle (dataset_version_id, sequence_no, candle_id)
values ('50000000000000000000000005', 0, '50000000000000000000000004');

insert into experiment.experiment (
    experiment_id, owner_user_id, name, status
) values
    ('60000000000000000000000001', '10000000-0000-4000-8000-000000000001',
     'User A experiment', 'CREATED'),
    ('60000000000000000000000002', '10000000-0000-4000-8000-000000000002',
     'User B experiment', 'CREATED');

insert into experiment.experiment_manifest (
    experiment_id, manifest_version, dataset_version_id, strategy_kind,
    strategy_ref_id, strategy_version, strategy_parameters, backtest_config,
    search_config, evaluation_config, software_version, git_commit, fingerprint,
    source_user_strategy_version_id, dataset_provenance, strategy_provenance
) values (
    '60000000000000000000000001', 'v1', '50000000000000000000000005',
    'SINGLE', 'macd', '1.0.0', '{"fastPeriod":12,"slowPeriod":26}'::jsonb,
    '{}'::jsonb, '{}'::jsonb, '{}'::jsonb, 'v2-test', 'test-commit',
    'user-a-manifest', '40000000000000000000000001',
    '{"datasetVersionId":"50000000000000000000000005","version":"v2-test","checksum":"sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","provider":"fixture-v2","tradingPair":"V2BTC/V2USDT","timeframe":"1h","normalizationVersion":"v1","rangeStart":"2026-08-01T00:00:00Z","rangeEnd":"2026-08-01T01:00:00Z","candleCount":1}'::jsonb,
    '{"kind":"SINGLE","parameters":{"fastPeriod":{"type":"INTEGER","value":"12"},"slowPeriod":{"type":"INTEGER","value":"26"}},"strategyFingerprint":"strategy-v1:sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","sourceUserStrategyVersionId":"40000000000000000000000001","singleStrategy":{"strategyVersionId":"20000000000000000000000001","pluginId":"macd","implementationVersion":"1.0.0"},"components":[]}'::jsonb
);

-- V2-06: a Manifest cannot use another owner's saved Strategy.
do $$
begin
    begin
        insert into experiment.experiment_manifest (
            experiment_id, manifest_version, dataset_version_id, strategy_kind,
            strategy_ref_id, strategy_version, strategy_parameters, backtest_config,
            search_config, evaluation_config, software_version, git_commit, fingerprint,
            source_user_strategy_version_id, dataset_provenance, strategy_provenance
        ) values (
            '60000000000000000000000002', 'v1', '50000000000000000000000005',
            'SINGLE', 'macd', '1.0.0', '{}'::jsonb, '{}'::jsonb, '{}'::jsonb,
            '{}'::jsonb, 'v2-test', 'test-commit', 'wrong-owner-manifest',
            '40000000000000000000000001',
            '{}'::jsonb, '{}'::jsonb
        );
        raise exception 'Manifest accepted another owner Strategy';
    exception when raise_exception then
        if sqlerrm = 'Manifest accepted another owner Strategy' then raise; end if;
    end;
end;
$$;

insert into experiment.candidate_definition (
    candidate_id, experiment_id, generation_index, definition, fingerprint
) values
    ('60000000000000000000000003', '60000000000000000000000001', 0,
     '{}'::jsonb, 'candidate-a'),
    ('60000000000000000000000004', '60000000000000000000000002', 0,
     '{}'::jsonb, 'candidate-b');

insert into experiment.job (
    job_id, experiment_id, candidate_id, job_type, status, correlation_id,
    total_work, completed_work, failed_work
) values
    ('70000000000000000000000001', '60000000000000000000000001', null,
     'SEARCH', 'QUEUED', '70000000000000000000000011', 1000, 0, 0),
    ('70000000000000000000000002', '60000000000000000000000001',
     '60000000000000000000000003', 'BACKTEST', 'RUNNING',
     '70000000000000000000000012', 1, 0, 0);

-- V2-07 and V2-08: Job type, progress and Candidate ownership.
do $$
begin
    begin
        insert into experiment.job (
            job_id, experiment_id, candidate_id, job_type, status, correlation_id,
            total_work, completed_work, failed_work
        ) values (
            '70000000000000000000000003', '60000000000000000000000001',
            '60000000000000000000000004', 'BACKTEST', 'QUEUED',
            '70000000000000000000000013', 1, 0, 0
        );
        raise exception 'Job accepted Candidate from another Experiment';
    exception when foreign_key_violation then null; end;

    begin
        insert into experiment.job (
            job_id, experiment_id, candidate_id, job_type, status, correlation_id,
            total_work, completed_work, failed_work
        ) values (
            '70000000000000000000000004', '60000000000000000000000001',
            '60000000000000000000000003', 'SEARCH', 'QUEUED',
            '70000000000000000000000014', 1, 0, 0
        );
        raise exception 'Search Job with Candidate was accepted';
    exception when check_violation then null; end;

    begin
        insert into experiment.job (
            job_id, experiment_id, candidate_id, job_type, status, correlation_id,
            total_work, completed_work, failed_work
        ) values (
            '70000000000000000000000005', '60000000000000000000000001',
            '60000000000000000000000003', 'BACKTEST', 'QUEUED',
            '70000000000000000000000015', 1, 0, 0
        );
        raise exception 'second Backtest Job for Candidate was accepted';
    exception when unique_violation then null; end;

    begin
        insert into experiment.job (
            job_id, experiment_id, candidate_id, job_type, status, correlation_id,
            total_work, completed_work, failed_work
        ) values (
            '70000000000000000000000006', '60000000000000000000000002', null,
            'SEARCH', 'QUEUED', '70000000000000000000000016', 1, 1, 1
        );
        raise exception 'invalid Job progress was accepted';
    exception when check_violation then null; end;
end;
$$;

insert into experiment.execution_attempt (
    attempt_id, job_id, candidate_id, attempt_no, status
) values
    ('80000000000000000000000001', '70000000000000000000000002',
     '60000000000000000000000003', 1, 'FAILED'),
    ('80000000000000000000000002', '70000000000000000000000002',
     '60000000000000000000000003', 2, 'FAILED'),
    ('80000000000000000000000003', '70000000000000000000000002',
     '60000000000000000000000003', 3, 'SUCCEEDED');

-- V2-09: Attempt must resolve the same Job/Candidate pair.
do $$
begin
    begin
        insert into experiment.execution_attempt (
            attempt_id, job_id, candidate_id, attempt_no, status
        ) values (
            '80000000000000000000000004', '79999999999999999999999999',
            '60000000000000000000000003', 1, 'QUEUED'
        );
        raise exception 'orphan Job Attempt was accepted';
    exception when foreign_key_violation then null; end;

    begin
        insert into experiment.execution_attempt (
            attempt_id, job_id, candidate_id, attempt_no, status
        ) values (
            '80000000000000000000000005', '70000000000000000000000002',
            '60000000000000000000000004', 4, 'QUEUED'
        );
        raise exception 'Attempt with wrong Job Candidate was accepted';
    exception when foreign_key_violation then null; end;
end;
$$;

-- V2-10 and V2-11: ownership paths and recovery indexes.
select pg_temp.assert_true(
    (select count(*) = 1
     from strategy.user_strategy s
     where s.user_strategy_id = '30000000000000000000000001'
       and s.owner_user_id = '10000000-0000-4000-8000-000000000001'),
    'User Strategy must resolve to its Auth owner'
);

select pg_temp.assert_true(
    (select count(*) = 2
     from experiment.job j
     join experiment.experiment e on e.experiment_id = j.experiment_id
     where e.owner_user_id = '10000000-0000-4000-8000-000000000001'),
    'Job ownership must resolve through Experiment'
);

select pg_temp.assert_true(
    (select count(*) = 4
     from pg_indexes
     where indexname in (
         'user_strategy_owner_status_updated_idx',
         'job_experiment_created_idx',
         'job_recovery_idx',
         'job_backtest_candidate_unique'
     )),
    'owner listing and Job recovery indexes must exist'
);

-- V2-12: browser roles retain no direct business access.
select pg_temp.assert_true(
    not exists (
        select 1
        from information_schema.role_table_grants
        where grantee in ('anon', 'authenticated')
          and table_schema in ('strategy', 'experiment')
          and table_name in (
              'user_strategy', 'user_strategy_version',
              'user_strategy_component', 'job'
          )
    ),
    'anon and authenticated must not have grants on new business tables'
);

select pg_temp.assert_true(
    (select count(*) = 3
     from experiment.execution_attempt
     where job_id = '70000000000000000000000002'),
    'one Job must retain all three retry Attempts'
);

rollback;
