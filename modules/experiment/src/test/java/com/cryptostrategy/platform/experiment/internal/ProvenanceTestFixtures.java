package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

final class ProvenanceTestFixtures {
    static final String STRATEGY_FINGERPRINT = "strategy-v1:sha256:" + "a".repeat(64);
    private static final String STRATEGY_VERSION_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAY";
    private ProvenanceTestFixtures() {}

    static StrategyProvenanceSnapshot single(String pluginId, String version, Map<String, ?> parameters, String sourceVersionId) {
        return StrategyProvenanceSnapshot.single(
                new StrategyReference(new StrategyVersionId(STRATEGY_VERSION_ID), new StrategyPluginId(pluginId), normalizeVersion(version)),
                parameters(parameters), Optional.ofNullable(sourceVersionId).map(UserStrategyVersionId::new), STRATEGY_FINGERPRINT);
    }

    static StrategyParameterSet parameters(Map<String, ?> input) {
        Map<String, StrategyParameterValue> values = new TreeMap<>();
        input.forEach((name, value) -> values.put(name, switch (value) {
            case Integer integer -> new StrategyParameterValue.IntegerValue(integer.longValue());
            case Long longValue -> new StrategyParameterValue.IntegerValue(longValue);
            case BigDecimal decimal -> new StrategyParameterValue.DecimalValue(decimal);
            case Boolean bool -> new StrategyParameterValue.BooleanValue(bool);
            default -> new StrategyParameterValue.TextValue(value.toString());
        }));
        return StrategyParameterSet.of(values);
    }

    static StrategyReference reference(String pluginId) {
        return new StrategyReference(new StrategyVersionId(STRATEGY_VERSION_ID), new StrategyPluginId(pluginId), new SemanticVersion(1, 0, 0));
    }

    private static SemanticVersion normalizeVersion(String version) {
        return version.matches("\\d+\\.\\d+") ? SemanticVersion.parse(version + ".0") : SemanticVersion.parse(version);
    }
}
