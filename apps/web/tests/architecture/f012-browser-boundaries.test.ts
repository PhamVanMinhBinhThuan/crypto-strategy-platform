import { describe, expect, it } from "vitest";
import { globSync, readFileSync } from "node:fs";
describe("F-012 browser boundary", () => {
  it("contains no direct provider, business table, internal sentiment or duplicate client", () => {
    const source = globSync("src/features/**/*.{ts,tsx}")
      .map((file) => readFileSync(file, "utf8"))
      .join("\n");
    expect(source).not.toMatch(/binance|\/internal\/news-items|createClient\(|from\(["']/i);
    expect(source).not.toMatch(/new WebSocket|createApiClient|createRealtimeClient/);
  });
});
