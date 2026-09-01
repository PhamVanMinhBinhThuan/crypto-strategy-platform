package com.cryptostrategy.platform.evaluation.api;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.evaluation.api.model.*;
import org.junit.jupiter.api.Test;

class EvaluationFoundationTest {
    @Test void idsAndVersionsAreTyped() {
        assertEquals("00000000000000000000000003", new EvaluationResultId("00000000000000000000000003").value());
        assertEquals("metrics-v1", new MetricVersion("metrics-v1").value());
        assertThrows(IllegalArgumentException.class, () -> new MetricVersion(" "));
    }
}
