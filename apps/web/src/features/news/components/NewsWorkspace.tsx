"use client";
import { useCallback, useEffect, useMemo, useReducer, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useClients } from "@/src/foundation/composition/client-provider";
import type { NewsAnalysisStatus } from "../model/news";
import { listNewsItems } from "../api/news-api";
import { newsReducer, type NewsState } from "../state/news-reducer";
import { NEWS_STATUSES, NewsFilters } from "./NewsFilters";
import { NewsFeed } from "./NewsFeed";
import { AsyncStatus } from "../../shared/AsyncStatus";
const parseStatuses = (params: URLSearchParams) =>
  [...new Set(params.getAll("analysisStatus"))].filter((value): value is NewsAnalysisStatus =>
    NEWS_STATUSES.some((item) => item === value)
  );
export function NewsWorkspace() {
  const { api } = useClients(),
    params = useSearchParams(),
    router = useRouter();
  const urlStatuses = useMemo(
    () => parseStatuses(new URLSearchParams(params.toString())),
    [params]
  );
  const initial = useMemo<NewsState>(
    () => ({
      items: [],
      cursor: null,
      hasMore: false,
      queryGeneration: 0,
      loading: false,
      error: null,
      selectedStatuses: urlStatuses
    }),
    [urlStatuses]
  );
  const [state, dispatch] = useReducer(newsReducer, initial);
  const pendingUrlStatuses = useRef<string | null>(null);
  useEffect(() => {
    const current = state.selectedStatuses.join("|");
    const incoming = urlStatuses.join("|");
    if (pendingUrlStatuses.current !== null) {
      if (incoming === pendingUrlStatuses.current) pendingUrlStatuses.current = null;
      else return;
    }
    if (current !== incoming) dispatch({ type: "SET_STATUS_FILTER", statuses: urlStatuses });
  }, [state.selectedStatuses, urlStatuses]);
  const fetchPage = useCallback(
    async (cursor?: string) => {
      const generation = state.queryGeneration;
      dispatch({ type: "FETCH_START", generation });
      const result = await listNewsItems(api, {
        statuses: state.selectedStatuses,
        cursor,
        limit: 30
      });
      if (result.ok)
        dispatch({
          type: "FETCH_SUCCESS",
          generation,
          items: result.data.items,
          nextCursor: result.data.nextCursor,
          hasMore: result.data.hasMore
        });
      else
        dispatch({
          type: "FETCH_ERROR",
          generation,
          error: result.error.retryable
            ? "News đang tạm gián đoạn. Vui lòng thử lại."
            : "Không thể tải News với bộ lọc hiện tại."
        });
    },
    [api, state.queryGeneration, state.selectedStatuses]
  );
  useEffect(() => {
    const timer = window.setTimeout(() => void fetchPage(), 0);
    return () => clearTimeout(timer);
  }, [fetchPage]);
  const changeFilters = (statuses: NewsAnalysisStatus[]) => {
    const next = new URLSearchParams();
    statuses.forEach((status) => next.append("analysisStatus", status));
    pendingUrlStatuses.current = statuses.join("|");
    router.replace(`/news${next.size ? `?${next}` : ""}`);
    dispatch({ type: "SET_STATUS_FILTER", statuses });
  };
  return (
    <main className="news-workspace">
      <AsyncStatus
        message={state.loading ? "Đang tải News" : (state.error ?? "News đã sẵn sàng")}
        urgent={Boolean(state.error)}
      />
      <header>
        <div>
          <p className="eyebrow">F-012 · News</p>
          <h1>News Sentiment</h1>
          <p>Tin tức và sentiment công khai; mỗi nguồn lỗi được cô lập.</p>
        </div>
        <NewsFilters selected={state.selectedStatuses} onChange={changeFilters} />
      </header>
      <NewsFeed
        items={state.items}
        loading={state.loading}
        error={state.error}
        hasMore={state.hasMore}
        onRetry={() => void fetchPage()}
        onLoadMore={() => state.cursor && void fetchPage(state.cursor)}
      />
    </main>
  );
}
