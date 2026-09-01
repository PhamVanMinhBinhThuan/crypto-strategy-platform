-- H3 Fix: Enforce exact lineage across experiment components
alter table experiment.backtest_result add constraint uq_backtest_result_experiment unique (experiment_id, backtest_result_id);
alter table experiment.evaluation_result add constraint uq_evaluation_result_experiment unique (experiment_id, evaluation_result_id);
alter table experiment.leaderboard_revision add constraint uq_leaderboard_revision_experiment unique (experiment_id, leaderboard_revision_id);

alter table experiment.reproduction_verification 
    add constraint fk_reproduction_source_backtest 
    foreign key (source_experiment_id, original_backtest_result_id) 
    references experiment.backtest_result(experiment_id, backtest_result_id);

alter table experiment.reproduction_verification 
    add constraint fk_reproduction_reproduced_backtest 
    foreign key (reproduction_experiment_id, reproduced_backtest_result_id) 
    references experiment.backtest_result(experiment_id, backtest_result_id);

alter table experiment.reproduction_verification 
    add constraint fk_reproduction_source_evaluation 
    foreign key (source_experiment_id, original_evaluation_result_id) 
    references experiment.evaluation_result(experiment_id, evaluation_result_id);

alter table experiment.reproduction_verification 
    add constraint fk_reproduction_reproduced_evaluation 
    foreign key (reproduction_experiment_id, reproduced_evaluation_result_id) 
    references experiment.evaluation_result(experiment_id, evaluation_result_id);

alter table experiment.reproduction_verification 
    add constraint fk_reproduction_source_leaderboard 
    foreign key (source_experiment_id, original_leaderboard_revision_id) 
    references experiment.leaderboard_revision(experiment_id, leaderboard_revision_id);

alter table experiment.reproduction_verification 
    add constraint fk_reproduction_reproduced_leaderboard 
    foreign key (reproduction_experiment_id, reproduced_leaderboard_revision_id) 
    references experiment.leaderboard_revision(experiment_id, leaderboard_revision_id);
