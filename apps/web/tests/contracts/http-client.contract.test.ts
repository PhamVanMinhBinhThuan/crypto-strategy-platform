import { describe, expect, it, vi } from "vitest";
import { createApiClient } from "@/src/foundation/http/api-client";
import type { AuthClient } from "@/src/foundation/auth/contracts";
const auth = {
  session: vi.fn(async () => ({
    userId: "u",
    email: "u@x.test",
    accessToken: "token",
    expiresAt: 1
  })),
  subscribe: () => () => {}
} as unknown as AuthClient;
describe("API client", () => {
  it("adds bearer and correlation headers", async () => {
    const fetcher = vi.fn(async (url: unknown, init?: RequestInit) => {
      expect(url).toBe("https://api.test/x");
      expect(init).toBeDefined();
      return new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    });
    const result = await createApiClient("https://api.test", auth, fetcher as typeof fetch).request(
      "/x"
    );
    expect(result.ok).toBe(true);
    expect(fetcher.mock.calls[0]?.[1]?.headers).toMatchObject({ Authorization: "Bearer token" });
  });
});
