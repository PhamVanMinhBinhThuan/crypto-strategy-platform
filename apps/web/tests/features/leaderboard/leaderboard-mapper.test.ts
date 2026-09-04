import { describe, expect, it } from "vitest";
import { mapLeaderboard } from "@/src/features/leaderboard/mappers/leaderboard-mapper";
import { leaderboardPage } from "@/src/features/leaderboard/fixtures/leaderboard-fixtures";
import { capLeaderboardLimit } from "@/src/features/leaderboard/types/leaderboard";
describe("Leaderboard contract", () => {
  it("preserves server order and the six released entry fields", () => {
    const value = mapLeaderboard(leaderboardPage);
    expect(value.items.map((e) => e.rank)).toEqual([1, 2]);
    expect(Object.keys(value.items[0]!)).toEqual([
      "rank",
      "evaluationResultId",
      "backtestResultId",
      "score",
      "maximumDrawdown",
      "evaluationFingerprint"
    ]);
  });
  it("caps limits by public and configured bounds", () => {
    expect(capLeaderboardLimit(0)).toBe(10);
    expect(capLeaderboardLimit(200)).toBe(100);
    expect(capLeaderboardLimit(50, 25)).toBe(25);
  });
  it("preserves exact score/drawdown, metadata and opaque cursors", () => {
    const value = mapLeaderboard({
      ...leaderboardPage,
      nextCursor: "opaque+cursor==",
      hasMore: true
    });
    expect(value.items[0]?.score).toBe("0.873400000000000001");
    expect(value.items[0]?.maximumDrawdown).toBe("0.0831");
    expect(value).toMatchObject({
      revision: 7,
      topK: 25,
      rankingPolicyVersion: "1.0.0",
      nextCursor: "opaque+cursor=="
    });
  });
  it("rejects empty authoritative result IDs and duplicate ranks", () => {
    expect(() =>
      mapLeaderboard({
        ...leaderboardPage,
        items: [
          { ...leaderboardPage.items[0], backtestResultId: "" },
          { ...leaderboardPage.items[1], rank: 1 }
        ]
      })
    ).toThrow();
  });
});
