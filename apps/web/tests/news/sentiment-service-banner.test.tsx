import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SentimentServiceBanner } from "@/src/features/news/components/SentimentServiceBanner";

describe("Sentiment service banner", () => {
  it("shows an explicit healthy state when new analysis is available", () => {
    render(
      <SentimentServiceBanner
        value={{
          status: "AVAILABLE",
          message: "Sentiment analysis is available.",
          checkedAt: "2026-09-08T00:00:00Z"
        }}
      />
    );

    expect(screen.getByRole("status")).toHaveAttribute("data-availability", "ready");
    expect(screen.getByText("Sentiment analysis is available.")).toBeInTheDocument();
  });

  it("shows a visible degraded state while preserving existing results", () => {
    render(
      <SentimentServiceBanner
        value={{
          status: "DEGRADED",
          message: "Existing results remain available; new items will be analyzed after recovery.",
          checkedAt: "2026-09-08T00:00:00Z"
        }}
      />
    );

    expect(screen.getByRole("status")).toHaveAttribute("data-availability", "degraded");
    expect(screen.getByText(/Existing results remain available/)).toBeInTheDocument();
  });
});
