import { describe, expect, it } from "vitest";
import { parseBacktestLookup } from "@/src/features/backtests/types/backtest-result";
describe("backtest lookup", () => {
  it.each([
    [{}, "none"],
    [{ resultId: "result-013" }, "resultId"],
    [{ backtestId: "backtest-013" }, "backtestId"],
    [{ resultId: "!" }, "invalid"],
    [{ resultId: "result-013", backtestId: "backtest-013" }, "invalid"]
  ] as const)("parses %o", (input, kind) =>
    expect(parseBacktestLookup(input)).toMatchObject({ kind })
  );
});
