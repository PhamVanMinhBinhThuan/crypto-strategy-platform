import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { NewsFeed } from "@/src/features/news/components/NewsFeed";
import { newsItemSchema } from "@/src/features/news/api/schemas";
import { newsPageFixture } from "../fixtures/f012/public-contract";
const item = newsItemSchema.parse(newsPageFixture.items[0]);
describe("News security", () => {
  it("opens only validated HTTP(S) links with isolation", () => {
    render(
      <NewsFeed
        items={[item]}
        loading={false}
        hasMore={false}
        onRetry={() => {}}
        onLoadMore={() => {}}
      />
    );
    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
    expect(link).toHaveAttribute("target", "_blank");
    expect(
      newsItemSchema.safeParse({ ...newsPageFixture.items[0], url: "javascript:alert(1)" }).success
    ).toBe(false);
  });
});
