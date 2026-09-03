import { describe, expect, it, vi } from "vitest";
import type { ApiClient } from "@/src/foundation/http/contracts";
import {
  createUserStrategyVersion,
  getUserStrategy
} from "@/src/features/strategy/api/strategy-api";
import { StrategyMutationController } from "@/src/features/strategy/state/strategy-controller";

describe("Strategy owner/version integration", () => {
  it("maps missing and foreign owner to the same public failure", async () => {
    const api = {
      request: vi.fn(async () => ({
        ok: false as const,
        error: {
          code: "RESOURCE_NOT_FOUND_OR_INACCESSIBLE",
          message: "Strategy is unavailable.",
          retryable: false
        }
      }))
    } as ApiClient;
    expect(await getUserStrategy(api, "missing")).toEqual(await getUserStrategy(api, "foreign"));
  });
  it("sends expected version and reconciles after timeout", async () => {
    const request = vi.fn(async (path: string, init?: RequestInit) => {
      expect(path).toContain("/versions");
      expect(init?.method).toBe("POST");
      throw new Error("timeout");
    });
    const api = { request } as unknown as ApiClient,
      reconcile = vi.fn(async () => {}),
      controller = new StrategyMutationController();
    await expect(
      controller.run(
        () =>
          createUserStrategyVersion(api, "own", 1, {
            type: "SINGLE",
            strategy: { strategyId: "ma", version: "1", parameters: { period: "5" } }
          }),
        reconcile
      )
    ).rejects.toThrow("timeout");
    expect(JSON.parse(request.mock.calls[0][1]?.body as string).expectedLatestVersionNo).toBe(1);
    expect(reconcile).toHaveBeenCalledOnce();
  });
});
