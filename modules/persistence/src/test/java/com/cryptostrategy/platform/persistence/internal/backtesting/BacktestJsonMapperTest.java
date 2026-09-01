package com.cryptostrategy.platform.persistence.internal.backtesting;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BacktestJsonMapperTest {
    @Test void assumptionsRoundTripWithoutHiddenOrLostValues() {
        BacktestAssumptions original = BacktestAssumptions.mvp(
                new BigDecimal("1000.00"), new BigDecimal("0.001"), new BigDecimal("0.0005"));
        BacktestJsonMapper mapper = new BacktestJsonMapper();
        assertEquals(original, mapper.read(mapper.write(original)));
    }

    @Test void malformedJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BacktestJsonMapper().read("{}"));
    }
}
