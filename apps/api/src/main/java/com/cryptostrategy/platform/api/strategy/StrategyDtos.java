package com.cryptostrategy.platform.api.strategy;

import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterDefinition;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.StrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyDetails;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategySummary;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyVersion;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class StrategyDtos {
    private StrategyDtos() {}

    public record StrategyPage(
            List<StrategyDescriptorResponse> items,
            String nextCursor,
            boolean hasMore) {
        public StrategyPage {
            items = List.copyOf(items);
        }
    }

    public record StrategyDescriptorResponse(
            @JsonSerialize(using = ToStringSerializer.class) StrategyPluginId strategyId,
            @JsonSerialize(using = TypedUlidSerializer.class) StrategyVersionId strategyVersionId,
            String version,
            String contractVersion,
            String displayName,
            String description,
            String category,
            List<String> supportedSignals,
            int requiredLookback,
            List<ParameterRuleResponse> parameters,
            List<CrossParameterRuleResponse> constraints,
            String descriptorFingerprint) {
        static StrategyDescriptorResponse from(StrategyDescriptor descriptor) {
            return new StrategyDescriptorResponse(
                    descriptor.reference().pluginId(),
                    descriptor.reference().strategyVersionId(),
                    descriptor.reference().implementationVersion().toString(),
                    descriptor.contractVersion(),
                    descriptor.displayName(),
                    descriptor.description(),
                    descriptor.category(),
                    descriptor.supportedSignals().stream()
                            .map(Enum::name)
                            .sorted()
                            .toList(),
                    descriptor.requiredLookback(),
                    descriptor.parameterSchema().definitions().stream()
                            .map(ParameterRuleResponse::from)
                            .toList(),
                    descriptor.parameterSchema().constraints().stream()
                            .map(value -> new CrossParameterRuleResponse(
                                    value.lowerParameter(), value.upperParameter()))
                            .toList(),
                    descriptor.descriptorFingerprint());
        }
    }

    public record ParameterRuleResponse(
            String name,
            String type,
            boolean required,
            String defaultValue,
            String minimum,
            String maximum,
            List<String> allowedValues,
            String description) {
        static ParameterRuleResponse from(ParameterDefinition definition) {
            return new ParameterRuleResponse(
                    definition.name(),
                    definition.type().name(),
                    definition.required(),
                    definition.defaultValue()
                            .map(value -> value.canonicalText())
                            .orElse(null),
                    definition.minimum().map(value -> value.toPlainString()).orElse(null),
                    definition.maximum().map(value -> value.toPlainString()).orElse(null),
                    definition.allowedValues().stream().sorted().toList(),
                    definition.description());
        }
    }

    public record CrossParameterRuleResponse(
            String lowerParameter, String upperParameter) {}

    public record StrategySelectionRequest(
            @JsonProperty("strategyId") String strategyPlugin,
            String version,
            Map<String, JsonNode> parameters) {
        public StrategySelectionRequest {
            parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        }
    }

    public record StrategySourceRequest(
            String type,
            StrategySelectionRequest strategy,
            @JsonProperty("policyId") String policy,
            String policyVersion,
            Map<String, JsonNode> policyParameters,
            List<StrategySelectionRequest> components) {
        public StrategySourceRequest {
            policyParameters = policyParameters == null ? Map.of() : Map.copyOf(policyParameters);
            components = components == null ? List.of() : List.copyOf(components);
        }
    }

    public record CreateUserStrategyRequest(
            String name, String description, String kind, StrategySourceRequest source) {}

    public record CreateUserStrategyVersionRequest(
            int expectedLatestVersionNo, StrategySourceRequest source) {}

    public record PublishUserStrategyVersionRequest(int expectedVersionNo) {}

    public record StrategySelectionResponse(
            @JsonSerialize(using = ToStringSerializer.class) StrategyPluginId strategyId,
            @JsonSerialize(using = TypedUlidSerializer.class) StrategyVersionId strategyVersionId,
            String version,
            SortedMap<String, String> parameters) {
        static StrategySelectionResponse from(SingleStrategyDraftSource source) {
            return from(source.strategyReference(), source.parameters());
        }

        static StrategySelectionResponse from(
                com.cryptostrategy.platform.strategy.api.model.StrategyReference reference,
                StrategyParameterSet parameters) {
            return new StrategySelectionResponse(
                    reference.pluginId(),
                    reference.strategyVersionId(),
                    reference.implementationVersion().toString(),
                    parameterValues(parameters));
        }
    }

    public sealed interface StrategySourceResponse
            permits SingleStrategySourceResponse, CompositeStrategySourceResponse {}

    public record SingleStrategySourceResponse(
            String type, StrategySelectionResponse strategy)
            implements StrategySourceResponse {
        static SingleStrategySourceResponse from(SingleStrategyDraftSource source) {
            return new SingleStrategySourceResponse(
                    "SINGLE", StrategySelectionResponse.from(source));
        }
    }

    public record CompositeStrategySourceResponse(
            String type,
            @JsonSerialize(using = ToStringSerializer.class) CombinationPolicyId policyId,
            String policyVersion,
            SortedMap<String, String> policyParameters,
            List<StrategySelectionResponse> components)
            implements StrategySourceResponse {
        static CompositeStrategySourceResponse from(CompositeStrategyDraftSource source) {
            return new CompositeStrategySourceResponse(
                    "COMPOSITE",
                    source.policyId(),
                    source.policyVersion().toString(),
                    parameterValues(source.policyParameters()),
                    source.components().stream()
                            .map(component -> StrategySelectionResponse.from(
                                    component.strategyReference(), component.parameters()))
                            .toList());
        }
    }

    public record UserStrategyVersionResponse(
            @JsonSerialize(using = TypedUlidSerializer.class)
                    UserStrategyVersionId userStrategyVersionId,
            @JsonSerialize(using = TypedUlidSerializer.class) UserStrategyId userStrategyId,
            int versionNo,
            String kind,
            StrategySourceResponse source,
            String status,
            String fingerprint,
            Instant publishedAt,
            Instant createdAt) {
        static UserStrategyVersionResponse from(UserStrategyVersion version) {
            return new UserStrategyVersionResponse(
                    version.id(),
                    version.userStrategyId(),
                    version.versionNo(),
                    version.kind().name(),
                    StrategyDtos.source(version.source()),
                    version.status().name(),
                    version.fingerprint(),
                    version.publishedAt().orElse(null),
                    version.createdAt());
        }
    }

    public record UserStrategyResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) UserStrategyId userStrategyId,
            String kind,
            String name,
            String description,
            String status,
            Instant archivedAt,
            Instant createdAt,
            Instant updatedAt,
            UserStrategyVersionResponse latestVersion) {
        static UserStrategyResponse from(UserStrategyDetails details) {
            var strategy = details.strategy();
            return new UserStrategyResponse(
                    strategy.id(),
                    strategy.kind().name(),
                    strategy.name(),
                    strategy.description(),
                    strategy.status().name(),
                    strategy.archivedAt().orElse(null),
                    strategy.createdAt(),
                    strategy.updatedAt(),
                    UserStrategyVersionResponse.from(details.latestVersion()));
        }
    }

    public record UserStrategySummaryResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) UserStrategyId userStrategyId,
            String kind,
            String name,
            String description,
            Instant createdAt) {
        static UserStrategySummaryResponse from(UserStrategySummary summary) {
            return new UserStrategySummaryResponse(
                    summary.id(),
                    summary.kind().name(),
                    summary.name(),
                    summary.description(),
                    summary.createdAt());
        }
    }

    public record UserStrategyPage(
            List<UserStrategySummaryResponse> items,
            String nextCursor,
            boolean hasMore) {
        public UserStrategyPage {
            items = List.copyOf(items);
        }
    }

    private static StrategySourceResponse source(StrategyDraftSource source) {
        if (source instanceof SingleStrategyDraftSource single) {
            return SingleStrategySourceResponse.from(single);
        }
        return CompositeStrategySourceResponse.from((CompositeStrategyDraftSource) source);
    }

    private static SortedMap<String, String> parameterValues(
            StrategyParameterSet parameters) {
        TreeMap<String, String> values = new TreeMap<>();
        parameters.values().forEach(
                (name, value) -> values.put(name, value.canonicalText()));
        return java.util.Collections.unmodifiableSortedMap(values);
    }
}
