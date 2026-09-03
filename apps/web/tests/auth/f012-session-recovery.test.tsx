import { describe, expect, it, vi } from "vitest";
import { clearPrivateClientState, registerPrivateStateCleanup } from "@/src/foundation/auth/logout";
describe("F-012 session recovery", () => {
  it("cleans each registered feature once without retry loops", async () => {
    const market = vi.fn(),
      strategy = vi.fn(),
      news = vi.fn();
    const cleanup = [market, strategy, news].map(registerPrivateStateCleanup);
    await clearPrivateClientState();
    expect(market).toHaveBeenCalledOnce();
    expect(strategy).toHaveBeenCalledOnce();
    expect(news).toHaveBeenCalledOnce();
    cleanup.forEach((off) => off());
  });
});
