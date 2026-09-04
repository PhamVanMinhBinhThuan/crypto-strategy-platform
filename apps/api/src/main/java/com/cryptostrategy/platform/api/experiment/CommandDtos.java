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
import com.fasterxml.jackson.annotation.JsonValue;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.io.IOException;
import java.io.Serial;

public final class CommandDtos {
    private CommandDtos() {}

    public record StartBacktestRequest(
            @JsonDeserialize(using = DatasetVersionIdDeserializer.class) DatasetVersionId datasetId,
            StrategyDtos.StrategySelectionRequest strategy,
            BacktestConfigurationRequest configuration) {}

    public record StartExperimentRequest(
            String name,
            @JsonDeserialize(using = DatasetVersionIdDeserializer.class) DatasetVersionId datasetId,
            GeneratorSelectionRequest generator,
            @JsonDeserialize(using = UserStrategyVersionIdDeserializer.class) UserStrategyVersionId userStrategyVersionId,
            SearchSpaceRequest searchSpace,
            StopConditionRequest stopCondition,
            Integer topK) {
        public StartExperimentRequest {
            topK = topK == null ? 10 : topK;
        }
    }

    public record GeneratorSelectionRequest(
            GeneratorId generatorId,
            String version,
            Long seed) {}

    public record SearchSpaceRequest(
            @JsonDeserialize(using = StrategyPluginIdDeserializer.class) StrategyPluginId strategyId,
            String strategyVersion,
            java.util.Map<String, ParameterRangeRequest> parameters) {}

    public record ParameterRangeRequest(
            Long minimum,
            Long maximum,
            java.util.List<String> options) {}

    public record StopConditionRequest(
            Integer maximumCandidates,
            Integer maximumDurationSeconds) {}

    public record ReproduceExperimentRequest(
            String name) {}

    public record ExperimentAcceptedResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            @JsonSerialize(using = TypedUlidSerializer.class) JobId jobId,
            String status) {
        public static ExperimentAcceptedResponse from(
                com.cryptostrategy.platform.experiment.api.Experiment experiment, JobId jobId) {
            return new ExperimentAcceptedResponse(
                    experiment.experimentId(),
                    jobId,
                    "QUEUED"); // F-005 FreezeExperimentUseCase returns JobId, always queued initially
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
