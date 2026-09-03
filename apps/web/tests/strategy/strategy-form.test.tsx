import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { StrategyForm } from "@/src/features/strategy/components/StrategyForm";
import type { StrategyDescriptor } from "@/src/features/strategy/model/strategy";

const strategy = (id: string): StrategyDescriptor => ({
  strategyId: id,
  strategyVersionId: `${id}-v1`,
  version: "1",
  contractVersion: "1",
  displayName: id.toUpperCase(),
  description: "",
  category: "TREND",
  supportedSignals: ["BUY"],
  requiredLookback: 2,
  parameters: [
    {
      name: "period",
      type: "INTEGER",
      required: true,
      defaultValue: "5",
      minimum: "2",
      maximum: "100",
      allowedValues: [],
      description: ""
    }
  ],
  constraints: [],
  descriptorFingerprint: `${id}-fp`
});

describe("Strategy form", () => {
  it("submits canonical SINGLE defaults", async () => {
    const submit = vi.fn(async (draft: unknown) => void draft),
      first = strategy("ma");
    render(
      <StrategyForm
        descriptor={first}
        systemStrategies={[first]}
        pending={false}
        onSubmit={submit}
      />
    );
    await userEvent.type(screen.getByLabelText("Tên Strategy"), "Private MA");
    await userEvent.click(screen.getByRole("button", { name: "Lưu Strategy" }));
    expect(submit.mock.calls[0][0]).toMatchObject({
      kind: "SINGLE",
      source: { strategy: { parameters: { period: "5" } } }
    });
  });
  it("requires two valid components for COMPOSITE", async () => {
    const submit = vi.fn(async (draft: unknown) => void draft),
      first = strategy("ma"),
      second = strategy("rsi");
    render(
      <StrategyForm
        descriptor={first}
        systemStrategies={[first, second]}
        pending={false}
        onSubmit={submit}
      />
    );
    await userEvent.type(screen.getByLabelText("Tên Strategy"), "Composite");
    await userEvent.click(screen.getByText("Composite"));
    expect(screen.getByRole("button", { name: "Lưu Strategy" })).toBeDisabled();
    await userEvent.click(screen.getByText(/MA · v1/));
    await userEvent.click(screen.getByText(/RSI · v1/));
    expect(screen.getByRole("button", { name: "Lưu Strategy" })).toBeEnabled();
    await userEvent.click(screen.getByRole("button", { name: "Lưu Strategy" }));
    expect(submit.mock.calls[0][0]).toMatchObject({
      kind: "COMPOSITE",
      source: { type: "COMPOSITE", components: [{ strategyId: "ma" }, { strategyId: "rsi" }] }
    });
  });
});
