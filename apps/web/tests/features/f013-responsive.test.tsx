import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";

describe("F-013 responsive matrix", () => {
  const css = readFileSync("app/globals.css", "utf8");
  it.each([360, 768, 1024, 1440])("supports the %ipx viewport without shell overflow", (width) => {
    expect(width).toBeGreaterThanOrEqual(360);
    expect(css).toMatch(/\.app-main\s*\{[^}]*min-width:\s*0/s);
    expect(css).toMatch(/\.table-scroll\s*\{[^}]*overflow-x:\s*auto/s);
    expect(css).toMatch(/@media \(max-width: 760px\)/);
    expect(css).toMatch(/@media \(max-width: 1024px\)/);
  });
  it("keeps primary mobile actions visible and honors reduced motion", () => {
    expect(css).toMatch(/\.actions \.button\s*\{[^}]*width:\s*100%/s);
    expect(css).toMatch(/prefers-reduced-motion:\s*reduce/);
    expect(css).toMatch(/:focus-visible/);
  });
});
