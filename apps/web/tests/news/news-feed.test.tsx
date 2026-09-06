import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { NewsFeed } from "@/src/features/news/components/NewsFeed";
import { newsPageFixture } from "../fixtures/f012/public-contract";
import { newsItemSchema } from "@/src/features/news/api/schemas";

const item = newsItemSchema.parse(newsPageFixture.items[0]);
const pagination = {
  pageNumber: 1,
  canPrevious: false,
  onPrevious: vi.fn(),
  onNext: vi.fn()
};

describe("News feed states", () => {
  it("renders loading, empty and retryable error states", async () => {
    const retry = vi.fn();
    const view = render(
      <NewsFeed items={[]} loading hasMore={false} onRetry={retry} {...pagination} />
    );
    expect(screen.getByRole("status")).toHaveTextContent("Loading");
    view.rerender(
      <NewsFeed items={[]} loading={false} hasMore={false} onRetry={retry} {...pagination} />
    );
    expect(screen.getByText(/No news matches/)).toBeInTheDocument();
    view.rerender(
      <NewsFeed
        items={[]}
        loading={false}
        error="Temporarily unavailable"
        hasMore={false}
        onRetry={retry}
        {...pagination}
      />
    );
    await userEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(retry).toHaveBeenCalledOnce();
  });

  it("keeps News readable when sentiment fails and paginates on user action", async () => {
    const next = vi.fn();
    render(
      <NewsFeed
        items={[{ ...item, analysisStatus: "FAILED", sentiment: null }]}
        loading={false}
        hasMore
        pageNumber={1}
        canPrevious={false}
        onRetry={vi.fn()}
        onPrevious={vi.fn()}
        onNext={next}
      />
    );
    expect(screen.getByRole("link")).toBeInTheDocument();
    expect(screen.getByText(/News content is available/)).toHaveTextContent(
      /Sentiment is pending or unavailable/
    );
    expect(screen.getByText(/Showing 1–1 · Page 1/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    expect(next).toHaveBeenCalledOnce();
  });
});
