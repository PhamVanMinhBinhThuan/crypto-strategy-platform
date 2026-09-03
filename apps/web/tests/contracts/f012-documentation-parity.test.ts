import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

const root = "../../";
const featureReference = readFileSync(`${root}docs/ui/features/F-012.md`, "utf8");
const roadmap = readFileSync(`${root}docs/implementation-roadmap.md`, "utf8");
const specification = readFileSync(`${root}specs/012-market-strategy-news-ui/spec.md`, "utf8");

describe("F-012 documentation parity", () => {
  it.each([
    ["Market Dashboard", "/market"],
    ["Strategy Composer", "/strategies"],
    ["News Sentiment", "/news"]
  ])("maps %s to its implemented route", (screen, route) => {
    expect(featureReference).toContain(screen);
    expect(featureReference).toContain(`Production route: \`${route}\``);
  });

  it("keeps Search/Experiment/Result/Leaderboard outside F-012", () => {
    expect(specification).toMatch(
      /MUST không triển khai Experiment\/Result\/Leaderboard UI, Search/
    );
    expect(roadmap).toContain("### F-013 — Experiment, Result and Leaderboard UI");
    expect(roadmap).toContain("### F-010 — Search Coordinator");
  });
});
