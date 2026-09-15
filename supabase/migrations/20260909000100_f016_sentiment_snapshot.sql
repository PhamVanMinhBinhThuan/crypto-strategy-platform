-- Migration: 20260909000100_f016_sentiment_snapshot.sql
-- Description: Persist immutable, model-pinned Sentiment snapshots for Strategy execution.

begin;

create table news.sentiment_snapshot (
    sentiment_snapshot_id varchar(26) primary key
        check (sentiment_snapshot_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),
    schema_version text not null
        check (schema_version = 'sentiment-snapshot-v1'),
    asset_id varchar(26) not null references market.asset(asset_id),
    model_version text not null references news.sentiment_model_release(model_version),
    publication_cutoff timestamptz not null,
    observation_count integer not null check (observation_count > 0),
    fingerprint text not null unique
        check (fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    created_at timestamptz not null default now()
);

create index sentiment_snapshot_asset_cutoff_idx
    on news.sentiment_snapshot(asset_id, publication_cutoff desc, sentiment_snapshot_id);

create table news.sentiment_snapshot_observation (
    sentiment_snapshot_id varchar(26) not null
        references news.sentiment_snapshot(sentiment_snapshot_id),
    ordinal integer not null check (ordinal > 0),
    news_item_id varchar(26) not null references news.news_item(news_item_id),
    sentiment_result_id varchar(26) not null references news.sentiment_result(sentiment_result_id),
    published_at timestamptz not null,
    analyzed_at timestamptz not null,
    content_hash text not null check (content_hash ~ '^sha256:[0-9a-f]{64}$'),
    polarity_score numeric(20,10) not null check (polarity_score between -1 and 1),
    confidence numeric(20,10) not null check (confidence between 0 and 1),
    primary key (sentiment_snapshot_id, ordinal),
    unique (sentiment_snapshot_id, news_item_id),
    unique (sentiment_snapshot_id, sentiment_result_id)
);

create index sentiment_snapshot_observation_time_idx
    on news.sentiment_snapshot_observation(
        sentiment_snapshot_id, published_at, news_item_id
    );

create function news.validate_sentiment_snapshot_observation()
returns trigger
language plpgsql
as $$
declare
    snapshot_asset varchar(26);
    snapshot_model text;
    snapshot_cutoff timestamptz;
    result_news varchar(26);
    result_hash text;
    result_model text;
    result_analyzed_at timestamptz;
    result_polarity numeric(20,10);
    result_confidence numeric(20,10);
    news_published_at timestamptz;
begin
    select asset_id, model_version, publication_cutoff
      into snapshot_asset, snapshot_model, snapshot_cutoff
      from news.sentiment_snapshot
     where sentiment_snapshot_id = new.sentiment_snapshot_id
     for key share;

    select r.news_item_id, r.content_hash, r.model_version, r.analyzed_at,
           r.polarity_score, r.confidence, n.published_at
      into result_news, result_hash, result_model, result_analyzed_at,
           result_polarity, result_confidence, news_published_at
      from news.sentiment_result r
      join news.news_item n on n.news_item_id = r.news_item_id
     where r.sentiment_result_id = new.sentiment_result_id;

    if not found then
        raise exception using errcode = '23503', message = 'snapshot observation references missing sentiment evidence';
    end if;
    if new.news_item_id <> result_news
       or new.content_hash <> result_hash
       or new.published_at <> news_published_at
       or new.analyzed_at <> result_analyzed_at
       or new.polarity_score <> result_polarity
       or new.confidence <> result_confidence then
        raise exception using errcode = '23514', message = 'snapshot observation does not match immutable sentiment evidence';
    end if;
    if snapshot_model <> result_model then
        raise exception using errcode = '23514', message = 'snapshot observation model release mismatch';
    end if;
    if new.published_at > snapshot_cutoff then
        raise exception using errcode = '23514', message = 'snapshot observation is after publication cutoff';
    end if;
    if not exists (
        select 1 from news.news_item_asset a
         where a.news_item_id = new.news_item_id and a.asset_id = snapshot_asset
    ) then
        raise exception using errcode = '23514', message = 'snapshot observation is unrelated to snapshot asset';
    end if;
    return new;
end
$$;

create trigger sentiment_snapshot_observation_validate
before insert on news.sentiment_snapshot_observation
for each row execute function news.validate_sentiment_snapshot_observation();

create function news.validate_sentiment_snapshot_count()
returns trigger
language plpgsql
as $$
declare
    target_id varchar(26);
    expected_count integer;
    actual_count bigint;
begin
    target_id := coalesce(new.sentiment_snapshot_id, old.sentiment_snapshot_id);
    select observation_count into expected_count
      from news.sentiment_snapshot where sentiment_snapshot_id = target_id;
    if found then
        select count(*) into actual_count
          from news.sentiment_snapshot_observation where sentiment_snapshot_id = target_id;
        if actual_count <> expected_count then
            raise exception using errcode = '23514', message = 'sentiment snapshot observation count mismatch';
        end if;
    end if;
    return null;
end
$$;

create constraint trigger sentiment_snapshot_count_after_snapshot
after insert on news.sentiment_snapshot
deferrable initially deferred
for each row execute function news.validate_sentiment_snapshot_count();

create constraint trigger sentiment_snapshot_count_after_observation
after insert or update or delete on news.sentiment_snapshot_observation
deferrable initially deferred
for each row execute function news.validate_sentiment_snapshot_count();

create function news.reject_sentiment_snapshot_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception using errcode = '23514', message = 'sentiment snapshots are immutable';
end
$$;

create trigger sentiment_snapshot_immutable
before update or delete on news.sentiment_snapshot
for each row execute function news.reject_sentiment_snapshot_mutation();

create trigger sentiment_snapshot_observation_immutable
before update or delete on news.sentiment_snapshot_observation
for each row execute function news.reject_sentiment_snapshot_mutation();

revoke all on news.sentiment_snapshot from anon, authenticated;
revoke all on news.sentiment_snapshot_observation from anon, authenticated;

commit;
