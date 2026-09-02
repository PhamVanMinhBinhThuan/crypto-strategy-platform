package com.cryptostrategy.platform.api.leaderboard;

import com.cryptostrategy.platform.api.transport.InvalidCursorException;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

record LeaderboardCursor(LeaderboardRevisionId revisionId, int rank) {
    private static final String VERSION = "leaderboard-v1";

    String encode() {
        String value = VERSION + "|" + revisionId.value() + "|" + rank;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static LeaderboardCursor decode(String cursor, LeaderboardRevisionId expectedRevisionId) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 3
                    || !VERSION.equals(parts[0])
                    || !expectedRevisionId.value().equals(parts[1])) {
                throw invalid(null);
            }
            int rank = Integer.parseInt(parts[2]);
            if (rank < 1) {
                throw invalid(null);
            }
            return new LeaderboardCursor(new LeaderboardRevisionId(parts[1]), rank);
        } catch (InvalidCursorException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid(exception);
        }
    }

    private static InvalidCursorException invalid(Throwable cause) {
        return cause == null
                ? new InvalidCursorException("Leaderboard cursor is invalid")
                : new InvalidCursorException("Leaderboard cursor is invalid", cause);
    }
}
