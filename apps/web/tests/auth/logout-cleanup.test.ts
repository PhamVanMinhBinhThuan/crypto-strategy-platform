import { describe, expect, it, vi } from "vitest";
import { clearPrivateClientState, registerPrivateStateCleanup } from "@/src/foundation/auth/logout";
describe("logout cleanup", () => {
  it("runs registered cleanup", async () => {
    const fn = vi.fn();
    const off = registerPrivateStateCleanup(fn);
    await clearPrivateClientState();
    expect(fn).toHaveBeenCalledOnce();
    off();
  });
});
