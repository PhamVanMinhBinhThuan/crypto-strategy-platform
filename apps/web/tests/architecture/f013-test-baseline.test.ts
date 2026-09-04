import { describe, expect, it } from "vitest";
import packageJson from "../../package.json";
import { readFileSync } from "node:fs";
describe("F-013 test baseline", () => {
  it("uses existing Vitest and Playwright scripts", () => {
    expect(packageJson.scripts.test).toBe("vitest run");
    expect(packageJson.scripts["test:e2e"]).toBe("playwright test");
    expect(readFileSync("vitest.config.ts", "utf8")).toContain("tests/**/*.test");
    expect(readFileSync("playwright.config.ts", "utf8")).toContain("./tests/e2e");
  });
});
