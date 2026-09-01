package com.cryptostrategy.platform.experiment.api.job;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record AttemptId(String value) implements UlidIdentifier {
    public AttemptId {
        value = Ulids.requireValid(value);
    }

    public static AttemptId generate() {
        return new AttemptId(Ulids.generate());
    }

    @Override
    public String toString() {
        return value;
    }
}
