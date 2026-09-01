begin;
set local statement_timeout = '60s';

create or replace function pg_temp.assert_true(condition boolean, message text)
returns void language plpgsql as $$ begin
    if condition is distinct from true then raise exception 'ASSERTION FAILED: %', message; end if;
end $$;

select pg_temp.assert_true(
    (select count(*) = 1 from information_schema.tables where table_schema='news' and table_name='sentiment_model_release'),
    'sentiment model release table must exist');
select pg_temp.assert_true(
    (select count(*) = 7 from information_schema.columns where table_schema='news' and table_name='news_item'
      and column_name in ('language','target_model_version','lease_owner','lease_token','lease_expires_at','attempt_count','next_eligible_attempt')),
    'all lease and target columns must exist');
select pg_temp.assert_true(
    (select count(*) = 2 from pg_indexes where schemaname='news' and indexname in ('news_analysis_claim_idx','news_expired_lease_idx')),
    'claim and expired lease indexes must exist');
select pg_temp.assert_true(
    (select pg_get_constraintdef(oid) from pg_constraint
      where conrelid='news.news_item'::regclass and conname='news_item_analysis_status_check')
      like all (array['%PENDING%','%ANALYZING%','%ANALYZED%','%FAILED_RETRYABLE%','%FAILED%']),
    'News workflow must expose exactly the five supported states');
select pg_temp.assert_true(
    (select indexdef from pg_indexes where schemaname='news' and indexname='news_analysis_claim_idx')
      like '%news_item_id%' and
    (select indexdef from pg_indexes where schemaname='news' and indexname='news_expired_lease_idx')
      like '%lease_expires_at%news_item_id%',
    'eligibility and expired-lease indexes must use deterministic News ID ordering');
select pg_temp.assert_true(
    exists(select 1 from pg_constraint where conrelid='news.sentiment_model_release'::regclass
      and contype='p' and pg_get_constraintdef(oid)='PRIMARY KEY (model_version)'),
    'model_version must be the release primary key');
select pg_temp.assert_true(
    exists(select 1 from pg_constraint where conrelid='news.sentiment_result'::regclass
      and conname='sentiment_result_model_release_fk' and confdeltype='a'),
    'Sentiment Result must restrict deletion of its model release');
select pg_temp.assert_true(
    exists(select 1 from pg_constraint where conrelid='news.sentiment_result'::regclass
      and conname='sentiment_model_input_unique'),
    'Sentiment Result logical identity must remain unique');

-- Migration-path fixtures model the reviewed legacy mapping gate independently
-- so the empty, explicitly mapped, and unmapped cases remain executable after
-- the forward migration itself has been applied.
create temporary table legacy_result_fixture(model_version text primary key);
create temporary table reviewed_release_fixture(model_version text primary key);
create or replace function pg_temp.assert_legacy_release_mapping()
returns void language plpgsql as $$
begin
    if exists (
        select 1 from legacy_result_fixture legacy
        left join reviewed_release_fixture reviewed using(model_version)
        where reviewed.model_version is null
    ) then
        raise exception using errcode='23514', message='unmapped legacy Sentiment model release';
    end if;
end
$$;
select pg_temp.assert_legacy_release_mapping();
insert into legacy_result_fixture values ('legacy-reviewed');
insert into reviewed_release_fixture values ('legacy-reviewed');
select pg_temp.assert_legacy_release_mapping();
delete from reviewed_release_fixture where model_version='legacy-reviewed';
do $$ begin
    begin
        perform pg_temp.assert_legacy_release_mapping();
        raise exception 'unmapped legacy model was accepted';
    exception when check_violation then null; end;
end $$;
truncate legacy_result_fixture, reviewed_release_fixture;

insert into news.sentiment_model_release(model_version,model_name,preprocessing_version,contract_version)
values ('model-v1','multichannel-english','multichannel-whitespace-en-1','sentiment-v1');

insert into news.news_item(news_item_id,source,url,title,content,content_hash,published_at,crawled_at,analysis_status,language,target_model_version)
values ('10000000000000000000000001','fixture','https://example.test/news','Good news','Positive market update',
        'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',now(),now(),'PENDING','en','model-v1');

-- A dispatch reservation is consumed before transport handoff. A process crash
-- leaves the incremented attempt fenced by an expiring ANALYZING lease.
insert into news.news_item(news_item_id,source,url,title,content,content_hash,published_at,crawled_at,analysis_status,language,target_model_version,
                           lease_owner,lease_token,lease_expires_at,attempt_count)
values ('10000000000000000000000002','fixture','https://example.test/crash','Market news','English article content',
        'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd',now(),now(),'ANALYZING','en','model-v1',
        'worker-crash','30000000000000000000000001',now()+interval '120 seconds',0);
update news.news_item set attempt_count=attempt_count+1
 where news_item_id='10000000000000000000000002' and lease_token='30000000000000000000000001';
select pg_temp.assert_true(
    (select analysis_status='ANALYZING' and attempt_count=1 and lease_token='30000000000000000000000001'
       from news.news_item where news_item_id='10000000000000000000000002'),
    'crash after reservation must retain the consumed attempt and lease fence');
