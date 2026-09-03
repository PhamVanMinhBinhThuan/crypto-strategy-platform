package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

/** Search-owned typed reference to an immutable Candidate aggregate. */
public record SearchCandidateId(String value) implements UlidIdentifier {
    public SearchCandidateId { Ulids.requireValid(value); }
}
