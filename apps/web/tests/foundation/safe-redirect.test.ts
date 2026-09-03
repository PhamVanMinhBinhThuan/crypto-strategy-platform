import { describe, expect, it } from "vitest";
import { safeRedirect } from "@/src/foundation/auth/safe-redirect";
describe("safeRedirect", () => {
  it("keeps internal routes", () =>
    expect(safeRedirect("/backtests?id=1")).toBe("/backtests?id=1"));
  it.each([null, "https://evil.test", "//evil.test", "/login?next=/login"])(
    "falls back for unsafe target",
    (value) => expect(safeRedirect(value)).toBe("/market")
  );
});
