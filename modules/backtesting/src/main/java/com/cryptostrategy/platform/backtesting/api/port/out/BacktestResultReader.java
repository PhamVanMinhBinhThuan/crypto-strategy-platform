package com.cryptostrategy.platform.backtesting.api.port.out;
import com.cryptostrategy.platform.backtesting.api.model.*;import java.util.Optional;
public interface BacktestResultReader { Optional<BacktestResult> findById(BacktestResultId id); }
