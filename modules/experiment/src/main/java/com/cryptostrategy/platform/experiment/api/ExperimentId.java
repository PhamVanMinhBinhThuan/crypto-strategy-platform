package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record ExperimentId(String value) implements UlidIdentifier {
    public ExperimentId {
        value = Ulids.requireValid(value);
    }

    public static ExperimentId generate() {
        return new ExperimentId(Ulids.generate());
    }

    @Override
    public String toString() {
        return value;
    }
}
