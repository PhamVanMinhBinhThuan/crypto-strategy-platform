import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createBacktestResultService } from "@/src/features/backtests/service/backtest-result-service";
import { normalBacktestResult } from "@/src/features/backtests/fixtures/backtest-result-fixtures";
import type { BacktestId, BacktestResultId } from "@/src/features/backtests/types/backtest-result";
describe("backtest route/query states", () => {
  it("contains loading, refreshing, empty, inaccessible, blocked, retryable and terminal presentations", () => {
    const hook = readFileSync("src/features/backtests/hooks/useBacktestResult.ts", "utf8"),
      view = readFileSync("src/features/backtests/components/BacktestResultsView.tsx", "utf8");
    for (const state of [
      "loading",
      "refreshing",
      "empty-identifier",
      "inaccessible",
      "dependency-blocked",
      "retryable-failure",
      "terminal-failure"
    ])
      expect(hook + view).toContain(state);
    expect(view).toContain("canRetry");
  });
  it("loads the authoritative standalone snapshot", async () => {
    const api = new MockApiClient().respond(
      "/api/v1/backtests/backtest-013/result",
      normalBacktestResult
    );
    const result = await createBacktestResultService(api).readByBacktestId(
      "backtest-013" as BacktestId
    );
    expect(result.ok && result.data.backtestId).toBe("backtest-013");
  });
  it("loads a Search-produced result without inventing a standalone Backtest ID", async () => {
    const api = new MockApiClient().respond("/api/v1/backtest-results/result-013", {
      ...normalBacktestResult,
      backtestResultId: "result-013",
      backtestId: null
    });
    const result = await createBacktestResultService(api).readByResultId(
      "result-013" as BacktestResultId
    );
    expect(result.ok && result.data.backtestResultId).toBe("result-013");
    expect(result.ok && result.data.backtestId).toBeUndefined();
    expect(api.requests).toHaveLength(1);
  });
});
