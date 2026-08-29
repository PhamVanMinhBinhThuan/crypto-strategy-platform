package com.cryptostrategy.platform.marketdata.internal.provider.binance.transport;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface BinanceRestTransport {
    Response getKlines(String symbol, String interval, long startTime, long endTime, int limit);
    record Response(int status, String body, Map<String, List<String>> headers) { public Response { headers = Map.copyOf(headers); } }
    URI baseUri();
}
