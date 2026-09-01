package com.cryptostrategy.platform.strategy.internal.fingerprint;

import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CanonicalStrategyEncoder {
    public byte[] encodeSingle(StrategyReference reference, StrategyParameterSet parameters) {
        StringBuilder value = new StringBuilder("single|").append(field(reference.strategyVersionId().value()))
                .append(field(reference.pluginId().value())).append(field(reference.implementationVersion().toString()));
        parameters.values().forEach((name, parameter) -> value.append(field(name)).append(field(parameter.type().name())).append(field(parameter.canonicalText())));
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }
    public String encodePolicy(com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId policyId,
                               com.cryptostrategy.platform.strategy.api.model.SemanticVersion version,
                               StrategyParameterSet parameters) {
        StringBuilder value = new StringBuilder("policy|").append(field(policyId.value())).append(field(version.toString()));
        parameters.values().forEach((name, parameter) -> value.append(field(name)).append(field(parameter.type().name())).append(field(parameter.canonicalText())));
        return value.toString();
    }
    public byte[] encodeComposite(String policy, List<byte[]> components) {
        List<String> ordered = components.stream().map(bytes -> java.util.HexFormat.of().formatHex(bytes)).toList();
        StringBuilder value = new StringBuilder("composite|").append(field(policy)); ordered.forEach(component -> value.append(field(component)));
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static String field(String value) { return value.length() + ":" + value + "|"; }
}
