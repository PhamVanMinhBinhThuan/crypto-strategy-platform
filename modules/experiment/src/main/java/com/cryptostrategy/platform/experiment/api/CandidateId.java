package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record CandidateId(String value) implements UlidIdentifier {
    public CandidateId {
        value = Ulids.requireValid(value);
    }

    public static CandidateId generate() {
        return new CandidateId(Ulids.generate());
    }

    @Override
    public String toString() {
        return value;
    }
}
