import { describe, expect, it } from "vitest";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createLeaderboardService } from "@/src/features/leaderboard/service/leaderboard-service";
import { leaderboardPage } from "@/src/features/leaderboard/fixtures/leaderboard-fixtures";
describe("F-009 Leaderboard adapter", () => {
  it("uses bounded limit and encoded opaque cursor while retaining order", async () => {
    const path = "/api/v1/experiments/experiment-013/leaderboard?limit=25&cursor=opaque%2Bcursor";
    const api = new MockApiClient().respond(path, leaderboardPage);
    const result = await createLeaderboardService(api).read(
      "experiment-013",
      50,
      "opaque+cursor",
      25
    );
    expect(api.requests[0]?.path).toBe(path);
    expect(result.ok && result.data.items.map((x) => x.rank)).toEqual([1, 2]);
  });
  it.each([
    ["RESOURCE_NOT_FOUND", false],
    ["AUTHENTICATION_REQUIRED", false],
    ["RATE_LIMIT_EXCEEDED", true]
  ])("preserves sanitized %s response", async (code, retryable) => {
    const api = new MockApiClient().respond(
      "/api/v1/experiments/experiment-013/leaderboard?limit=10",
      { ok: false, error: { code, message: "safe", retryable } }
    );
    expect(await createLeaderboardService(api).read("experiment-013")).toEqual({
      ok: false,
      error: { code, message: "safe", retryable }
    });
  });
});
