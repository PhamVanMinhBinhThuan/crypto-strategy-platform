package com.cryptostrategy.platform.evaluation.internal;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.backtesting.api.model.*;
import com.cryptostrategy.platform.evaluation.api.model.*;
import com.cryptostrategy.platform.experiment.api.*;
import com.cryptostrategy.platform.experiment.api.job.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
class EvaluationReproductionTest {
    @Test void frozenResultReproducesMetricsAndEveryVersionMutationIsDetected(){
        var evaluator=new DeterministicEvaluator();var source=result();
        var original=evaluator.evaluate(source,new MetricVersion("metrics-v1"),new RankingVersion("ranking-v1"));
        var same=evaluator.evaluate(source,new MetricVersion("metrics-v1"),new RankingVersion("ranking-v1"));
        assertTrue(new EvaluationReproductionVerifier().verify(original,same).matched());
        assertFalse(new EvaluationReproductionVerifier().verify(original,evaluator.evaluate(source,new MetricVersion("metrics-v2"),new RankingVersion("ranking-v1"))).matched());
        assertFalse(new EvaluationReproductionVerifier().verify(original,evaluator.evaluate(source,new MetricVersion("metrics-v1"),new RankingVersion("ranking-v2"))).matched());
    }
    private static BacktestResult result(){
        var id=new BacktestResultId("00000000000000000000000010");
        var trade=new Trade(new TradeId("00000000000000000000000011"),id,0,PositionSide.LONG,Instant.parse("2026-01-01T00:00:00Z"),Instant.parse("2026-01-01T00:01:00Z"),Money.of(BigDecimal.TEN),Money.of(BigDecimal.valueOf(11)),new Quantity(BigDecimal.ONE),Money.zero(),Money.zero(),Money.zero(),BigDecimal.ONE,Money.of(BigDecimal.valueOf(1001)),ExitReason.STRATEGY_SELL);
        return new BacktestResult(id,new ExperimentId("00000000000000000000000020"),new CandidateId("00000000000000000000000021"),new JobId("00000000000000000000000022"),new AttemptId("00000000000000000000000023"),new BacktestProvenance("m","d","s"),BacktestAssumptions.mvp(BigDecimal.valueOf(1000),BigDecimal.ZERO,BigDecimal.ZERO),Money.of(BigDecimal.valueOf(1000)),Money.of(BigDecimal.valueOf(1100)),Money.zero(),List.of(trade),new EquityCurveSummary(2,Money.of(BigDecimal.valueOf(1000)),Money.of(BigDecimal.valueOf(800)),0,1,"sha256:"+"0".repeat(64)),"sha256:"+"1".repeat(64),Instant.parse("2026-01-01T01:00:00Z"));
    }
}
