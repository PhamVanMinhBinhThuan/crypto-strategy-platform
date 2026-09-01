package com.cryptostrategy.platform.backtesting.internal;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.backtesting.api.error.BacktestException;
import com.cryptostrategy.platform.backtesting.api.model.BacktestRunCommand;
import com.cryptostrategy.platform.backtesting.api.port.out.ResolvedStrategy;
import com.cryptostrategy.platform.experiment.api.*;
import com.cryptostrategy.platform.experiment.api.execution.FrozenBacktestExecution;
import com.cryptostrategy.platform.experiment.api.job.*;
import com.cryptostrategy.platform.experiment.api.provenance.*;
import com.cryptostrategy.platform.marketdata.api.model.DatasetIntegrityResult;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class RunBacktestServiceTest {
    @Test void resolvesAllExecutableInputsFromFrozenGraph() {
        BacktestTestFixture f=new BacktestTestFixture(); AtomicBoolean stored=new AtomicBoolean();
        Strategy hold=c->new StrategyDecision(StrategySignal.HOLD,c.evaluationTime(),f.reference,"HOLD","test",Map.of());
        String fingerprint="strategy-v1:sha256:"+"1".repeat(64);
        var service=new RunBacktestService((owner,e,c,j,a)->frozen(f,fingerprint),id->f.dataset,
                id->DatasetIntegrityResult.validResult(),f.reader(),p->new ResolvedStrategy(hold,3,fingerprint),
                result->{stored.set(true);return result;});
        service.run(command()); assertTrue(stored.get());
    }
    @Test void checksumFailureProducesNoPartialStore() {
        BacktestTestFixture f=new BacktestTestFixture(); AtomicBoolean stored=new AtomicBoolean();
        String fingerprint="strategy-v1:sha256:"+"1".repeat(64);
        var service=new RunBacktestService((owner,e,c,j,a)->frozen(f,fingerprint),id->f.dataset,
                id->DatasetIntegrityResult.invalid("tampered"),f.reader(),p->{throw new AssertionError();},
                result->{stored.set(true);return result;});
        assertThrows(BacktestException.class,()->service.run(command())); assertFalse(stored.get());
    }
    private static BacktestRunCommand command(){return new BacktestRunCommand(owner(),eid(),cid(),jid(),aid(),1);}
    private static FrozenBacktestExecution frozen(BacktestTestFixture f,String strategyFingerprint){
        Instant now=f.start;var strategy=StrategyProvenanceSnapshot.single(f.reference,StrategyParameterSet.empty(),Optional.empty(),strategyFingerprint);
        var dataset=new DatasetProvenanceSnapshot(f.datasetId,"1",f.dataset.checksum(),"BINANCE",f.pair.canonicalSymbol(),"1m","v1",f.start,f.start.plusSeconds(180),3);
        Map<String,Object> config=Map.of("assumptionsVersion","backtest-assumptions-v1","initialCapital","1000","feeRate","0.001","slippageRate","0.001","executionPriceRule","NEXT_CANDLE_OPEN","positionMode","LONG_ONLY","forceCloseAtEnd",true,"roundingMode","HALF_EVEN");
        var manifest=new ExperimentManifest(eid(),"manifest-v1",dataset,strategy,config,Map.of(),Map.of(),null,"1","abc","manifest",now);
        var experiment=new Experiment(eid(),owner(),"test",ExperimentStatus.RUNNING,null,null,now,null,null,null,now);
        var candidate=new CandidateDefinition(cid(),eid(),0,Map.of(),null,"candidate",now);
        var job=new Job(jid(),eid(),cid(),JobType.BACKTEST,JobStatus.SUCCEEDED,"corr",1,1,0,null,now,now,now,null,null,null,now,now);
        var attempt=new ExecutionAttempt(aid(),jid(),cid(),1,AttemptStatus.SUCCEEDED,"worker",now,now,null,null,null,false,now);
        return new FrozenBacktestExecution(experiment,manifest,candidate,job,attempt);
    }
    private static UUID owner(){return UUID.fromString("00000000-0000-0000-0000-000000000001");}
    private static ExperimentId eid(){return new ExperimentId("0000000000000000000000000A");}
    private static CandidateId cid(){return new CandidateId("0000000000000000000000000B");}
    private static JobId jid(){return new JobId("0000000000000000000000000C");}
    private static AttemptId aid(){return new AttemptId("0000000000000000000000000D");}
}
