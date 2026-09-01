package com.cryptostrategy.platform.backtesting.api.model;
import static org.junit.jupiter.api.Assertions.*;
import java.math.*;
import org.junit.jupiter.api.Test;
class BacktestAssumptionsContractTest {
    @Test void publicConstructorRejectsUnsupportedMvpSemantics(){
        assertThrows(IllegalArgumentException.class,()->new BacktestAssumptions("future",Money.of(BigDecimal.TEN),BigDecimal.ZERO,BigDecimal.ZERO,PositionMode.LONG_ONLY,ExecutionPriceRule.NEXT_CANDLE_OPEN,true,RoundingMode.HALF_EVEN));
        assertThrows(IllegalArgumentException.class,()->new BacktestAssumptions("backtest-assumptions-v1",Money.of(BigDecimal.TEN),BigDecimal.ZERO,BigDecimal.ZERO,PositionMode.LONG_ONLY,ExecutionPriceRule.NEXT_CANDLE_OPEN,false,RoundingMode.HALF_EVEN));
        assertThrows(IllegalArgumentException.class,()->new BacktestAssumptions("backtest-assumptions-v1",Money.of(BigDecimal.TEN),BigDecimal.ZERO,BigDecimal.ZERO,PositionMode.LONG_ONLY,ExecutionPriceRule.NEXT_CANDLE_OPEN,true,RoundingMode.DOWN));
    }
}
