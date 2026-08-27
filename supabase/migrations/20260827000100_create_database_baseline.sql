begin;

create schema if not exists market;
create schema if not exists strategy;
create schema if not exists experiment;
create schema if not exists news;
create schema if not exists platform;

create table market.asset (
    asset_id varchar(26) primary key check (asset_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    symbol text not null unique check (symbol ~ '^[A-Z0-9]+$'),
    name text,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

create table market.trading_pair (
    trading_pair_id varchar(26) primary key check (trading_pair_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    base_asset_id varchar(26) not null references market.asset(asset_id),
    quote_asset_id varchar(26) not null references market.asset(asset_id),
    symbol text not null unique check (symbol ~ '^[A-Z0-9]+$'),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    constraint trading_pair_distinct_assets check (base_asset_id <> quote_asset_id),
    constraint trading_pair_asset_pair_unique unique (base_asset_id, quote_asset_id)
);

create table market.candle (
    candle_id varchar(26) primary key check (candle_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    provider text not null check (provider <> ''),
    trading_pair_id varchar(26) not null references market.trading_pair(trading_pair_id),
    timeframe text not null check (timeframe in ('1m', '5m', '15m', '30m', '1h', '2h', '4h', '1d')),
    open_time timestamptz not null,
    close_time timestamptz not null,
    open numeric(30,12) not null,
    high numeric(30,12) not null,
    low numeric(30,12) not null,
    close numeric(30,12) not null,
    volume numeric(30,12) not null,
    created_at timestamptz not null default now(),
    constraint candle_identity_unique unique (provider, trading_pair_id, timeframe, open_time),
    constraint candle_time_valid check (close_time > open_time),
    constraint candle_values_nonnegative check (open >= 0 and high >= 0 and low >= 0 and close >= 0 and volume >= 0),
    constraint candle_high_valid check (high >= open and high >= low and high >= close),
    constraint candle_low_valid check (low <= open and low <= high and low <= close)
);
create index candle_range_idx on market.candle (trading_pair_id, timeframe, open_time);

create table market.dataset_version (
    dataset_version_id varchar(26) primary key check (dataset_version_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    version text not null check (version <> ''),
    provider text not null check (provider <> ''),
    trading_pair_id varchar(26) not null references market.trading_pair(trading_pair_id),
    timeframe text not null check (timeframe in ('1m', '5m', '15m', '30m', '1h', '2h', '4h', '1d')),
    normalization_version text not null check (normalization_version <> ''),
    range_start timestamptz not null,
    range_end timestamptz not null,
    candle_count integer not null check (candle_count > 0),
    checksum text not null unique check (checksum ~ '^sha256:[0-9a-f]{64}$'),
    created_at timestamptz not null default now(),
    constraint dataset_range_valid check (range_end > range_start)
);

create table market.dataset_candle (
    dataset_version_id varchar(26) not null references market.dataset_version(dataset_version_id),
    sequence_no integer not null check (sequence_no >= 0),
    candle_id varchar(26) not null references market.candle(candle_id),
    primary key (dataset_version_id, sequence_no),
    constraint dataset_candle_membership_unique unique (dataset_version_id, candle_id)
);
create index dataset_candle_candle_idx on market.dataset_candle (candle_id);

create table strategy.strategy_version (
    strategy_version_id varchar(26) primary key check (strategy_version_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    plugin_id text not null check (plugin_id <> ''),
    version text not null check (version <> ''),
    display_name text not null check (display_name <> ''),
    parameter_schema jsonb not null,
    default_parameters jsonb not null,
    supported_signals jsonb not null,
    fingerprint text not null unique check (fingerprint <> ''),
    created_at timestamptz not null default now(),
    constraint strategy_plugin_version_unique unique (plugin_id, version)
);

create table strategy.composite_version (
    composite_version_id varchar(26) primary key check (composite_version_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    composite_id text not null check (composite_id <> ''),
    version text not null check (version <> ''),
    display_name text not null check (display_name <> ''),
    policy_id text not null check (policy_id <> ''),
    policy_version text not null check (policy_version <> ''),
    policy_parameters jsonb not null,
    fingerprint text not null unique check (fingerprint <> ''),
    created_at timestamptz not null default now(),
    constraint composite_identity_unique unique (composite_id, version)
);

create table strategy.composite_component (
    composite_version_id varchar(26) not null references strategy.composite_version(composite_version_id),
    position integer not null check (position >= 0),
    strategy_version_id varchar(26) not null references strategy.strategy_version(strategy_version_id),
    parameter_overrides jsonb not null default '{}'::jsonb,
    weight numeric(20,10) check (weight is null or weight > 0),
    primary key (composite_version_id, position),
    constraint composite_strategy_unique unique (composite_version_id, strategy_version_id)
);

create table platform.user_profile (
    user_id uuid primary key references auth.users(id) on delete cascade,
    display_name text check (display_name is null or display_name <> ''),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table experiment.experiment (
    experiment_id varchar(26) primary key check (experiment_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    owner_user_id uuid not null references auth.users(id),
    derived_from_experiment_id varchar(26) references experiment.experiment(experiment_id),
    name text not null check (name <> ''),
    status text not null check (status in ('CREATED', 'QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'STOP_REQUESTED', 'STOPPED')),
    started_at timestamptz,
    completed_at timestamptz,
    failure_code text,
    failure_message text,
    created_at timestamptz not null default now()
);
create index experiment_owner_created_idx on experiment.experiment (owner_user_id, created_at desc);
create index experiment_status_created_idx on experiment.experiment (status, created_at);

create table experiment.experiment_manifest (
    experiment_id varchar(26) primary key references experiment.experiment(experiment_id),
    manifest_version text not null check (manifest_version <> ''),
    dataset_version_id varchar(26) not null references market.dataset_version(dataset_version_id),
    strategy_kind text not null check (strategy_kind in ('SINGLE', 'COMPOSITE')),
    strategy_ref_id text not null check (strategy_ref_id <> ''),
    strategy_version text not null check (strategy_version <> ''),
    strategy_parameters jsonb not null,
    backtest_config jsonb not null,
    search_config jsonb not null,
    evaluation_config jsonb not null,
    sentiment_config jsonb,
    software_version text not null check (software_version <> ''),
    git_commit text not null check (git_commit <> ''),
    fingerprint text not null check (fingerprint <> ''),
    created_at timestamptz not null default now()
);
create index experiment_manifest_fingerprint_idx on experiment.experiment_manifest (fingerprint);

create table experiment.candidate_definition (
    candidate_id varchar(26) primary key check (candidate_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    experiment_id varchar(26) not null references experiment.experiment(experiment_id),
    generation_index integer not null check (generation_index >= 0),
    definition jsonb not null,
    generator_state jsonb,
    fingerprint text not null check (fingerprint <> ''),
    created_at timestamptz not null default now(),
    constraint candidate_generation_unique unique (experiment_id, generation_index),
    constraint candidate_fingerprint_unique unique (experiment_id, fingerprint)
);

create table experiment.execution_attempt (
    attempt_id varchar(26) primary key check (attempt_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    job_id varchar(26) not null check (job_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    candidate_id varchar(26) not null references experiment.candidate_definition(candidate_id),
    attempt_no integer not null check (attempt_no > 0),
    status text not null check (status in ('QUEUED', 'RUNNING', 'RETRY_SCHEDULED', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    worker_id text,
    started_at timestamptz,
    finished_at timestamptz,
    next_retry_at timestamptz,
    failure_code text,
    failure_message text,
    retryable boolean,
    created_at timestamptz not null default now(),
    constraint execution_job_attempt_unique unique (job_id, attempt_no)
);
create index execution_attempt_candidate_idx on experiment.execution_attempt (candidate_id);
create index execution_attempt_recovery_idx on experiment.execution_attempt (status, next_retry_at);

create table experiment.backtest_result (
    backtest_result_id varchar(26) primary key check (backtest_result_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    candidate_id varchar(26) not null unique references experiment.candidate_definition(candidate_id),
    successful_attempt_id varchar(26) not null references experiment.execution_attempt(attempt_id),
    initial_capital numeric(30,12) not null check (initial_capital >= 0),
    final_capital numeric(30,12) not null check (final_capital >= 0),
    result_fingerprint text not null check (result_fingerprint <> ''),
    completed_at timestamptz not null,
    reproduces_result_id varchar(26) references experiment.backtest_result(backtest_result_id),
    created_at timestamptz not null default now()
);

create table experiment.trade (
    trade_id varchar(26) primary key check (trade_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    backtest_result_id varchar(26) not null references experiment.backtest_result(backtest_result_id),
    sequence_no integer not null check (sequence_no >= 0),
    side text not null check (side in ('BUY', 'SELL')),
    entry_time timestamptz not null,
    exit_time timestamptz not null,
    entry_price numeric(30,12) not null check (entry_price >= 0),
    exit_price numeric(30,12) not null check (exit_price >= 0),
    quantity numeric(30,12) not null check (quantity >= 0),
    fee numeric(30,12) not null check (fee >= 0),
    profit_loss numeric(30,12) not null,
    created_at timestamptz not null default now(),
    constraint trade_result_sequence_unique unique (backtest_result_id, sequence_no),
    constraint trade_time_valid check (exit_time >= entry_time)
);
create index trade_result_entry_idx on experiment.trade (backtest_result_id, entry_time);

create table experiment.evaluation_result (
    evaluation_result_id varchar(26) primary key check (evaluation_result_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    backtest_result_id varchar(26) not null references experiment.backtest_result(backtest_result_id),
    metric_version text not null check (metric_version <> ''),
    ranking_version text not null check (ranking_version <> ''),
    total_return numeric(20,10) not null,
    win_rate numeric(20,10) not null check (win_rate between 0 and 1),
    maximum_drawdown numeric(20,10) not null check (maximum_drawdown >= 0),
    number_of_trades integer not null check (number_of_trades >= 0),
    overall_score numeric(20,10) not null,
    evaluated_at timestamptz not null,
    created_at timestamptz not null default now(),
    constraint evaluation_metric_version_unique unique (backtest_result_id, metric_version)
);
create index evaluation_score_idx on experiment.evaluation_result (overall_score desc);

create table experiment.leaderboard_revision (
    leaderboard_revision_id varchar(26) primary key check (leaderboard_revision_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    experiment_id varchar(26) not null references experiment.experiment(experiment_id),
    revision_no bigint not null check (revision_no > 0),
    top_k integer not null check (top_k > 0),
    created_at timestamptz not null default now(),
    constraint leaderboard_revision_unique unique (experiment_id, revision_no)
);
create index leaderboard_latest_idx on experiment.leaderboard_revision (experiment_id, revision_no desc);

create table experiment.leaderboard_entry (
    leaderboard_revision_id varchar(26) not null references experiment.leaderboard_revision(leaderboard_revision_id),
    rank integer not null check (rank > 0),
    evaluation_result_id varchar(26) not null references experiment.evaluation_result(evaluation_result_id),
    score numeric(20,10) not null,
    primary key (leaderboard_revision_id, rank),
    constraint leaderboard_evaluation_unique unique (leaderboard_revision_id, evaluation_result_id)
);

create table news.news_item (
    news_item_id varchar(26) primary key check (news_item_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    source text not null check (source <> ''),
    source_item_id text,
    url text not null unique check (url <> ''),
    title text not null check (title <> ''),
    summary text,
    content text,
    content_hash text not null check (content_hash <> ''),
    published_at timestamptz not null,
    crawled_at timestamptz not null,
    analysis_status text not null check (analysis_status in ('PENDING', 'ANALYZING', 'ANALYZED', 'FAILED')),
    created_at timestamptz not null default now()
);
create unique index news_source_item_unique on news.news_item (source, source_item_id) where source_item_id is not null;
create index news_published_idx on news.news_item (published_at desc);
create index news_analysis_idx on news.news_item (analysis_status, crawled_at);

create table news.news_item_asset (
    news_item_id varchar(26) not null references news.news_item(news_item_id),
    asset_id varchar(26) not null references market.asset(asset_id),
    relevance_score numeric(20,10) check (relevance_score between 0 and 1),
    primary key (news_item_id, asset_id)
);
create index news_item_asset_asset_idx on news.news_item_asset (asset_id);

create table news.sentiment_result (
    sentiment_result_id varchar(26) primary key check (sentiment_result_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    news_item_id varchar(26) not null references news.news_item(news_item_id),
    content_hash text not null check (content_hash <> ''),
    model_version text not null check (model_version <> ''),
    label text not null check (label in ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    confidence numeric(20,10) not null check (confidence between 0 and 1),
    polarity_score numeric(20,10) not null check (polarity_score between -1 and 1),
    analyzed_at timestamptz not null,
    created_at timestamptz not null default now(),
    constraint sentiment_model_input_unique unique (news_item_id, content_hash, model_version)
);
create index sentiment_news_analyzed_idx on news.sentiment_result (news_item_id, analyzed_at desc);

create table platform.outbox_event (
    outbox_event_id varchar(26) primary key check (outbox_event_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    message_id varchar(26) not null unique check (message_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    aggregate_type text not null check (aggregate_type <> ''),
    aggregate_id varchar(26) not null check (aggregate_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    event_type text not null check (event_type <> ''),
    event_version text not null check (event_version <> ''),
    payload jsonb not null,
    headers jsonb not null default '{}'::jsonb,
    occurred_at timestamptz not null,
    published_at timestamptz,
    publish_attempts integer not null default 0 check (publish_attempts >= 0),
    last_error text,
    created_at timestamptz not null default now()
);
create index outbox_unpublished_idx on platform.outbox_event (occurred_at) where published_at is null;

create table platform.processed_message (
    consumer_name text not null check (consumer_name <> ''),
    message_id varchar(26) not null check (message_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    processed_at timestamptz not null default now(),
    expires_at timestamptz not null,
    primary key (consumer_name, message_id)
);
create index processed_message_expiry_idx on platform.processed_message (expires_at);

create table platform.idempotency_record (
    user_id uuid not null references auth.users(id),
    scope text not null check (scope <> ''),
    idempotency_key text not null check (idempotency_key <> ''),
    request_hash text not null check (request_hash <> ''),
    resource_type text,
    resource_id varchar(26),
    response_status integer not null check (response_status between 100 and 599),
    response_body jsonb not null,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    primary key (user_id, scope, idempotency_key)
);
create index idempotency_expiry_idx on platform.idempotency_record (expires_at);

revoke all on schema market, strategy, experiment, news, platform from anon, authenticated;
revoke all on all tables in schema market, strategy, experiment, news, platform from anon, authenticated;

commit;
