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
});
