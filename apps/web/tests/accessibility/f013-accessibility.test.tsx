import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RealtimeStatus } from "@/src/features/experiments/components/RealtimeStatus";
import { TradeHistory } from "@/src/features/backtests/components/TradeHistory";
import { manyTradeBacktestResult } from "@/src/features/backtests/fixtures/backtest-result-fixtures";
import { ExperimentActions } from "@/src/features/experiments/components/ExperimentActions";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { runningExperiment } from "@/src/features/experiments/fixtures/experiment-job-fixtures";

describe("F-013 accessibility", () => {
  it("announces realtime degradation with text and an accessible retry", () => {
    render(
      <RealtimeStatus
        value={{ status: "disconnected", attempt: 5, exhausted: true }}
        onReconnect={vi.fn()}
      />
    );
    expect(screen.getByRole("complementary")).toHaveAttribute("aria-live", "polite");
    expect(screen.getByText("Automatic retries exhausted.")).toBeVisible();
    expect(screen.getByRole("button", { name: "Reconnect" })).toBeVisible();
  });
  it("provides keyboard-local table scrolling and full authoritative decimals", () => {
    render(<TradeHistory trades={manyTradeBacktestResult.trades} />);
    const region = screen.getByRole("region", { name: "Scrollable trade history" });
    expect(region).toHaveAttribute("tabindex", "0");
    expect(region.querySelector("td[title]")).toBeTruthy();
  });
  it("restores focus after the stop dialog", () => {
    const api = { request: vi.fn() } as unknown as ApiClient;
    render(<ExperimentActions api={api} experiment={runningExperiment} onRefresh={vi.fn()} />);
    const trigger = screen.getByRole("button", { name: "Stop Experiment" });
    trigger.focus();
    fireEvent.click(trigger);
    expect(screen.getByRole("dialog")).toHaveAttribute("aria-modal", "true");
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(trigger).toHaveFocus();
  });
});
