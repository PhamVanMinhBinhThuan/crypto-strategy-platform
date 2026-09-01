package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.model.*;
import java.time.Instant;
import java.util.Objects;

record Position(Instant entryTime, Money entryPrice, Quantity quantity, Money entryFee, Money entryNotional) {
    Position { Objects.requireNonNull(entryTime); Objects.requireNonNull(entryPrice); Objects.requireNonNull(quantity);
        Objects.requireNonNull(entryFee); Objects.requireNonNull(entryNotional); }
}
