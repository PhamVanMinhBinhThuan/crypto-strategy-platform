import { describe, expect, it } from "vitest";
import { newsReducer, type NewsState } from "@/src/features/news/state/news-reducer";
import type { NewsItem } from "@/src/features/news/model/news";

describe("newsReducer", () => {
  const initialState: NewsState = {
    items: [],
    cursor: null,
    hasMore: false,
    queryGeneration: 1,
    loading: false,
    error: null,
    selectedStatuses: []
  };

  const item1: NewsItem = {
    newsId: "1",
    title: "Test 1",
    source: "A",
    url: "https://a.com",
    publishedAt: "2026-09-03T01:00:00Z",
    analysisStatus: "ANALYZED",
    relatedAssetIds: [],
    sentiment: null
  };

  const item2: NewsItem = {
    newsId: "2",
    title: "Test 2",
    source: "B",
    url: "https://b.com",
    publishedAt: "2026-09-03T02:00:00Z", // Newer
    analysisStatus: "ANALYZED",
    relatedAssetIds: [],
    sentiment: null
  };

  it("handles status filter changes and increments generation", () => {
    const next = newsReducer(initialState, { type: "SET_STATUS_FILTER", statuses: ["PENDING"] });
    expect(next.selectedStatuses).toEqual(["PENDING"]);
    expect(next.queryGeneration).toBe(2);
  });

  it("deduplicates items by id and keeps stable order (newest first)", () => {
    const state = newsReducer(initialState, {
      type: "FETCH_SUCCESS",
      generation: 1,
      items: [item1],
      nextCursor: "c1",
      hasMore: true
    });

    // Fetch second page that has duplicate item1 and a newer item2
    const next = newsReducer(state, {
      type: "FETCH_SUCCESS",
      generation: 1,
      items: [item1, item2],
      nextCursor: null,
      hasMore: false
    });

    expect(next.items).toHaveLength(2);
    expect(next.items[0].newsId).toBe("2"); // Newer first
    expect(next.items[1].newsId).toBe("1");
  });

  it("ignores late responses from older generations", () => {
    const next = newsReducer(initialState, {
      type: "FETCH_SUCCESS",
      generation: 0, // Late response
      items: [item1],
      nextCursor: null,
      hasMore: false
    });
    expect(next.items).toHaveLength(0);
  });
});
