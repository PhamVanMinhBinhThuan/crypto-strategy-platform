package com.cryptostrategy.platform.backtesting.api;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.backtesting.api.model.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BacktestFoundationTest {
    @Test void typedIdsValidateCanonicalUlid() {
        assertEquals("00000000000000000000000001", new BacktestResultId("00000000000000000000000001").value());
        assertEquals("00000000000000000000000002", new TradeId("00000000000000000000000002").value());
        assertThrows(IllegalArgumentException.class, () -> new BacktestResultId("bad"));
    }

    @Test void assumptionsAreVersionedAndValidated() {
        BacktestAssumptions assumptions = BacktestAssumptions.mvp(new BigDecimal("1000"), new BigDecimal("0.001"), new BigDecimal("0.002"));
        assertEquals("backtest-assumptions-v1", assumptions.contractVersion());
        assertEquals(PositionMode.LONG_ONLY, assumptions.positionMode());
        assertThrows(IllegalArgumentException.class, () -> BacktestAssumptions.mvp(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test void moneyUsesCanonicalScaleAndHalfEven() {
        assertEquals(new BigDecimal("1.234567890124"), Money.of(new BigDecimal("1.2345678901235")).value());
        assertThrows(IllegalArgumentException.class, () -> Money.of(new BigDecimal("-1")));
    }
}
