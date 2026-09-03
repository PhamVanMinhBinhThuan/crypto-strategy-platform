import { describe, expect, it, vi } from "vitest";
import { createApiClient } from "@/src/foundation/http/api-client";
import type { AuthClient } from "@/src/foundation/auth/contracts";
const auth = {
  session: vi.fn(async () => null),
  subscribe: () => () => {}
} as unknown as AuthClient;
describe("normalized HTTP recovery metadata", () => {
  it("normalizes Retry-After without exposing Response", async () => {
    const result = await createApiClient(
      "https://api.test",
      auth,
      vi.fn(
        async () =>
          new Response(JSON.stringify({ code: "RATE_LIMIT_EXCEEDED", message: "Wait" }), {
            status: 429,
            headers: { "Retry-After": "12", "Content-Type": "application/json" }
          })
      ) as typeof fetch
    ).request("/x");
    expect(result).toEqual({
      ok: false,
      error: expect.objectContaining({
        code: "RATE_LIMIT_EXCEEDED",
        retryAfterSeconds: 12,
        retryable: true
      })
    });
  });
  it.each([null, "", "later", "-1", "1.5"])("ignores invalid Retry-After %s", async (header) => {
    const result = await createApiClient(
      "https://api.test",
      auth,
      vi.fn(
        async () =>
          new Response(JSON.stringify({ message: "safe" }), {
            status: 429,
            headers: {
              ...(header === null ? {} : { "Retry-After": header }),
              "Content-Type": "application/json"
            }
          })
      ) as typeof fetch
    ).request("/x");
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.error.retryAfterSeconds).toBeUndefined();
      expect(result.error).not.toHaveProperty("response");
    }
  });
  it("recovers once and never replays a 401", async () => {
    const fetcher = vi.fn(
      async () =>
        new Response(JSON.stringify({ code: "AUTHENTICATION_REQUIRED" }), {
          status: 401,
          headers: { "Content-Type": "application/json" }
        })
    );
    const recover = vi.fn();
    const result = await createApiClient(
      "https://api.test",
      auth,
      fetcher as typeof fetch,
      recover
    ).request("/mutation", { method: "POST" });
    expect(result.ok).toBe(false);
    expect(recover).toHaveBeenCalledTimes(1);
    expect(fetcher).toHaveBeenCalledTimes(1);
  });
});
