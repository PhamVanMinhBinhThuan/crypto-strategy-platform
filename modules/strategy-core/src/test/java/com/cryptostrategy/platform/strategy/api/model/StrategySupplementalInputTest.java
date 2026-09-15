package com.cryptostrategy.platform.strategy.api.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategySupplementalInputTest {
    @Test
    void canonicalizesObservationOrderAndCollections() {
        var later = observation("news-b", "2026-01-02T00:00:00Z");
        var earlier = observation("news-a", "2026-01-01T00:00:00Z");
        var input = new StrategySupplementalInput("sentiment-polarity", "sentiment-snapshot-v1",
                new StrategyInputSnapshotId("01J00000000000000000000050"), "sha256:abc", Map.of("modelVersion", "v1"),
                List.of(later, earlier));

        assertEquals(List.of(earlier, later), input.observations());
        assertThrows(UnsupportedOperationException.class,
                () -> input.metadata().put("another", "value"));
    }

    @Test
    void rejectsDuplicateObservationIdentity() {
        var first = observation("news-a", "2026-01-01T00:00:00Z");
        var duplicate = observation("news-a", "2026-01-02T00:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> new StrategySupplementalInput(
                "sentiment-polarity", "sentiment-snapshot-v1", new StrategyInputSnapshotId("01J00000000000000000000050"), "sha256:abc",
                Map.of(), List.of(first, duplicate)));
    }

    private static StrategyObservation observation(String identity, String occurredAt) {
        String suffix = identity.endsWith("a") ? "51" : "52";
        return new StrategyObservation(new StrategyObservationId("01J000000000000000000000" + suffix), "result-" + identity, Instant.parse(occurredAt),
                new BigDecimal("0.5"), new BigDecimal("0.8"), Map.of());
    }
}
