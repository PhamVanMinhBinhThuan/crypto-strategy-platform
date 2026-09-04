import { describe, expect, it } from "vitest";
import {
  marketSelectionQuery,
  parseMarketSelection
} from "@/src/features/market/model/market-selection";
describe("Market selection", () => {
  it("defaults invalid values and caps panels at four", () => {
    const params = new URLSearchParams(
      "pair=BTCUSDT&timeframe=bad&timeframe=5m&timeframe=15m&timeframe=1h&timeframe=4h&timeframe=1d"
    );
    const value = parseMarketSelection(params);
    expect(value.pair).toBe("BTC/USDT");
    expect(value.panels.map((p) => p.timeframe)).toEqual(["5m", "15m", "1h", "4h"]);
    expect(marketSelectionQuery(value)).toContain("pair=BTC%2FUSDT");
  });

  it("keeps four positional panels when independent timeframes are duplicated", () => {
    const params = new URLSearchParams(
      "pair=BTC%2FUSDT&timeframe=5m&timeframe=4h&timeframe=1h&timeframe=4h"
    );

    expect(parseMarketSelection(params).panels.map((panel) => panel.timeframe)).toEqual([
      "5m",
      "4h",
      "1h",
      "4h"
    ]);
  });

  it("fills omitted slots with defaults so the dashboard always has four panels", () => {
    const params = new URLSearchParams("timeframe=15m");

    expect(parseMarketSelection(params).panels.map((panel) => panel.timeframe)).toEqual([
      "15m",
      "15m",
      "1h",
      "4h"
    ]);
  });
});
