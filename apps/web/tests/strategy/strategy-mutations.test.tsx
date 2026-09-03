import { describe, expect, it, vi } from "vitest";
import { StrategyMutationController } from "@/src/features/strategy/state/strategy-controller";
describe("Strategy mutations", () => {
  it("blocks rapid duplicate submit and always reconciles after failure", async () => {
    const controller = new StrategyMutationController(),
      reconcile = vi.fn(async () => {}),
      reject = vi.fn(async () => {
        throw new Error("timeout");
      });
    const first = controller.run(reject, reconcile);
    const duplicate = await controller.run(reject, reconcile);
    expect(duplicate).toBeUndefined();
    await expect(first).rejects.toThrow("timeout");
    expect(reject).toHaveBeenCalledOnce();
    expect(reconcile).toHaveBeenCalledOnce();
    expect(controller.isPending).toBe(false);
  });
});
