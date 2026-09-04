import {
  DEFAULT_MARKET_PAIR,
  DEFAULT_MARKET_TIMEFRAMES,
  MARKET_PAIRS,
  MARKET_TIMEFRAMES,
  type MarketPair,
  type MarketTimeframe
} from "./market-catalog";
import { canonicalEnum } from "../../shared/url-state";

export type MarketPanel = Readonly<{ id: string; timeframe: MarketTimeframe }>;
export type MarketSelection = Readonly<{ pair: MarketPair; panels: readonly MarketPanel[] }>;

export function parseMarketSelection(params: URLSearchParams): MarketSelection {
  const pair = canonicalEnum(params.get("pair"), MARKET_PAIRS, DEFAULT_MARKET_PAIR);
  const supplied = params
    .getAll("timeframe")
    .filter((value): value is MarketTimeframe =>
      MARKET_TIMEFRAMES.some((timeframe) => timeframe === value)
    )
    .slice(0, 4);
  const timeframes = DEFAULT_MARKET_TIMEFRAMES.map(
    (fallback, index) => supplied[index] ?? fallback
  );
  return {
    pair,
    panels: timeframes.map((timeframe, index) => ({ id: `panel-${index + 1}`, timeframe }))
  };
}

export function marketSelectionQuery(selection: MarketSelection) {
  const params = new URLSearchParams({ pair: selection.pair });
  selection.panels.forEach(({ timeframe }) => params.append("timeframe", timeframe));
  return params.toString();
}
