import { describe, expect, it } from "vitest";
import { mapBacktestResult } from "@/src/features/backtests/mappers/backtest-result-mapper";
import {
  normalBacktestResult,
  zeroTradeBacktestResult
} from "@/src/features/backtests/fixtures/backtest-result-fixtures";
import { parseBacktestLookup } from "@/src/features/backtests/types/backtest-result";
describe("Backtest result contract", () => {
  it("keeps distinct identities and exactly four metrics", () => {
    expect(parseBacktestLookup({ resultId: "result-013" }).kind).toBe("resultId");
    expect(parseBacktestLookup({ backtestId: "backtest-013" }).kind).toBe("backtestId");
    expect(parseBacktestLookup({ resultId: "result-013", backtestId: "backtest-013" }).kind).toBe(
      "invalid"
    );
    const mapped = mapBacktestResult(normalBacktestResult);
    expect(Object.keys(mapped.metrics)).toEqual([
      "totalReturn",
      "winRate",
      "maximumDrawdown",
      "numberOfTrades"
    ]);
    expect(mapped.metrics.totalReturn).toBe("18.427500000000000001");
  });
  it("accepts zero trades as a valid outcome", () =>
    expect(mapBacktestResult(zeroTradeBacktestResult).trades).toEqual([]));
  it("validates UTC trades, capital, provenance and assumptions", () => {
    const mapped = mapBacktestResult(normalBacktestResult);
    expect(mapped.completedAt).toMatch(/Z$/);
    expect(mapped.trades[0]?.entryPrice).toBe("65000.123456789");
    expect(mapped.initialCapital).toBe("10000.00000000");
    expect(mapped.provenance.resultFingerprint).toBe("sha256:result013");
    expect(mapped.assumptions.roundingMode).toBe("HALF_EVEN");
    expect(() => mapBacktestResult({ ...normalBacktestResult, completedAt: "not-utc" })).toThrow();
  });
});
