package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.strategy.StrategyDtos;
import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktestAcceptance;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public final class CommandDtos {
    private CommandDtos() {}

    public record StartBacktestRequest(
            String datasetId,
            StrategyDtos.StrategySelectionRequest strategy,
            BacktestConfigurationRequest configuration) {}

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
}
