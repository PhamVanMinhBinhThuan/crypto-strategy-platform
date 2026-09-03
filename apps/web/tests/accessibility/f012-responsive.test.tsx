import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
describe("F-012 responsive styles", () => {
  it("provides one-column Market, Strategy and News layouts at mobile breakpoint", () => {
    const css = readFileSync("app/globals.css", "utf8");
    expect(css).toMatch(/@media \(max-width: 760px\)/);
    expect(css).toMatch(/\.market-grid\s*{\s*grid-template-columns: 1fr/);
    expect(css).toMatch(/\.strategy-layout\s*{\s*grid-template-columns: 1fr/);
    expect(css).toMatch(/\.news-card\s*{\s*grid-template-columns: 1fr/);
  });
});
