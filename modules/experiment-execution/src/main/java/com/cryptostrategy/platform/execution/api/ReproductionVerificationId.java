package com.cryptostrategy.platform.execution.api;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

/** Canonical execution identity for a durable linked-reproduction verification. */
public record ReproductionVerificationId(String value) implements UlidIdentifier {
    public ReproductionVerificationId {
        value = Ulids.requireValid(value);
    }

    public static ReproductionVerificationId generate() {
        return new ReproductionVerificationId(Ulids.generate());
    }
}
