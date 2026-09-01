package com.cryptostrategy.platform.backtesting.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FailureClassificationCompatibilityTest {
    @Test void reusesAllF005ValuesWithoutReplacement() {
        assertEquals(Set.of("TRANSIENT_NETWORK_ERROR", "DATA_UNAVAILABLE_RETRY", "WORKER_CRASHED", "PERMANENT_LOGIC_ERROR", "UNKNOWN_ERROR"),
                java.util.Arrays.stream(FailureClassification.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }
}
