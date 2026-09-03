package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record SearchRunId(String value) implements UlidIdentifier {
    public SearchRunId {
        value = Ulids.requireValid(value);
    }

    public static SearchRunId generate() {
        return new SearchRunId(Ulids.generate());
    }
}
