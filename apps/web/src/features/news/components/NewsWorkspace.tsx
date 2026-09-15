"use client";
import { useCallback, useEffect, useMemo, useReducer, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useClients } from "@/src/foundation/composition/client-provider";
import type { NewsAnalysisStatus, SentimentServiceStatus } from "../model/news";
import { getSentimentServiceStatus, listNewsItems } from "../api/news-api";
import { newsReducer, type NewsState } from "../state/news-reducer";
import { NEWS_STATUSES, NewsFilters } from "./NewsFilters";
import { NewsFeed } from "./NewsFeed";
import { AsyncStatus } from "../../shared/AsyncStatus";
import { SentimentServiceBanner } from "./SentimentServiceBanner";
import { listSystemStrategies } from "../../strategy/api/strategy-api";
import { NewsStrategyAction } from "./NewsStrategyAction";
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
      currentCursor: undefined,
      cursorHistory: [],
      hasMore: false,
      queryGeneration: 0,
      loading: false,
      error: null,
      selectedStatuses: urlStatuses
    }),
    [urlStatuses]
  );
  const [state, dispatch] = useReducer(newsReducer, initial);
  const [sentimentStatus, setSentimentStatus] = useState<SentimentServiceStatus | null>(null);
  const [sentimentStrategyAvailable, setSentimentStrategyAvailable] = useState(false);
  const pendingUrlStatuses = useRef<string | null>(null);
  useEffect(() => {
    let active = true;
    const refresh = async () => {
      const result = await getSentimentServiceStatus(api);
      if (!active) return;
      setSentimentStatus(
        result.ok
          ? result.data
          : {
              status: "DEGRADED",
              message: "Unable to verify the Sentiment service. Existing results remain available.",
              checkedAt: new Date().toISOString()
            }
      );
    };
    void refresh();
    const timer = window.setInterval(() => void refresh(), 5_000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [api]);
  useEffect(() => {
    let active = true;
    void listSystemStrategies(api).then((result) => {
      if (!active) return;
      setSentimentStrategyAvailable(
        result.ok && result.data.items.some((item) => item.strategyId === "sentiment-polarity")
      );
    });
    return () => {
      active = false;
    };
  }, [api]);
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
    async (cursor?: string, cursorHistory: Array<string | undefined> = []) => {
      const generation = state.queryGeneration;
      dispatch({ type: "FETCH_START", generation });
      const result = await listNewsItems(api, {
        statuses: state.selectedStatuses,
        cursor,
        limit: 10
      });
      if (result.ok)
        dispatch({
          type: "FETCH_SUCCESS",
          generation,
          items: result.data.items,
          nextCursor: result.data.nextCursor,
          hasMore: result.data.hasMore,
          currentCursor: cursor,
          cursorHistory
        });
      else
        dispatch({
          type: "FETCH_ERROR",
          generation,
          error: result.error.retryable
            ? "News is temporarily unavailable. Please try again."
            : "Unable to load news with the current filters."
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
        message={state.loading ? "Loading news" : (state.error ?? "News is ready")}
        urgent={Boolean(state.error)}
      />
      <header>
        <div>
          <p className="eyebrow">News</p>
          <h1>News Sentiment</h1>
          <p>Public news and sentiment, with failures isolated by source.</p>
        </div>
        <div className="news-header-actions">
          <NewsStrategyAction available={sentimentStrategyAvailable} />
          <NewsFilters selected={state.selectedStatuses} onChange={changeFilters} />
        </div>
      </header>
      {sentimentStatus ? <SentimentServiceBanner value={sentimentStatus} /> : null}
      <NewsFeed
        items={state.items}
        loading={state.loading}
        error={state.error}
        hasMore={state.hasMore}
        pageNumber={state.cursorHistory.length + 1}
        canPrevious={state.cursorHistory.length > 0}
        onRetry={() => void fetchPage(state.currentCursor, state.cursorHistory)}
        onPrevious={() => {
          if (!state.cursorHistory.length) return;
          const previousCursor = state.cursorHistory.at(-1);
          void fetchPage(previousCursor, state.cursorHistory.slice(0, -1));
        }}
        onNext={() => {
          if (!state.cursor) return;
          void fetchPage(state.cursor, [...state.cursorHistory, state.currentCursor]);
        }}
      />
    </main>
  );
}
