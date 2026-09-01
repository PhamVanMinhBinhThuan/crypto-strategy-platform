package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.error.*;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.marketdata.api.model.DatasetMembership;

final class BacktestInputValidator {
    void validate(ResolvedBacktestRun command, Candle candle, DatasetMembership member,
            int expectedSequence, Candle previous) {
        if (member.sequenceNo() != expectedSequence
                || !member.datasetVersionId().equals(command.dataset().datasetVersionId())) {
            throw new BacktestException(BacktestErrorCode.INVALID_BATCH, "Dataset membership mismatch");
        }
        if (!candle.closed() || !candle.key().tradingPair().equals(command.dataset().tradingPair())
                || candle.key().timeframe() != command.dataset().timeframe()) {
            throw new BacktestException(BacktestErrorCode.INVALID_CANDLE, "Candle outside frozen Dataset identity");
        }
        if (previous != null && !candle.key().openTime().isAfter(previous.key().openTime())) {
            throw new BacktestException(BacktestErrorCode.INVALID_CANDLE, "Duplicate or out-of-order Candle");
        }
    }
}
