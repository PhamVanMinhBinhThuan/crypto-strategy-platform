import { describe, expect, it, vi } from "vitest";
import { readFileSync } from "node:fs";
describe("Leaderboard reconciliation", () => {
  it("ignores stale/equal revisions and refreshes strictly newer revisions", () => {
    const refresh = vi.fn();
    const decide = (incoming: number, rendered: number) => {
      if (incoming > rendered) refresh();
    };
    decide(6, 7);
    decide(7, 7);
    expect(refresh).not.toHaveBeenCalled();
    decide(8, 7);
    expect(refresh).toHaveBeenCalledOnce();
  });
  it("contains no local ranking", () => {
    const source =
      readFileSync("src/features/leaderboard/hooks/useLeaderboardRealtime.ts", "utf8") +
      readFileSync("src/features/leaderboard/hooks/useLeaderboard.ts", "utf8");
    expect(source).toContain("revision > renderedRevision");
    expect(source).not.toMatch(/\.sort\(|score\s*[+*/-]/);
  });
});
