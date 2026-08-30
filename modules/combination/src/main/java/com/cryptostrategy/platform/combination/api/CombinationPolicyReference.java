package com.cryptostrategy.platform.combination.api;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import java.util.Objects;
public record CombinationPolicyReference(CombinationPolicyId policyId, SemanticVersion version) { public CombinationPolicyReference { Objects.requireNonNull(policyId); Objects.requireNonNull(version); } }
