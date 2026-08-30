package com.cryptostrategy.platform.experiment.api.job;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record JobId(String value) implements UlidIdentifier {
    public JobId {
        value = Ulids.requireValid(value);
    }

    public static JobId generate() {
        return new JobId(Ulids.generate());
    }

    @Override
    public String toString() {
        return value;
    }
}
