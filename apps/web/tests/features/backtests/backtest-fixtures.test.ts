import { describe, expect, it } from "vitest";
import {
  backtestFixtureErrors,
  extremeDecimalBacktestResult,
  manyTradeBacktestResult,
  normalBacktestResult,
  zeroTradeBacktestResult
} from "@/src/features/backtests/fixtures/backtest-result-fixtures";
describe("finite backtest fixtures", () => {
  it("contains normal, zero, many and extreme immutable snapshots", () => {
    expect(normalBacktestResult.trades).toHaveLength(2);
    expect(zeroTradeBacktestResult.trades).toHaveLength(0);
    expect(manyTradeBacktestResult.trades).toHaveLength(6);
    expect(extremeDecimalBacktestResult.metrics.totalReturn).toContain(".000000000000000001");
  });
  it("contains every safe failure class", () =>
    expect(Object.keys(backtestFixtureErrors)).toEqual([
      "inaccessible",
      "resultIdBlocked",
      "retryable",
      "terminal",
      "authentication",
      "rateLimited"
    ]));
  it("uses no timers, randomness or financial calculation", async () => {
    const source = await import("node:fs").then((fs) =>
      fs.readFileSync("src/features/backtests/fixtures/backtest-result-fixtures.ts", "utf8")
    );
    expect(source).not.toMatch(/setTimeout|setInterval|Math\.random|reduce\(/);
  });
});
