package com.cryptostrategy.platform.leaderboard.internal;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.evaluation.api.model.*;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
class LeaderboardReproductionTest {
    @Test void projectionIsIndependentOfInputAndDatabaseOrder(){
        var experiment=new ExperimentId("00000000000000000000000030");var version=new RankingVersion("ranking-v1");
        var a=e("00000000000000000000000031",experiment,"0.8","0.2","b",version);var b=e("00000000000000000000000032",experiment,"0.9","0.1","a",version);
        var projector=new TopKProjector();var first=projector.project(experiment,version,1,10,List.of(a,b));var second=projector.project(experiment,version,1,10,List.of(b,a));
        assertTrue(new LeaderboardReproductionVerifier().verify(first,second).isEmpty());assertEquals(first.fingerprint(),second.fingerprint());
    }
    private static EvaluationResult e(String id,ExperimentId experiment,String score,String drawdown,String fingerprint,RankingVersion version){return new EvaluationResult(new EvaluationResultId(id),experiment,new BacktestResultId("00000000000000000000000050"),new MetricVersion("metrics-v1"),version,BigDecimal.ZERO,BigDecimal.ONE,new BigDecimal(drawdown),5,new BigDecimal(score),true,fingerprint,Instant.parse("2026-01-01T00:00:00Z"));}
}
