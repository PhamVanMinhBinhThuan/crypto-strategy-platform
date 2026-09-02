package com.cryptostrategy.platform.api.market;

import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.api.transport.PageResponseMapper;
import com.cryptostrategy.platform.marketdata.api.port.in.LoadHistoricalCandlesUseCase;
import java.time.DateTimeException;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candles")
public final class MarketController {
    private final LoadHistoricalCandlesUseCase historical;
    private final MarketRequestMapper requests;
    private final PageRequestMapper pageRequests;
    private final PageResponseMapper pageResponses;

    public MarketController(
            LoadHistoricalCandlesUseCase historical,
            MarketRequestMapper requests,
            PageRequestMapper pageRequests,
            PageResponseMapper pageResponses) {
        this.historical = historical;
        this.requests = requests;
        this.pageRequests = pageRequests;
        this.pageResponses = pageResponses;
    }

    @GetMapping
    public MarketDtos.CandlePage list(
            @RequestParam String pair,
            @RequestParam String timeframe,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        var page = pageRequests.map(limit, cursor);
        var range = requests.range(pair, timeframe, startTime, endTime);
        Instant pageStart = page.cursor()
                .map(value -> MarketCursor.decode(value, range))
                .orElse(startTime);
        Instant pageEnd = fetchEnd(range, pageStart, page.limit() + 1L);
        var candles = historical.loadHistoricalCandles(
                        requests.query(range, pageStart, pageEnd))
                .candles();
        var response = pageResponses.mapLookahead(
                candles,
                page.limit(),
                candle -> MarketCursor.encode(
                        range.timeframe().next(candle.key().openTime()), range));
        return new MarketDtos.CandlePage(
                response.items().stream().map(MarketDtos.CandleResponse::from).toList(),
                response.nextCursor(),
                response.hasMore());
    }

    private static Instant fetchEnd(
            MarketRequestMapper.MarketRange range, Instant pageStart, long intervals) {
        try {
            Instant bounded = pageStart.plus(range.timeframe().duration().multipliedBy(intervals));
            return bounded.isBefore(range.endTime()) ? bounded : range.endTime();
        } catch (ArithmeticException | DateTimeException exception) {
            throw new IllegalArgumentException("Market range exceeds supported bounds", exception);
        }
    }
}
