package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

/** Search-owned typed reference to a logical Job aggregate. */
public record SearchJobId(String value) implements UlidIdentifier {
    public SearchJobId { Ulids.requireValid(value); }
}
