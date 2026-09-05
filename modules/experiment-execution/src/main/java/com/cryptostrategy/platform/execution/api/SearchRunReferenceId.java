package com.cryptostrategy.platform.execution.api;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

/** Search-run identity projected through the Experiment Execution boundary. */
public record SearchRunReferenceId(String value) implements UlidIdentifier {
    public SearchRunReferenceId {
        value = Ulids.requireValid(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
