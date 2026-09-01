package com.cryptostrategy.platform.backtesting.api;

import com.cryptostrategy.platform.backtesting.api.port.out.BacktestResultStore;
import com.cryptostrategy.platform.backtesting.api.port.out.FrozenStrategyResolver;
import com.cryptostrategy.platform.backtesting.internal.RunBacktestService;
import com.cryptostrategy.platform.experiment.api.port.in.GetFrozenBacktestExecutionUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.VerifyDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;

public final class BacktestingModuleFactory {
    private BacktestingModuleFactory() {}

    public static RunBacktestService runBacktestService(
            GetFrozenBacktestExecutionUseCase frozenExecutionUseCase,
            GetDatasetUseCase getDatasetUseCase,
            VerifyDatasetUseCase verifyDatasetUseCase,
            DatasetCandleReader candleReader,
            FrozenStrategyResolver strategyResolver,
            BacktestResultStore backtestResultStore
    ) {
        return new RunBacktestService(
                frozenExecutionUseCase,
                getDatasetUseCase,
                verifyDatasetUseCase,
                candleReader,
                strategyResolver,
                backtestResultStore
        );
    }
}
