export const MARKET_CATALOG_VERSION = 1 as const;

export const MARKET_PAIRS = ["BTC/USDT"] as const;
export const MARKET_TIMEFRAMES = ["5m", "15m", "1h", "4h"] as const;

export type MarketPair = (typeof MARKET_PAIRS)[number];
export type MarketTimeframe = (typeof MARKET_TIMEFRAMES)[number];

export const DEFAULT_MARKET_PAIR: MarketPair = "BTC/USDT";
export const DEFAULT_MARKET_TIMEFRAMES: readonly MarketTimeframe[] = MARKET_TIMEFRAMES;

export function isMarketPair(value: string): value is MarketPair {
  return MARKET_PAIRS.some((pair) => pair === value);
}

export function isMarketTimeframe(value: string): value is MarketTimeframe {
  return MARKET_TIMEFRAMES.some((timeframe) => timeframe === value);
}
