import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { NewsFeed } from "@/src/features/news/components/NewsFeed";
import { newsPageFixture } from "../fixtures/f012/public-contract";
import { newsItemSchema } from "@/src/features/news/api/schemas";
const item = newsItemSchema.parse(newsPageFixture.items[0]);
describe("News feed states", () => {
  it("renders loading, empty and retryable error states", async () => {
    const retry = vi.fn(),
      view = render(
        <NewsFeed items={[]} loading hasMore={false} onRetry={retry} onLoadMore={vi.fn()} />
      );
    expect(screen.getByRole("status")).toHaveTextContent("Đang tải");
    view.rerender(
      <NewsFeed items={[]} loading={false} hasMore={false} onRetry={retry} onLoadMore={vi.fn()} />
    );
    expect(screen.getByText(/Không có News/)).toBeInTheDocument();
    view.rerender(
      <NewsFeed
        items={[]}
        loading={false}
        error="Tạm gián đoạn"
        hasMore={false}
        onRetry={retry}
        onLoadMore={vi.fn()}
      />
    );
    await userEvent.click(screen.getByRole("button", { name: "Thử lại" }));
    expect(retry).toHaveBeenCalledOnce();
  });
  it("keeps News readable when sentiment fails and bounds load-more to user action", async () => {
    const load = vi.fn();
    render(
      <NewsFeed
        items={[{ ...item, analysisStatus: "FAILED", sentiment: null }]}
        loading={false}
        hasMore
        onRetry={vi.fn()}
        onLoadMore={load}
      />
    );
    expect(screen.getByRole("link")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Tải thêm" }));
    expect(load).toHaveBeenCalledOnce();
  });
});
