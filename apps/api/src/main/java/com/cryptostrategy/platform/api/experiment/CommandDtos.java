package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.strategy.StrategyDtos;
import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktestAcceptance;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonValue;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.execution.api.SearchRunReferenceId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.io.IOException;
import java.io.Serial;
import java.math.BigDecimal;

public final class CommandDtos {
    private CommandDtos() {}

    public record StartBacktestRequest(
            @JsonDeserialize(using = DatasetVersionIdDeserializer.class) DatasetVersionId datasetId,
            StrategyDtos.StrategySelectionRequest strategy,
            BacktestConfigurationRequest configuration) {}

    public record StartExperimentRequest(
            Integer configurationVersion,
            String name,
            @JsonDeserialize(using = DatasetVersionIdDeserializer.class) DatasetVersionId datasetId,
            GeneratorSelectionRequest generator,
            @JsonDeserialize(using = UserStrategyVersionIdDeserializer.class) UserStrategyVersionId userStrategyVersionId,
            SearchSpaceRequest searchSpace,
            StopConditionRequest stopCondition,
            StopConditionRequest stopConditions,
            Integer requestedConcurrency,
            Integer topK,
            SearchBacktestConfigurationRequest backtestConfiguration) {
        public StartExperimentRequest {
            topK = topK == null ? 10 : topK;
        }

        public StartExperimentRequest(Integer configurationVersion, String name,
                DatasetVersionId datasetId, GeneratorSelectionRequest generator,
                UserStrategyVersionId userStrategyVersionId, SearchSpaceRequest searchSpace,
                StopConditionRequest stopCondition, StopConditionRequest stopConditions,
                Integer requestedConcurrency, Integer topK) {
            this(configurationVersion, name, datasetId, generator, userStrategyVersionId,
                    searchSpace, stopCondition, stopConditions, requestedConcurrency, topK, null);
        }

        public StartExperimentRequest(String name, DatasetVersionId datasetId,
                GeneratorSelectionRequest generator, UserStrategyVersionId userStrategyVersionId,
                SearchSpaceRequest searchSpace, StopConditionRequest stopCondition, Integer topK) {
            this(null, name, datasetId, generator, userStrategyVersionId, searchSpace,
                    stopCondition, null, null, topK, null);
        }
    }

    public record GeneratorSelectionRequest(
            GeneratorId generatorId,
            String version,
            Long seed) {}

    public record SearchSpaceRequest(
            Integer schemaVersion,
            @JsonDeserialize(using = StrategyPluginIdDeserializer.class) StrategyPluginId strategyId,
            String strategyVersion,
            java.util.Map<String, ParameterRangeRequest> parameters,
            java.util.List<StrategyPoolEntryRequest> strategyPool,
            @JsonAlias("minimumComponents") Integer minComponents,
            @JsonAlias("maximumComponents") Integer maxComponents,
            CombinationPolicyRequest combinationPolicy,
            java.util.List<ConstraintRequest> constraints) {
        public SearchSpaceRequest(StrategyPluginId strategyId, String strategyVersion,
                java.util.Map<String, ParameterRangeRequest> parameters) {
            this(null, strategyId, strategyVersion, parameters, java.util.List.of(),
                    null, null, null, java.util.List.of());
        }
    }

    public record StrategyPoolEntryRequest(
            String artifactType,
            @JsonDeserialize(using = StrategyPluginIdDeserializer.class) StrategyPluginId strategyId,
            @JsonAlias("strategyVersion") String version,
            @JsonDeserialize(using = UserStrategyVersionIdDeserializer.class)
            UserStrategyVersionId userStrategyVersionId,
            @JsonAlias("parameters") java.util.Map<String, ParameterRangeRequest> parameterDomains) {}

    public record CombinationPolicyRequest(CombinationPolicyId policyId, String version,
            java.util.Map<String, Object> configuration) {}

    public record ConstraintRequest(String kind, String left, String right) {}

