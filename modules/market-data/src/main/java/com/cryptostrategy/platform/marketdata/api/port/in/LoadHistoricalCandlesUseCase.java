package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;

@FunctionalInterface public interface LoadHistoricalCandlesUseCase { HistoricalCandleBatch loadHistoricalCandles(HistoricalCandleQuery query); }
