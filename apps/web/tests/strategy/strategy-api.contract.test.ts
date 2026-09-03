import { describe, expect, it } from "vitest";
import { userStrategySchema } from "@/src/features/strategy/api/schemas";
const value = {
  userStrategyId: "own-1",
  kind: "SINGLE",
  name: "Private MA",
  description: "",
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-09-03T00:00:00Z",
  updatedAt: "2026-09-03T00:00:00Z",
  latestVersion: {
    userStrategyVersionId: "v1",
    userStrategyId: "own-1",
    versionNo: 1,
    kind: "SINGLE",
    source: {
      type: "SINGLE",
      strategy: {
        strategyId: "ma",
        strategyVersionId: "sv1",
        version: "1",
        parameters: { threshold: "0.100000000001" }
      }
    },
    status: "DRAFT",
    fingerprint: "fp",
    publishedAt: null,
    createdAt: "2026-09-03T00:00:00Z"
  }
};
describe("private Strategy DTO", () => {
  it("parses owner-safe immutable detail", () =>
    expect(userStrategySchema.parse(value).latestVersion.source.type).toBe("SINGLE"));
  it("rejects internal owner fields", () =>
    expect(userStrategySchema.safeParse({ ...value, ownerId: "secret" }).success).toBe(false));
});
