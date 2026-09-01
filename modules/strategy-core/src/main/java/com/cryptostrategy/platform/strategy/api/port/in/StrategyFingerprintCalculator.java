package com.cryptostrategy.platform.strategy.api.port.in;

import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.List;

public interface StrategyFingerprintCalculator {
    String single(StrategyReference reference, StrategyParameterSet parameters);
    String composite(CombinationPolicyId policyId, SemanticVersion policyVersion,
            StrategyParameterSet policyParameters, List<Component> components);

    record Component(StrategyReference reference, StrategyParameterSet parameters) { }
}
