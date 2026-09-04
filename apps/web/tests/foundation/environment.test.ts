import { describe, expect, it } from "vitest";
import { parsePublicEnvironment } from "@/src/foundation/config/environment";

const valid = {
  NEXT_PUBLIC_SUPABASE_URL: "https://example.supabase.co",
  NEXT_PUBLIC_SUPABASE_ANON_KEY: "public-anon-key-long",
  NEXT_PUBLIC_API_BASE_URL: "http://localhost:8080/api/v1",
  NEXT_PUBLIC_WS_URL: "ws://localhost:8080/ws",
  NEXT_PUBLIC_ENABLE_FIXTURES: "false"
};
describe("public environment", () => {
  it("normalizes the documented API prefix to the server origin", () => {
    const environment = parsePublicEnvironment(valid, false);
    expect(environment.fixturesEnabled).toBe(false);
    expect(environment.apiBaseUrl).toBe("http://localhost:8080");
  });
  it("rejects production fixtures", () =>
    expect(() =>
      parsePublicEnvironment({ ...valid, NEXT_PUBLIC_ENABLE_FIXTURES: "true" }, true)
    ).toThrow(/Fixture/));
  it("rejects privileged secrets", () =>
    expect(() =>
      parsePublicEnvironment({ ...valid, SUPABASE_SERVICE_ROLE_KEY: "secret" }, false)
    ).toThrow(/forbidden/));
});
