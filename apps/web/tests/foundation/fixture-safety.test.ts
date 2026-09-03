import { describe, expect, it } from "vitest";
import { parsePublicEnvironment } from "@/src/foundation/config/environment";
describe("fixture safety", () => {
  it("rejects fixture production mode", () =>
    expect(() =>
      parsePublicEnvironment(
        {
          NEXT_PUBLIC_SUPABASE_URL: "https://x.test",
          NEXT_PUBLIC_SUPABASE_ANON_KEY: "1234567890123456",
          NEXT_PUBLIC_API_BASE_URL: "https://api.test",
          NEXT_PUBLIC_WS_URL: "wss://api.test/ws",
          NEXT_PUBLIC_ENABLE_FIXTURES: "true"
        },
        true
      )
    ).toThrow());
});
