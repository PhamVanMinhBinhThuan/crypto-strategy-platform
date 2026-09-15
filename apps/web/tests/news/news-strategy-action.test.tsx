import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { NewsStrategyAction } from "@/src/features/news/components/NewsStrategyAction";

describe("News to Strategy navigation", () => {
  it("opens the authoritative Sentiment template in Composer when capability exists", () => {
    render(<NewsStrategyAction available />);

    expect(screen.getByRole("link", { name: "Use as Strategy" })).toHaveAttribute(
      "href",
      "/strategies?strategy=sentiment-polarity"
    );
  });

  it("does not advertise an unavailable catalog capability", () => {
    render(<NewsStrategyAction available={false} />);
    expect(screen.queryByRole("link", { name: "Use as Strategy" })).not.toBeInTheDocument();
  });
});
