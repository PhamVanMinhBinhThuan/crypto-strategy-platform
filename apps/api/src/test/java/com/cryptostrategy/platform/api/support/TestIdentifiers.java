package com.cryptostrategy.platform.api.support;

import com.cryptostrategy.platform.domain.api.identity.Ulids;
import java.util.Locale;

/** Deterministic opaque resource and correlation identifiers for public contract tests. */
public final class TestIdentifiers {
    public static final String DATASET_ID = opaqueId(1);
    public static final String USER_STRATEGY_ID = opaqueId(2);
    public static final String EXPERIMENT_ID = opaqueId(3);
    public static final String CANDIDATE_ID = opaqueId(4);
    public static final String JOB_ID = opaqueId(5);
    public static final String RESULT_ID = opaqueId(6);
    public static final String LEADERBOARD_ID = opaqueId(7);

    private TestIdentifiers() {}

    public static String opaqueId(long sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException("Test ID sequence must not be negative");
        }
        return Ulids.requireValid(String.format(Locale.ROOT, "01J%023d", sequence));
    }

    public static String correlationId(String scenario) {
        if (scenario == null || !scenario.matches("[A-Z0-9]+(?:-[A-Z0-9]+)*")) {
            throw new IllegalArgumentException(
                    "Test correlation scenario must use uppercase letters, digits and hyphens");
        }
        String value = "F009-" + scenario;
        if (value.length() > 128) {
            throw new IllegalArgumentException("Test correlation ID must not exceed 128 characters");
        }
        return value;
    }
}
