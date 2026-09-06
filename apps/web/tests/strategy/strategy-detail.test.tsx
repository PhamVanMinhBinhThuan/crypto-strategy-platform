import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { StrategyDetail } from "@/src/features/strategy/components/StrategyDetail";
import { StrategyActions } from "@/src/features/strategy/components/StrategyActions";
import type { StrategyDescriptor, UserStrategy } from "@/src/features/strategy/model/strategy";
const owned = {
  userStrategyId: "s1",
  kind: "SINGLE",
  name: "Private MA",
  description: "",
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-09-03T00:00:00Z",
  updatedAt: "2026-09-03T00:00:00Z",
  latestVersion: {
    userStrategyVersionId: "v1",
    userStrategyId: "s1",
    versionNo: 1,
    kind: "SINGLE",
    source: {
      type: "SINGLE",
      strategy: { strategyId: "ma", strategyVersionId: "sv1", version: "1", parameters: {} }
    },
    status: "PUBLISHED",
    fingerprint: "fp",
    publishedAt: "2026-09-03T00:00:00Z",
    createdAt: "2026-09-03T00:00:00Z"
  }
} as const;
const descriptor = {
  strategyId: "ma",
  strategyVersionId: "sv1",
  version: "1.0.0",
  contractVersion: "strategy-contract-v1",
  displayName: "Moving Average Crossover",
  description: "MA demo",
  category: "TREND",
  supportedSignals: ["BUY", "SELL", "HOLD"],
  requiredLookback: 25,
  parameters: [],
  constraints: [],
  descriptorFingerprint: "descriptor-fp"
} as StrategyDescriptor;
const compositeOwned = {
  ...owned,
  userStrategyId: "composite-1",
  kind: "COMPOSITE",
  name: "Weighted Demo",
  latestVersion: {
    ...owned.latestVersion,
    userStrategyVersionId: "composite-v1",
    userStrategyId: "composite-1",
    kind: "COMPOSITE",
    source: {
      type: "COMPOSITE",
      policyId: "weighted-vote",
      policyVersion: "1.0.0",
      policyParameters: { "weight.sv1": "0.7", "weight.sv2": "0.3" },
      components: [
        {
          strategyId: "ma",
          strategyVersionId: "sv1",
          version: "1.0.0",
          parameters: {}
        },
        {
          strategyId: "rsi",
          strategyVersionId: "sv2",
          version: "1.0.0",
          parameters: {}
        }
      ]
    }
  }
} as UserStrategy;
describe("Strategy detail", () => {
  it("marks published versions immutable", () => {
    render(<StrategyDetail owned={owned} />);
    expect(screen.getByRole("note")).toHaveTextContent("bất biến");
  });
  it("shows the standard signals supported by a system strategy", () => {
    render(<StrategyDetail descriptor={descriptor} />);
    const signals = screen.getByRole("region", { name: "Tín hiệu hỗ trợ" });
    expect(signals).toHaveTextContent("BUY");
    expect(signals).toHaveTextContent("SELL");
    expect(signals).toHaveTextContent("HOLD");
  });
  it("explains weighted conflict resolution and configured components", () => {
    render(<StrategyDetail owned={compositeOwned} systemStrategies={[descriptor]} />);
    const policy = screen.getByRole("region", { name: "Quy tắc Composite" });
    expect(policy).toHaveTextContent("Weighted Vote");
    expect(policy).toHaveTextContent("Moving Average Crossover");
    expect(policy).toHaveTextContent("trọng số 0.7");
    expect(policy).toHaveTextContent("BUY 0.5 · SELL 0.5→ HOLD");
    expect(policy).toHaveTextContent("không phải tín hiệu thị trường hiện tại");
  });
  it("requires explicit archive confirmation", async () => {
    const archive = vi.fn(),
      confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    render(
      <StrategyActions
        canPublish={false}
        archived={false}
        pending={false}
        onPublish={vi.fn()}
        onArchive={archive}
        onNewVersion={vi.fn()}
      />
    );
    await userEvent.click(screen.getByRole("button", { name: "Archive" }));
    expect(archive).not.toHaveBeenCalled();
    confirm.mockReturnValue(true);
    await userEvent.click(screen.getByRole("button", { name: "Archive" }));
    expect(archive).toHaveBeenCalledOnce();
    confirm.mockRestore();
  });
});
