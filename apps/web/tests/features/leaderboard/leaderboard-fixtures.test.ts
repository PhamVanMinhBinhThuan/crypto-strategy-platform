import { describe, expect, it } from "vitest";
import {
  cursorLeaderboard,
  emptyLeaderboard,
  leaderboardFixtureErrors,
  leaderboardPage,
  newerLeaderboard,
  staleLeaderboard
} from "@/src/features/leaderboard/fixtures/leaderboard-fixtures";
describe("finite Leaderboard fixtures", () => {
  it("covers empty, entries, cursor, Top-K and revisions", () => {
    expect(emptyLeaderboard.items).toEqual([]);
    expect(leaderboardPage.items).toHaveLength(2);
    expect(cursorLeaderboard).toMatchObject({
      nextCursor: "opaque+cursor==",
      hasMore: true,
      topK: 25
    });
    expect([staleLeaderboard.revision, newerLeaderboard.revision]).toEqual([6, 8]);
  });
  it("covers inaccessible, 401 and 429", () =>
    expect(Object.keys(leaderboardFixtureErrors)).toEqual([
      "inaccessible",
      "authentication",
      "rateLimited"
    ]));
  it("contains no score/rank calculation", async () => {
    const source = await import("node:fs").then((fs) =>
      fs.readFileSync("src/features/leaderboard/fixtures/leaderboard-fixtures.ts", "utf8")
    );
    expect(source).not.toMatch(/sort\(|reduce\(|Math\./);
  });
});
