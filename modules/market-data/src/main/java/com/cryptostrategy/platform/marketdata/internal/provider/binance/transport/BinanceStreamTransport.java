package com.cryptostrategy.platform.marketdata.internal.provider.binance.transport;

import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import java.util.function.Consumer;

public interface BinanceStreamTransport {
    CandleSubscription subscribe(String streamName, Consumer<String> frames, Runnable disconnected);
}
