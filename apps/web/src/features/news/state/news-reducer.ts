import type { NewsItem, NewsAnalysisStatus } from "../model/news";

export type NewsState = {
  items: NewsItem[];
  cursor: string | null;
  hasMore: boolean;
  queryGeneration: number;
  loading: boolean;
  error: string | null;
  selectedStatuses: NewsAnalysisStatus[];
};

export type NewsAction =
  | { type: "SET_STATUS_FILTER"; statuses: NewsAnalysisStatus[] }
  | { type: "FETCH_START"; generation: number }
  | {
      type: "FETCH_SUCCESS";
      generation: number;
      items: NewsItem[];
      nextCursor: string | null;
      hasMore: boolean;
    }
  | { type: "FETCH_ERROR"; generation: number; error: string };

export function newsReducer(state: NewsState, action: NewsAction): NewsState {
  switch (action.type) {
    case "SET_STATUS_FILTER":
      return {
        ...state,
        selectedStatuses: action.statuses,
        queryGeneration: state.queryGeneration + 1,
        items: [],
        cursor: null,
        hasMore: false,
        error: null
      };

    case "FETCH_START":
      if (action.generation !== state.queryGeneration) return state;
      return { ...state, loading: true, error: null };

    case "FETCH_SUCCESS": {
      if (action.generation !== state.queryGeneration) return state;

      const newItems = [...state.items];
      const existingIds = new Set(newItems.map((item) => item.newsId));

      for (const item of action.items) {
        if (!existingIds.has(item.newsId)) {
          newItems.push(item);
          existingIds.add(item.newsId);
        }
      }

      // Stable sort newest first
      newItems.sort((a, b) => {
        const timeA = new Date(a.publishedAt).getTime();
        const timeB = new Date(b.publishedAt).getTime();
        if (timeA !== timeB) return timeB - timeA;
        return a.newsId.localeCompare(b.newsId);
      });

      return {
        ...state,
        loading: false,
        items: newItems,
        cursor: action.nextCursor,
        hasMore: action.hasMore
      };
    }

    case "FETCH_ERROR":
      if (action.generation !== state.queryGeneration) return state;
      return { ...state, loading: false, error: action.error };

    default:
      return state;
  }
}
