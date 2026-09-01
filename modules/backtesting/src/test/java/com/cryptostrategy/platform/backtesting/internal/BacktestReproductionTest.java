package com.cryptostrategy.platform.backtesting.internal;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
class BacktestReproductionTest {
    @Test void frozenInputHasSameSemanticTradesAndFingerprintAcrossBatchSizes(){
        var fixture=new BacktestTestFixture();
        Strategy strategy=context->new StrategyDecision(
                context.evaluationTime().equals(fixture.start.plusSeconds(60))?StrategySignal.BUY:
                context.evaluationTime().equals(fixture.start.plusSeconds(120))?StrategySignal.SELL:StrategySignal.HOLD,
                context.evaluationTime(),fixture.reference,"TEST","test",Map.of());
        var engine=new DeterministicBacktestEngine();
        var original=engine.run(fixture.command(1),fixture.reader(),strategy);
        var reproduced=engine.run(fixture.command(3),fixture.reader(),strategy);
        var report=new BacktestReproductionVerifier().verify(original,reproduced);
        assertTrue(report.matched(),report.differences().toString());
        assertEquals(original.fingerprint(),reproduced.fingerprint());
    }
}
