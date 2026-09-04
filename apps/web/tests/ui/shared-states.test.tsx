import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { LoadingState } from "@/src/components/states/LoadingState";
import { DegradedState } from "@/src/components/states/DegradedState";
describe("shared states", () => {
  it("announces loading", () => {
    render(<LoadingState />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading");
  });
  it("explains degradation", () => {
    render(<DegradedState message="Sentiment is unavailable." />);
    expect(screen.getByText("Limited availability")).toBeInTheDocument();
  });

  it.each([
    ["ready", "Ready", "✓"],
    ["degraded", "Limited availability", "!"],
    ["stale", "Stale snapshot", "↻"],
    ["recovering", "Recovering", "…"]
  ] as const)("renders %s with a visible label and non-color symbol", (state, label, symbol) => {
    render(<DegradedState state={state} message={`${state} detail`} />);

    const status = screen.getByRole("status");
    expect(status).toHaveAttribute("data-availability", state);
    expect(status).toHaveTextContent(label);
    expect(status).toHaveTextContent(symbol);
    expect(status).toHaveTextContent(`${state} detail`);
  });
});
