package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record ReproductionVerificationId(String value) implements UlidIdentifier {
    public ReproductionVerificationId {
        value = Ulids.requireValid(value);
    }

    public static ReproductionVerificationId generate() {
        return new ReproductionVerificationId(Ulids.generate());
    }
}
