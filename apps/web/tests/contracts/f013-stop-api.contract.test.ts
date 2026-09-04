import { describe, expect, it } from "vitest";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createExperimentCommandService } from "@/src/features/experiments/service/experiment-command-service";
describe("F-009 Stop adapter", () => {
  it("posts one logical Idempotency-Key and accepts 202-shaped evidence", async () => {
    const api = new MockApiClient().respond("POST /api/v1/experiments/experiment-013/stop", {
      experimentId: "experiment-013",
      status: "STOP_REQUESTED"
    });
    const result = await createExperimentCommandService(api).stop("experiment-013", "logical-key");
    expect(result.ok).toBe(true);
    expect(api.requests).toHaveLength(1);
    expect(api.requests[0]).toMatchObject({
      path: "/api/v1/experiments/experiment-013/stop",
      init: { method: "POST", headers: { "Idempotency-Key": "logical-key" } }
    });
  });
  it.each([
    ["INVALID_STATE_TRANSITION", false],
    ["TRANSPORT_UNCERTAIN", true],
    ["AUTHENTICATION_REQUIRED", false],
    ["RATE_LIMIT_EXCEEDED", true]
  ])("preserves %s without replay", async (code, retryable) => {
    const api = new MockApiClient().respond("POST /api/v1/experiments/experiment-013/stop", {
      ok: false,
      error: {
        code,
        message: "safe",
        retryable,
        ...(code === "RATE_LIMIT_EXCEEDED" ? { retryAfterSeconds: 9 } : {})
      }
    });
    const result = await createExperimentCommandService(api).stop("experiment-013", "same-key");
    expect(result.ok).toBe(false);
    expect(api.requests).toHaveLength(1);
    if (!result.ok && code === "RATE_LIMIT_EXCEEDED")
      expect(result.error.retryAfterSeconds).toBe(9);
  });
});
