import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { parsePublicEnvironment } from "@/src/foundation/config/environment";

describe("F-013 production composition", () => {
  it("rejects fixture-enabled production and keeps routes fixture-free", () => {
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
    for (const route of ["app/(protected)/backtests/page.tsx", "app/(protected)/search/page.tsx"]) {
      expect(readFileSync(route, "utf8")).not.toMatch(/fixtures|scenarios|testing/);
    }
  });
});
