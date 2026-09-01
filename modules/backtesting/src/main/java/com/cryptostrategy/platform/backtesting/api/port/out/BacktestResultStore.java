package com.cryptostrategy.platform.backtesting.api.port.out;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
public interface BacktestResultStore { BacktestResult save(BacktestResult result); }
