import { describe, expect, it } from "vitest";
import { emptyCandleState, mergeCandles } from "@/src/features/market/state/candle-reducer";
import { candlePageFixture } from "../fixtures/f012/public-contract";
describe("Candle reducer", () => {
  it("deduplicates identity and rejects stale/open overwrite of closed Candle", () => {
    const candle = { ...candlePageFixture.items[0] };
    let state = mergeCandles(emptyCandleState, [candle], "2026-09-03T01:00:02Z");
    state = mergeCandles(state, [{ ...candle, close: "1", closed: false }], "2026-09-03T01:00:01Z");
    expect(state.items).toHaveLength(1);
    expect(state.items[0].close).toBe("100750.00");
  });
  it("orders and bounds the window", () => {
    const items = Array.from({ length: 205 }, (_, i) => ({
      ...candlePageFixture.items[0],
      openTime: new Date(Date.UTC(2026, 0, 1, i)).toISOString()
    }));
    expect(mergeCandles(emptyCandleState, items).items).toHaveLength(200);
  });
});
