package com.cryptostrategy.platform.search.api.model;

public enum ReproductionVerificationStatus {
    PENDING,
    RUNNING,
    MATCHED,
    MISMATCHED,
    FAILED;

    public boolean isTerminal() {
        return this == MATCHED || this == MISMATCHED || this == FAILED;
    }
}
