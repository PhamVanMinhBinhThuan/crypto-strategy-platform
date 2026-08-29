package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;

@FunctionalInterface public interface SubscribeCandlesUseCase { CandleSubscription subscribeCandles(RealtimeCandleQuery query, CandleUpdateHandler updates, ConnectionStateHandler states); }
