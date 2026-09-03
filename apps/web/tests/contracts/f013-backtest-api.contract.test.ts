import { describe, expect, it } from "vitest";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createBacktestResultService } from "@/src/features/backtests/service/backtest-result-service";
import { normalBacktestResult } from "@/src/features/backtests/fixtures/backtest-result-fixtures";
import type { BacktestId } from "@/src/features/backtests/types/backtest-result";
const id = "backtest-013" as BacktestId;
describe("F-009 backtest adapter", () => {
  it("uses the released standalone path and preserves exact DTO strings", async () => {
    const api = new MockApiClient().respond(
      "/api/v1/backtests/backtest-013/result",
      normalBacktestResult
    );
    const result = await createBacktestResultService(api).readByBacktestId(id);
    expect(api.requests[0]?.path).toBe("/api/v1/backtests/backtest-013/result");
    expect(result.ok && result.data.metrics.totalReturn).toBe("18.427500000000000001");
  });
  it.each(["RESOURCE_NOT_FOUND", "FORBIDDEN"])(
    "collapses %s to ownership-safe inaccessible",
    async (code) => {
      const api = new MockApiClient().respond("/api/v1/backtests/backtest-013/result", {
        ok: false,
        error: { code, message: "private detail", retryable: false }
      });
      await expect(createBacktestResultService(api).readByBacktestId(id)).resolves.toEqual({
        ok: false,
        error: { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false }
      });
    }
  );
  it.each([
    ["AUTHENTICATION_REQUIRED", false],
    ["RATE_LIMIT_EXCEEDED", true]
  ])("preserves sanitized %s", async (code, retryable) => {
    const api = new MockApiClient().respond("/api/v1/backtests/backtest-013/result", {
      ok: false,
      error: { code, message: "safe", retryable }
    });
    const result = await createBacktestResultService(api).readByBacktestId(id);
    expect(result).toEqual({ ok: false, error: { code, message: "safe", retryable } });
  });
});
