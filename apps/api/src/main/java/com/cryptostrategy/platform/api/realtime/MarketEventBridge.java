package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.in.ResolveTradingPairUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.SubscribeCandlesUseCase;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Adapts F-003 canonical realtime events into F-009 transport events. */
@Component
public final class MarketEventBridge {
    private final ResolveTradingPairUseCase pairs;
    private final SubscribeCandlesUseCase subscriptions;

    public MarketEventBridge(
            ResolveTradingPairUseCase pairs,
            SubscribeCandlesUseCase subscriptions) {
        this.pairs = Objects.requireNonNull(pairs, "pairs");
        this.subscriptions = Objects.requireNonNull(subscriptions, "subscriptions");
    }

    CandleSubscription subscribe(
            String pair,
            String timeframe,
            String correlationId,
            String subscriptionId,
            Consumer<RealtimeMessageMapper.ServerEvent> delivery) {
        String[] symbols = pair == null ? new String[0] : pair.split("/", -1);
        if (symbols.length != 2) {
            throw invalidMarket();
        }
        var resolvedPair = pairs.resolveTradingPair(
                new AssetSymbol(symbols[0]), new AssetSymbol(symbols[1]));
        Timeframe resolvedTimeframe;
        try {
            resolvedTimeframe = Timeframe.fromCode(timeframe);
        } catch (RuntimeException exception) {
            throw invalidMarket();
        }
        return subscriptions.subscribeCandles(
                new RealtimeCandleQuery(MarketProvider.BINANCE, resolvedPair, resolvedTimeframe),
                update -> {
                    var candle = update.candle();
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("pair", candle.key().tradingPair().canonicalSymbol());
                    payload.put("timeframe", candle.key().timeframe().code());
                    payload.put("openTime", candle.key().openTime());
                    payload.put("closeTime", candle.closeTime());
                    payload.put("open", candle.open().toPlainString());
                    payload.put("high", candle.high().toPlainString());
                    payload.put("low", candle.low().toPlainString());
                    payload.put("close", candle.close().toPlainString());
                    payload.put("volume", candle.volume().toPlainString());
                    payload.put("closed", candle.closed());
                    String key = candle.key().tradingPair().canonicalSymbol()
                            + "|" + candle.key().timeframe().code()
                            + "|" + candle.key().openTime();
                    delivery.accept(new RealtimeMessageMapper.ServerEvent(
                            "CANDLE_UPDATED",
                            update.providerEventTime(),
                            correlationId,
                            subscriptionId,
                            Map.copyOf(payload),
                            !candle.closed(),
                            key));
                },
                state -> delivery.accept(RealtimeMessageMapper.event(
                        "MARKET_CONNECTION_STATUS_CHANGED",
                        correlationId,
                        subscriptionId,
                        Map.of("status", state.name(), "lastSuccessfulEventAt", Instant.now()),
                        false,
                        null)));
    }

    private static RealtimeProtocolException invalidMarket() {
        return new RealtimeProtocolException(
                "INVALID_MARKET_QUERY", "The requested market subscription is invalid", false);
    }
}
