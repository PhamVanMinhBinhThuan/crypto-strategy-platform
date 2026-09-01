begin;

do $$
begin
    if exists (select 1 from news.sentiment_result) then
        raise exception using
            errcode = '23514',
            message = 'F-008 migration requires an explicit model-release mapping for existing sentiment_result rows';
    end if;
end
$$;

create table news.sentiment_model_release (
    model_version text primary key check (btrim(model_version) <> ''),
    model_name text not null check (btrim(model_name) <> ''),
    preprocessing_version text not null check (btrim(preprocessing_version) <> ''),
    contract_version text not null check (btrim(contract_version) <> ''),
    created_at timestamptz not null default now()
);

alter table news.news_item
    add column language text not null default 'und',
    add column target_model_version text references news.sentiment_model_release(model_version),
    add column lease_owner text,
    add column lease_token varchar(26),
    add column lease_expires_at timestamptz,
    add column attempt_count integer not null default 0,
    add column next_eligible_attempt timestamptz;

alter table news.news_item
    drop constraint news_item_analysis_status_check,
    add constraint news_item_analysis_status_check
        check (analysis_status in ('PENDING', 'ANALYZING', 'ANALYZED', 'FAILED_RETRYABLE', 'FAILED')),
    add constraint news_item_language_valid
        check (language ~ '^[a-z]{2,3}(-[a-z0-9]{2,8})*$' or language = 'und'),
    add constraint news_item_attempt_count_valid check (attempt_count >= 0),
    add constraint news_item_target_release_required
        check (language <> 'en' or target_model_version is not null),
    add constraint news_item_analysis_lease_state_valid check (
        (analysis_status = 'ANALYZING'
            and lease_owner is not null and btrim(lease_owner) <> ''
            and lease_token ~ '^[0-9A-HJKMNP-TV-Z]{26}$'
            and lease_expires_at is not null
            and next_eligible_attempt is null)
        or
        (analysis_status = 'FAILED_RETRYABLE'
            and lease_owner is null and lease_token is null and lease_expires_at is null
            and next_eligible_attempt is not null)
        or
        (analysis_status in ('PENDING', 'ANALYZED', 'FAILED')
            and lease_owner is null and lease_token is null and lease_expires_at is null
            and next_eligible_attempt is null)
    );

create index news_analysis_claim_idx on news.news_item (
    (case
        when analysis_status = 'ANALYZING' then lease_expires_at
        else coalesce(next_eligible_attempt, crawled_at)
    end),
    news_item_id
) where analysis_status in ('PENDING', 'FAILED_RETRYABLE', 'ANALYZING');

create index news_expired_lease_idx on news.news_item (lease_expires_at, news_item_id)
    where analysis_status = 'ANALYZING';

alter table news.sentiment_result
    add column language text not null default 'und',
    add constraint sentiment_result_language_valid
        check (language ~ '^[a-z]{2,3}(-[a-z0-9]{2,8})*$' or language = 'und'),
    add constraint sentiment_result_model_release_fk
        foreign key (model_version) references news.sentiment_model_release(model_version);

create function news.reject_sentiment_model_release_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception using
        errcode = '23514',
        message = 'sentiment model releases are immutable';
end
$$;

create trigger sentiment_model_release_immutable
before update or delete on news.sentiment_model_release
for each row execute function news.reject_sentiment_model_release_mutation();

create function news.validate_sentiment_result_insert()
returns trigger
language plpgsql
as $$
declare
    parent_hash text;
    parent_language text;
begin
    select content_hash, language
      into parent_hash, parent_language
      from news.news_item
     where news_item_id = new.news_item_id
     for key share;

    if not found then
        raise exception using errcode = '23503', message = 'sentiment result references a missing news item';
    end if;
    if new.content_hash <> parent_hash then
        raise exception using errcode = '23514', message = 'sentiment result content hash does not match its news item';
    end if;
    if new.language <> parent_language then
        raise exception using errcode = '23514', message = 'sentiment result language does not match its news item';
    end if;
    return new;
end
$$;

create trigger sentiment_result_validate_insert
before insert on news.sentiment_result
for each row execute function news.validate_sentiment_result_insert();

create function news.reject_sentiment_result_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception using errcode = '23514', message = 'sentiment results are immutable';
end
$$;

create trigger sentiment_result_immutable
before update or delete on news.sentiment_result
for each row execute function news.reject_sentiment_result_mutation();

create function news.protect_analyzed_news_content()
returns trigger
language plpgsql
as $$
begin
    if new.content_hash is distinct from old.content_hash
       and exists (
           select 1 from news.sentiment_result
            where news_item_id = old.news_item_id
       ) then
        raise exception using
            errcode = '23514',
            message = 'news content hash is immutable after a sentiment result exists';
    end if;
    return new;
end
$$;

create trigger news_item_protect_analyzed_content
before update of content_hash on news.news_item
for each row execute function news.protect_analyzed_news_content();

revoke all on news.sentiment_model_release from anon, authenticated;

commit;
