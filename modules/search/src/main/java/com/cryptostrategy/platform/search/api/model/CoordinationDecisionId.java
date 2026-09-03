package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record CoordinationDecisionId(String value) implements UlidIdentifier {
    public CoordinationDecisionId {
        value = Ulids.requireValid(value);
    }

    public static CoordinationDecisionId generate() {
        return new CoordinationDecisionId(Ulids.generate());
    }
}
