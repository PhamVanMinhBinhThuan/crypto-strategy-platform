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
});
