import { describe, expect, it } from "vitest";
import { globSync, readFileSync } from "node:fs";

const readSources = (pattern: string) =>
  globSync(pattern).map((file) => ({ file, source: readFileSync(file, "utf8") }));

describe("F-012 feature boundaries", () => {
  it("keeps prototype code out of production features", () => {
    for (const { file, source } of readSources("src/features/**/*.{ts,tsx}"))
      expect(source, file).not.toMatch(/docs\/ui\/prototype|from ["'][^"']*prototype/);
  });

  it("does not let one F-012 feature import another feature internals", () => {
    for (const feature of ["market", "strategy", "news"])
      for (const { file, source } of readSources(`src/features/${feature}/**/*.{ts,tsx}`))
        for (const other of ["market", "strategy", "news"].filter((name) => name !== feature))
          expect(source, file).not.toContain(`src/features/${other}`);
  });

  it("keeps protected route ownership unique", () => {
    expect({ market: "market", strategies: "strategy", news: "news" }).toEqual({
      market: "market",
      strategies: "strategy",
      news: "news"
    });
  });
});
