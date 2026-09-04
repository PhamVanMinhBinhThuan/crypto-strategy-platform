import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createFixtureClients } from "@/src/foundation/composition/development-clients";
import { parsePublicEnvironment } from "@/src/foundation/config/environment";
describe("F-013 fixture composition", () => {
  it("captures deterministic result and request metadata", async () => {
    const api = new MockApiClient().respond("POST /command", {
      ok: true,
      data: { accepted: true }
    });
    const result = await api.request("/command", {
      method: "POST",
      headers: { "Idempotency-Key": "same-key" }
    });
    expect(result).toEqual({ ok: true, data: { accepted: true } });
    expect(api.requests).toEqual([
      { path: "/command", init: { method: "POST", headers: { "Idempotency-Key": "same-key" } } }
    ]);
  });
  it("selects fixtures explicitly and rejects production fixture configuration", () => {
    expect(createFixtureClients().fixtures).toBe(true);
    expect(() =>
      parsePublicEnvironment(
        {
          NEXT_PUBLIC_SUPABASE_URL: "https://example.supabase.co",
          NEXT_PUBLIC_SUPABASE_ANON_KEY: "public-anon-key-placeholder",
          NEXT_PUBLIC_API_BASE_URL: "https://api.example.test",
          NEXT_PUBLIC_WS_URL: "wss://api.example.test/ws",
          NEXT_PUBLIC_ENABLE_FIXTURES: "true"
        },
        true
      )
    ).toThrow("Fixture mode cannot be enabled in production");
  });
  it("keeps static testing imports out of the production composition root", () => {
    const root = readFileSync("src/foundation/composition/client-provider.tsx", "utf8");
    expect(root).not.toMatch(/foundation\/testing|\.\.\/testing|development-clients.*from/);
    for (const route of ["app/(protected)/backtests/page.tsx", "app/(protected)/search/page.tsx"]) {
      expect(readFileSync(route, "utf8")).not.toMatch(/fixtures|scenarios|testing/);
    }
  });
});
