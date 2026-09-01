package com.cryptostrategy.platform.leaderboard.api;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import org.junit.jupiter.api.Test;

class LeaderboardFoundationTest {
    @Test void revisionIdIsTypedUlid() {
        assertEquals("00000000000000000000000004", new LeaderboardRevisionId("00000000000000000000000004").value());
        assertThrows(IllegalArgumentException.class, () -> new LeaderboardRevisionId("uuid"));
    }
}
