import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { StrategyVersionForm } from "@/src/features/strategy/components/StrategyVersionForm";
import type { StrategyDescriptor, UserStrategy } from "@/src/features/strategy/model/strategy";

const descriptor = {
  strategyId: "bollinger-bands",
  strategyVersionId: "system-v1",
  version: "1.0.0",
  contractVersion: "strategy-contract-v1",
  displayName: "Bollinger Bands",
  description: "",
  category: "VOLATILITY",
  supportedSignals: ["BUY", "SELL", "HOLD"],
  requiredLookback: 20,
  parameters: [
    {
      name: "period",
      type: "INTEGER",
      required: true,
      defaultValue: "20",
      minimum: "2",
      maximum: "200",
      allowedValues: [],
      description: ""
    }
  ],
  constraints: [],
  descriptorFingerprint: "descriptor-fingerprint"
} as StrategyDescriptor;

const owned = {
  userStrategyId: "owned-1",
  kind: "SINGLE",
  name: "Bollinger Demo",
  description: "",
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-09-04T00:00:00Z",
  updatedAt: "2026-09-04T00:00:00Z",
  latestVersion: {
    userStrategyVersionId: "version-1",
    userStrategyId: "owned-1",
    versionNo: 1,
    kind: "SINGLE",
    source: {
      type: "SINGLE",
      strategy: {
        strategyId: "bollinger-bands",
        strategyVersionId: "system-v1",
        version: "1.0.0",
        parameters: { period: "21" }
      }
    },
    status: "DRAFT",
    fingerprint: "fingerprint-1",
    publishedAt: null,
    createdAt: "2026-09-04T00:00:00Z"
  }
} as UserStrategy;

const rsiDescriptor = {
  ...descriptor,
  strategyId: "rsi-threshold",
  strategyVersionId: "system-v2",
  displayName: "RSI Threshold"
} as StrategyDescriptor;

const compositeOwned = {
  ...owned,
  userStrategyId: "owned-composite",
  kind: "COMPOSITE",
  name: "Composite Demo",
  latestVersion: {
    ...owned.latestVersion,
    userStrategyVersionId: "version-composite-1",
    userStrategyId: "owned-composite",
    kind: "COMPOSITE",
    source: {
      type: "COMPOSITE",
      policyId: "weighted-vote",
      policyVersion: "1.0.0",
      policyParameters: {
        "weight.system-v1": "0.6",
        "weight.system-v2": "0.4"
      },
      components: [
        {
          strategyId: "bollinger-bands",
          strategyVersionId: "system-v1",
          version: "1.0.0",
          parameters: { period: "21" }
        },
        {
          strategyId: "rsi-threshold",
          strategyVersionId: "system-v2",
          version: "1.0.0",
          parameters: { period: "14" }
        }
      ]
    }
  }
} as UserStrategy;

describe("Strategy version form", () => {
  it("requires a change and submits typed parameters", async () => {
    const submit = vi.fn(async () => {});
    render(
      <StrategyVersionForm
        owned={owned}
        systemStrategies={[descriptor]}
        pending={false}
        onSubmit={submit}
        onCancel={vi.fn()}
      />
    );
    const button = screen.getByRole("button", { name: "Lưu version mới" });
    expect(button).toBeDisabled();
    await userEvent.clear(screen.getByLabelText("period"));
    await userEvent.type(screen.getByLabelText("period"), "22");
    expect(button).toBeEnabled();
    await userEvent.click(button);
    expect(submit).toHaveBeenCalledWith({
      type: "SINGLE",
      strategy: {
        strategyId: "bollinger-bands",
        version: "1.0.0",
        parameters: { period: 22 }
      }
    });
  });

  it("edits parameters of each component when creating a composite version", async () => {
    const submit = vi.fn(async () => {});
    render(
      <StrategyVersionForm
        owned={compositeOwned}
        systemStrategies={[descriptor, rsiDescriptor]}
        pending={false}
        onSubmit={submit}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByLabelText("Bollinger Bands · period")).toHaveValue("21");
    expect(screen.getByLabelText("RSI Threshold · period")).toHaveValue("14");

    await userEvent.clear(screen.getByLabelText("Bollinger Bands · period"));
    await userEvent.type(screen.getByLabelText("Bollinger Bands · period"), "30");
    await userEvent.click(screen.getByRole("button", { name: "Lưu version mới" }));

    expect(submit).toHaveBeenCalledWith({
      type: "COMPOSITE",
      policyId: "weighted-vote",
      policyVersion: "1.0.0",
      policyParameters: {
        "weight.system-v1": "0.6",
        "weight.system-v2": "0.4"
      },
      components: [
        {
          strategyId: "bollinger-bands",
          version: "1.0.0",
          parameters: { period: 30 }
        },
        {
          strategyId: "rsi-threshold",
          version: "1.0.0",
          parameters: { period: 14 }
        }
      ]
    });
  });
});
