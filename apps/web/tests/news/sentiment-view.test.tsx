import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SentimentStatus } from "@/src/features/news/components/SentimentStatus";
import { newsPageFixture } from "../fixtures/f012/public-contract";
import { newsItemSchema } from "@/src/features/news/api/schemas";
const item = newsItemSchema.parse(newsPageFixture.items[0]);
describe("Sentiment presentation", () => {
  it("shows public analyzed fields and disclaimer", () => {
    render(<SentimentStatus item={item} />);
    expect(screen.getByText(/Confidence 0.80/)).toBeInTheDocument();
    expect(screen.getByText(/không phải lời khuyên/)).toBeInTheDocument();
  });
  for (const status of ["PENDING", "ANALYZING", "FAILED_RETRYABLE", "FAILED"] as const)
    it(`keeps ${status} honest`, () => {
      render(<SentimentStatus item={{ ...item, analysisStatus: status, sentiment: null }} />);
      expect(screen.getByRole("status")).toHaveTextContent(status);
    });
});
