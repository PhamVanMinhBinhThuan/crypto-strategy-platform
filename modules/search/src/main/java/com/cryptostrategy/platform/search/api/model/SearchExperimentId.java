package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

/** Search-owned typed reference to an Experiment aggregate. */
public record SearchExperimentId(String value) implements UlidIdentifier {
    public SearchExperimentId { Ulids.requireValid(value); }
}
