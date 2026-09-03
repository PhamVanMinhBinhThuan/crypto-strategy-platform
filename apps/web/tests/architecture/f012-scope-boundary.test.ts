import { globSync, readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

const featureSource = globSync("src/features/{market,strategy,news}/**/*.{ts,tsx}")
  .map((file) => readFileSync(file, "utf8"))
  .join("\n");

describe("F-012 visual and scope boundary", () => {
  it("preserves the three approved workspace hierarchies", () => {
    expect(featureSource).toContain("market-workspace");
    expect(featureSource).toContain("market-grid");
    expect(featureSource).toContain("strategy-library");
    expect(featureSource).toContain("strategy-layout");
    expect(featureSource).toContain("news-feed");
    expect(featureSource).toContain("news-card");
  });

  it("does not implement prototype-only aggregate, AI, search, backtest or trading actions", () => {
    expect(featureSource).not.toMatch(
      /aggregate sentiment|trending topics|AI assistant|generate with AI|start search|run backtest|place order|buy now|sell now/i
    );
    expect(featureSource).not.toMatch(/features\/(search|experiment|backtest|leaderboard)/);
  });
});
