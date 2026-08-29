package com.cryptostrategy.platform.marketdata.internal.provider.binance.transport;

import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class JdkBinanceStreamTransport implements BinanceStreamTransport {
    private final URI baseUri; private final HttpClient client;
    public JdkBinanceStreamTransport(URI baseUri, HttpClient client) {
        if (!"wss".equalsIgnoreCase(baseUri.getScheme())) throw new IllegalArgumentException("Binance stream URL must use WSS");
        this.baseUri = baseUri; this.client = client;
    }
    @Override public CandleSubscription subscribe(String streamName, Consumer<String> frames, Runnable disconnected) {
        Listener listener = new Listener(frames, disconnected);
        try {
            WebSocket socket = client.newWebSocketBuilder().buildAsync(baseUri.resolve("/ws/" + streamName), listener).join();
            return () -> socket.sendClose(WebSocket.NORMAL_CLOSURE, "closed");
        } catch (RuntimeException exception) { throw new MarketDataException(MarketDataErrorCode.MARKET_PROVIDER_UNAVAILABLE, "Provider stream unavailable", Map.of(), exception); }
    }
    private static final class Listener implements WebSocket.Listener {
        private final Consumer<String> frames; private final Runnable disconnected; private final StringBuilder text = new StringBuilder();
        private Listener(Consumer<String> frames, Runnable disconnected) { this.frames = frames; this.disconnected = disconnected; }
        @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            text.append(data); if (last) { String frame = text.toString(); text.setLength(0); frames.accept(frame); } webSocket.request(1); return null;
        }
        @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) { disconnected.run(); return null; }
        @Override public void onError(WebSocket webSocket, Throwable error) { disconnected.run(); }
    }
}
