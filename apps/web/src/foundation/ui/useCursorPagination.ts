"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { ApiResult } from "@/src/foundation/http/contracts";

export type CursorPage<T> = Readonly<{
  items: readonly T[];
  nextCursor: string | null;
  hasMore: boolean;
  totalCount: number;
}>;

export function useCursorPagination<T>(
  loadPage: (cursor?: string) => Promise<ApiResult<CursorPage<T>>>
) {
  const [page, setPage] = useState<CursorPage<T>>();
  const [cursor, setCursor] = useState<string>();
  const [history, setHistory] = useState<Array<string | undefined>>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const requestVersion = useRef(0);

  const load = useCallback(async (
    targetCursor?: string,
    targetHistory: Array<string | undefined> = history
  ) => {
    const request = ++requestVersion.current;
    setLoading(true);
    setError(undefined);
    const result = await loadPage(targetCursor);
    if (request !== requestVersion.current) return;
    if (result.ok) {
      setPage(result.data);
      setCursor(targetCursor);
      setHistory(targetHistory);
    } else {
      setError(result.error.message);
    }
    setLoading(false);
  }, [history, loadPage]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- route entry starts an external API synchronization
    void load(undefined, []);
    return () => { requestVersion.current++; };
    // load intentionally changes after page navigation because it closes over history.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadPage]);

  const next = () => {
    if (!page?.nextCursor || loading) return;
    void load(page.nextCursor, [...history, cursor]);
  };
  const previous = () => {
    if (!history.length || loading) return;
    void load(history.at(-1), history.slice(0, -1));
  };

  return {
    page,
    loading,
    error,
    pageNumber: history.length + 1,
    canPrevious: history.length > 0,
    next,
    previous,
    retry: () => load(cursor, history)
  };
}