update news.news_item set lease_expires_at=now()-interval '1 second' where news_item_id='10000000000000000000000002';
select pg_temp.assert_true(
    exists(select 1 from news.news_item where news_item_id='10000000000000000000000002'
           and analysis_status='ANALYZING' and lease_expires_at<=now() and attempt_count=1),
    'expired crash lease must be reclaimable without refunding the attempt');

do $$ begin
    begin
        update news.news_item set analysis_status='ANALYZING' where news_item_id='10000000000000000000000001';
        raise exception 'incomplete lease accepted';
    exception when check_violation then null; end;
    begin
        update news.news_item set attempt_count=-1 where news_item_id='10000000000000000000000001';
        raise exception 'negative attempts accepted';
    exception when check_violation then null; end;
    begin
        update news.news_item set target_model_version=null where news_item_id='10000000000000000000000001';
        raise exception 'English News without a target release accepted';
    exception when check_violation then null; end;
    begin
        update news.news_item set analysis_status='FAILED_RETRYABLE' where news_item_id='10000000000000000000000001';
        raise exception 'retryable failure without eligibility accepted';
    exception when check_violation then null; end;
    begin
        update news.news_item set analysis_status='FAILED',lease_owner='worker' where news_item_id='10000000000000000000000001';
        raise exception 'terminal state with lease data accepted';
    exception when check_violation then null; end;
    begin
        insert into news.sentiment_model_release(model_version,model_name,preprocessing_version,contract_version)
        values ('blank-release',' ','prep','sentiment-v1');
        raise exception 'blank release provenance accepted';
    exception when check_violation then null; end;
end $$;

insert into news.sentiment_result(sentiment_result_id,news_item_id,content_hash,model_version,label,confidence,polarity_score,analyzed_at,language)
values ('20000000000000000000000001','10000000000000000000000001',
        'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa','model-v1','POSITIVE',0.8,0.6,now(),'en');

do $$ begin
    begin
        insert into news.sentiment_result(sentiment_result_id,news_item_id,content_hash,model_version,label,confidence,polarity_score,analyzed_at,language)
        values ('20000000000000000000000002','10000000000000000000000001',
                'sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb','model-v1','NEUTRAL',0.5,0,now(),'en');
        raise exception 'mismatched content hash accepted';
    exception when check_violation then null; end;
    begin
        update news.sentiment_result set label='NEGATIVE' where sentiment_result_id='20000000000000000000000001';
        raise exception 'sentiment result was mutable';
    exception when check_violation then null; end;
    begin
        update news.sentiment_model_release set model_name='changed' where model_version='model-v1';
        raise exception 'model release was mutable';
    exception when check_violation then null; end;
    begin
        delete from news.sentiment_model_release where model_version='model-v1';
        raise exception 'model release was deletable';
    exception when check_violation then null; end;
    begin
        delete from news.sentiment_result where sentiment_result_id='20000000000000000000000001';
        raise exception 'sentiment result was deletable';
    exception when check_violation then null; end;
    begin
        insert into news.sentiment_result(sentiment_result_id,news_item_id,content_hash,model_version,label,confidence,polarity_score,analyzed_at,language)
        values ('20000000000000000000000003','10000000000000000000000002',
                'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd','missing-release','POSITIVE',0.5,0,now(),'en');
        raise exception 'orphan model release accepted';
    exception when foreign_key_violation then null; end;
    begin
        insert into news.sentiment_result(sentiment_result_id,news_item_id,content_hash,model_version,label,confidence,polarity_score,analyzed_at,language)
        values ('20000000000000000000000004','10000000000000000000000002',
                'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd','model-v1','UNKNOWN',1.1,-1.1,now(),'en');
        raise exception 'invalid label and score ranges accepted';
    exception when check_violation then null; end;
    begin
        insert into news.sentiment_result(sentiment_result_id,news_item_id,content_hash,model_version,label,confidence,polarity_score,analyzed_at,language)
        values ('20000000000000000000000005','10000000000000000000000002',
                'sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd','model-v1','NEUTRAL',0.5,0,now(),'vi');
        raise exception 'result language differing from parent accepted';
    exception when check_violation then null; end;
    begin
        update news.news_item set content_hash='sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc'
        where news_item_id='10000000000000000000000001';
        raise exception 'analyzed parent hash was mutable';
    exception when check_violation then null; end;
end $$;

-- Concurrent register-or-verify conflict fixture: whichever racing insert wins,
-- a later conflicting tuple cannot change the established global identity.
insert into news.sentiment_model_release(model_version,model_name,preprocessing_version,contract_version)
values ('model-v1','conflicting-name','other-preprocessing','sentiment-v2') on conflict do nothing;
select pg_temp.assert_true(
    (select row(model_name,preprocessing_version,contract_version)=row('multichannel-english','multichannel-whitespace-en-1','sentiment-v1')
       from news.sentiment_model_release where model_version='model-v1'),
    'conflicting release insertion must preserve the established immutable tuple');

select pg_temp.assert_true(
    not exists(select 1 from information_schema.role_table_grants where grantee in ('anon','authenticated') and table_schema='news'),
    'browser roles must not have direct News table access');

rollback;
