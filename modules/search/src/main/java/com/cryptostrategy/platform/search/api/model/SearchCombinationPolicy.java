package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.Objects;

/** Immutable combination-policy selection included in Search-space and candidate identity. */
public record SearchCombinationPolicy(
        CombinationPolicyId policyId,
        SemanticVersion version,
        StrategyParameterSet parameters) {
    public static final CombinationPolicyId MAJORITY_VOTE = new CombinationPolicyId("majority-vote");
    public static final SemanticVersion MAJORITY_VOTE_V1 = SemanticVersion.parse("1.0.0");

    public SearchCombinationPolicy {
        Objects.requireNonNull(policyId, "policyId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(parameters, "parameters");
        if (!MAJORITY_VOTE.equals(policyId) || !MAJORITY_VOTE_V1.equals(version)) {
            throw new IllegalArgumentException("Unsupported Search combination policy");
        }
        if (!parameters.values().isEmpty()) {
            throw new IllegalArgumentException("Majority Vote does not accept policy parameters");
        }
    }

    public static SearchCombinationPolicy majorityVote() {
        return new SearchCombinationPolicy(MAJORITY_VOTE, MAJORITY_VOTE_V1, StrategyParameterSet.empty());
    }
}
