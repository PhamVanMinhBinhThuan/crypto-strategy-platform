begin;
set local search_path = experiment, public;

insert into auth.users(id) values ('91000000-0000-4000-8000-000000000001');
insert into experiment.experiment(experiment_id,owner_user_id,name,status)
values ('61000000000000000000000001','91000000-0000-4000-8000-000000000001','F006 negative SQL','RUNNING');
insert into experiment.candidate_definition(candidate_id,experiment_id,generation_index,definition,fingerprint)
values ('61000000000000000000000002','61000000000000000000000001',0,'{}','candidate-negative');
insert into experiment.job(job_id,experiment_id,candidate_id,job_type,status,correlation_id,total_work,completed_work,failed_work)
values ('61000000000000000000000003','61000000000000000000000001','61000000000000000000000002','BACKTEST','RUNNING','61000000000000000000000009',1,0,0);
insert into experiment.execution_attempt(attempt_id,job_id,candidate_id,attempt_no,status,started_at)
values ('61000000000000000000000004','61000000000000000000000003','61000000000000000000000002',1,'RUNNING',now());

do $$ begin
    begin
        insert into experiment.backtest_result(backtest_result_id,experiment_id,candidate_id,job_id,successful_attempt_id,
          initial_capital,final_capital,total_fees,result_fingerprint,manifest_fingerprint,dataset_fingerprint,
          strategy_fingerprint,assumptions_version,assumptions_json,equity_point_count,equity_peak,equity_trough,
          equity_peak_sequence,equity_trough_sequence,equity_curve_fingerprint,completed_at)
        values ('61000000000000000000000005','61000000000000000000000001','61000000000000000000000002',
          '61000000000000000000000003','61000000000000000000000004',1000,1000,0,
          'sha256:'||repeat('1',64),'manifest','dataset','strategy','backtest-assumptions-v1','{}',1,1000,1000,0,0,
          'sha256:'||repeat('2',64),now());
        raise exception 'ASSERTION FAILED: non-successful Attempt was accepted';
    exception when raise_exception then
        if sqlerrm like 'ASSERTION FAILED:%' then raise; end if;
    end;
end $$;

update experiment.execution_attempt set status='SUCCEEDED', finished_at=now()
where attempt_id='61000000000000000000000004';
update experiment.job set status='SUCCEEDED', completed_work=1, finished_at=now()
where job_id='61000000000000000000000003';

insert into experiment.backtest_result(backtest_result_id,experiment_id,candidate_id,job_id,successful_attempt_id,
  initial_capital,final_capital,total_fees,result_fingerprint,manifest_fingerprint,dataset_fingerprint,
  strategy_fingerprint,assumptions_version,assumptions_json,equity_point_count,equity_peak,equity_trough,
  equity_peak_sequence,equity_trough_sequence,equity_curve_fingerprint,completed_at)
values ('61000000000000000000000005','61000000000000000000000001','61000000000000000000000002',
  '61000000000000000000000003','61000000000000000000000004',1000,1000,0,
  'sha256:'||repeat('1',64),'manifest','dataset','strategy','backtest-assumptions-v1','{}',1,1000,1000,0,0,
  'sha256:'||repeat('2',64),now());

insert into experiment.evaluation_result(evaluation_result_id,experiment_id,backtest_result_id,metric_version,
 ranking_version,total_return,win_rate,maximum_drawdown,number_of_trades,return_score,win_rate_score,
 drawdown_score,overall_score,leaderboard_eligible,evaluation_fingerprint,evaluated_at)
values ('61000000000000000000000006','61000000000000000000000001','61000000000000000000000005',
 'metrics-v1','ranking-v1',0,0,0,5,0,0,1,0.25,true,'sha256:'||repeat('3',64),now());
insert into experiment.leaderboard_revision(leaderboard_revision_id,experiment_id,revision_no,top_k,ranking_version,revision_fingerprint)
values ('61000000000000000000000007','61000000000000000000000001',1,1,'ranking-v1','sha256:'||repeat('4',64));

do $$ begin
    begin
        insert into experiment.leaderboard_entry(leaderboard_revision_id,experiment_id,rank,evaluation_result_id,
          score,maximum_drawdown,evaluation_fingerprint)
        values ('61000000000000000000000007','61000000000000000000000001',2,
          '61000000000000000000000006',0.25,0,'sha256:'||repeat('3',64));
        raise exception 'ASSERTION FAILED: rank beyond Top-K was accepted';
    exception when raise_exception then
        if sqlerrm like 'ASSERTION FAILED:%' then raise; end if;
    end;
    begin
        insert into experiment.leaderboard_entry(leaderboard_revision_id,experiment_id,rank,evaluation_result_id,
          score,maximum_drawdown,evaluation_fingerprint)
        values ('61000000000000000000000007','61000000000000000000000001',1,
          '61000000000000000000000006',0.99,0,'sha256:'||repeat('3',64));
        raise exception 'ASSERTION FAILED: incorrect Evaluation snapshot was accepted';
    exception when raise_exception then
        if sqlerrm like 'ASSERTION FAILED:%' then raise; end if;
    end;
    begin
        update experiment.backtest_result set final_capital=999 where backtest_result_id='61000000000000000000000005';
        raise exception 'ASSERTION FAILED: immutable Result was updated';
    exception when raise_exception then
        if sqlerrm like 'ASSERTION FAILED:%' then raise; end if;
    end;
end $$;

rollback;