    public record ParameterRangeRequest(
            String kind,
            @JsonAlias("minimum") BigDecimal min,
            @JsonAlias("maximum") BigDecimal max,
            BigDecimal step,
            @JsonAlias("options") java.util.List<String> values) {
        public ParameterRangeRequest(Long minimum, Long maximum, java.util.List<String> options) {
            this(null, decimal(minimum), decimal(maximum), null, options);
        }

        private static BigDecimal decimal(Long value) {
            return value == null ? null : BigDecimal.valueOf(value);
        }
    }

    public record StopConditionRequest(
            Integer maximumCandidates,
            Integer maximumDurationSeconds,
            Integer maximumWithoutImprovement) {
        public StopConditionRequest(Integer maximumCandidates, Integer maximumDurationSeconds) {
            this(maximumCandidates, maximumDurationSeconds, null);
        }
    }

    public record ReproduceExperimentRequest(
            String name) {}

    public record ExperimentAcceptedResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            @JsonSerialize(using = TypedUlidSerializer.class) JobId jobId,
            @JsonSerialize(using = TypedUlidSerializer.class)
            SearchRunReferenceId searchRunId,
            String status,
            Integer configurationVersion,
            String configurationFingerprint,
            String monitorPath) {
        public ExperimentAcceptedResponse(ExperimentId experimentId, JobId jobId, String status) {
            this(experimentId, jobId, null, status, null, null,
                    "/search/" + experimentId.value());
        }
        public static ExperimentAcceptedResponse from(
                com.cryptostrategy.platform.experiment.api.Experiment experiment, JobId jobId) {
            return new ExperimentAcceptedResponse(experiment.experimentId(), jobId, "QUEUED");
        }
    }

    public record BacktestConfigurationRequest(
            String initialCapital,
            String feeRate,
            String slippageRate,
            String positionMode,
            String executionPriceRule,
            Boolean forceCloseAtEnd,
            String roundingMode) {
        public BacktestConfigurationRequest {
            slippageRate = slippageRate == null ? "0" : slippageRate;
            forceCloseAtEnd = forceCloseAtEnd == null ? Boolean.TRUE : forceCloseAtEnd;
            roundingMode = roundingMode == null ? "HALF_EVEN" : roundingMode;
        }
    }

    public record BacktestAcceptedResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) BacktestId backtestId,
            @JsonSerialize(using = TypedUlidSerializer.class) JobId jobId,
            String status) {
        public static BacktestAcceptedResponse from(
                StandaloneBacktestAcceptance acceptance) {
            return new BacktestAcceptedResponse(
                    acceptance.backtest().backtestId(),
                    acceptance.jobId(),
                    acceptance.acceptedStatus().name());
        }
    }

    public record GeneratorId(String value) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public GeneratorId {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("generatorId is required");
            }
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public record SearchBacktestConfigurationRequest(
            String initialCapital,
            String feeRate,
            String slippageRate) {}

    public record CombinationPolicyId(String value) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public CombinationPolicyId {
            if (value == null || !value.matches("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$")) {
                throw new IllegalArgumentException("combinationPolicyId must be a lowercase slug");
            }
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public static final class DatasetVersionIdDeserializer extends StdDeserializer<DatasetVersionId> {
        @Serial private static final long serialVersionUID = 1L;
        public DatasetVersionIdDeserializer() { super(DatasetVersionId.class); }
        @Override public DatasetVersionId deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return new DatasetVersionId(parser.getValueAsString());
        }
    }

    public static final class StrategyPluginIdDeserializer extends StdDeserializer<StrategyPluginId> {
        @Serial private static final long serialVersionUID = 1L;
        public StrategyPluginIdDeserializer() { super(StrategyPluginId.class); }
        @Override public StrategyPluginId deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return new StrategyPluginId(parser.getValueAsString());
        }
    }

    public static final class UserStrategyVersionIdDeserializer extends StdDeserializer<UserStrategyVersionId> {
        @Serial private static final long serialVersionUID = 1L;
        public UserStrategyVersionIdDeserializer() { super(UserStrategyVersionId.class); }
        @Override public UserStrategyVersionId deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return new UserStrategyVersionId(parser.getValueAsString());
        }
    }
}
