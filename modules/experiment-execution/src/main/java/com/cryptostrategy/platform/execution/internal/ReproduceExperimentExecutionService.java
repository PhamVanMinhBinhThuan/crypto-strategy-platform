package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.execution.api.*;
import com.cryptostrategy.platform.execution.api.port.in.ReproduceExperimentExecutionUseCase;
import com.cryptostrategy.platform.execution.api.port.out.*;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.port.in.ReproduceExperimentUseCase;
import java.time.Instant;
import java.util.*;

public final class ReproduceExperimentExecutionService implements ReproduceExperimentExecutionUseCase {
    private final ReproduceExperimentUseCase experiments;
    private final ExecutionEvidenceReader evidence;
    private final ReproductionExecutionRunner runner;
    private final ReproductionVerificationStore verifications;

    public ReproduceExperimentExecutionService(ReproduceExperimentUseCase experiments,
            ExecutionEvidenceReader evidence, ReproductionExecutionRunner runner,
            ReproductionVerificationStore verifications) {
        this.experiments=Objects.requireNonNull(experiments);this.evidence=Objects.requireNonNull(evidence);
        this.runner=Objects.requireNonNull(runner);this.verifications=Objects.requireNonNull(verifications);
    }

    @Override public ReproductionVerification reproduce(UUID owner, ExperimentId source, String name) {
        ExecutionEvidence original=evidence.load(owner,source);
        var reproduction=experiments.reproduceExperiment(owner,source,name);
        if(!source.equals(reproduction.reproducesExperimentId()))throw new IllegalStateException("Reproduction lineage mismatch");
        ExecutionEvidence reproduced=runner.run(owner,reproduction.experimentId());
        Map<String,Object> differences=compare(original,reproduced);
        var fingerprints=Map.of("backtest",reproduced.backtest().fingerprint(),"evaluation",reproduced.evaluation().fingerprint(),"leaderboard",reproduced.leaderboard().fingerprint());
        var verification=new ReproductionVerification(reproduction.experimentId(),source,original,reproduced,
                differences.isEmpty()?ReproductionOutcome.MATCHED:ReproductionOutcome.MISMATCHED,
                differences,fingerprints,Instant.now());
        return verifications.save(verification);
    }

    private static Map<String,Object> compare(ExecutionEvidence original,ExecutionEvidence reproduced){
        Map<String,Object> differences=new TreeMap<>();
        var leftTrades=original.backtest().trades().stream().map(ReproduceExperimentExecutionService::semanticTrade).toList();
        var rightTrades=reproduced.backtest().trades().stream().map(ReproduceExperimentExecutionService::semanticTrade).toList();
        mismatch(differences,"trades",leftTrades,rightTrades);
        mismatch(differences,"equity",original.backtest().equityCurveSummary(),reproduced.backtest().equityCurveSummary());
        mismatch(differences,"backtestFingerprint",original.backtest().fingerprint(),reproduced.backtest().fingerprint());
        mismatch(differences,"metrics",metrics(original),metrics(reproduced));
        mismatch(differences,"evaluationFingerprint",original.evaluation().fingerprint(),reproduced.evaluation().fingerprint());
        mismatch(differences,"leaderboardFingerprint",original.leaderboard().fingerprint(),reproduced.leaderboard().fingerprint());
        return Map.copyOf(differences);
    }
    private static List<Object> metrics(ExecutionEvidence value){return List.of(value.evaluation().totalReturn(),value.evaluation().winRate(),value.evaluation().maximumDrawdown(),value.evaluation().numberOfTrades());}
    private static String semanticTrade(com.cryptostrategy.platform.backtesting.api.model.Trade t){return t.sequence()+"|"+t.entryTime()+"|"+t.exitTime()+"|"+t.entryPrice()+"|"+t.exitPrice()+"|"+t.quantity()+"|"+t.totalFee()+"|"+t.realizedPnl();}
    private static void mismatch(Map<String,Object> differences,String name,Object expected,Object actual){if(!Objects.equals(expected,actual))differences.put(name,Map.of("expected",expected,"actual",actual));}
}
