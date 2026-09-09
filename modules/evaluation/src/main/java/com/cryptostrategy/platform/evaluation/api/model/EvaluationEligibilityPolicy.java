package com.cryptostrategy.platform.evaluation.api.model;

/** Shared eligibility policy used by evaluation and read projections. */
public final class EvaluationEligibilityPolicy {
    public static final int MINIMUM_TRADES = 5;

    private EvaluationEligibilityPolicy() {}

    public static boolean isEligible(int numberOfTrades) {
        return numberOfTrades >= MINIMUM_TRADES;
    }
}
