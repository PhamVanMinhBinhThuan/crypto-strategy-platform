import type { MarketTimeframe } from "./market-catalog";

const TIMEFRAME_SECONDS: Record<MarketTimeframe, number> = {
  "5m": 5 * 60,
  "15m": 15 * 60,
  "1h": 60 * 60,
  "4h": 4 * 60 * 60
};

const MARKET_WINDOW_MILLISECONDS = 24 * 60 * 60 * 1000;

/**
 * F-009 only accepts candle ranges whose endpoints are exact timeframe boundaries.
 * Keep that transport constraint in the market model instead of relying on wall-clock
 * seconds/milliseconds from Date.
 */
export function marketRangeEndingAt(now: Date, timeframe: MarketTimeframe) {
  const intervalMilliseconds = TIMEFRAME_SECONDS[timeframe] * 1000;
  const alignedEndMilliseconds = Math.floor(now.getTime() / intervalMilliseconds) * intervalMilliseconds;
  const alignedStartMilliseconds = alignedEndMilliseconds - MARKET_WINDOW_MILLISECONDS;

  return {
    startTime: new Date(alignedStartMilliseconds).toISOString(),
    endTime: new Date(alignedEndMilliseconds).toISOString()
  } as const;
}
