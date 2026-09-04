"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createLeaderboardService } from "../service/leaderboard-service";
import type { LeaderboardSnapshot } from "../types/leaderboard";
export function useLeaderboard(api: ApiClient, id?: string) {
  const [snapshot, setSnapshot] = useState<LeaderboardSnapshot>();
  const [limit, setLimit] = useState(10);
  const [error, setError] = useState<string>();
  const requestVersion = useRef(0);
  const refresh = useCallback(async () => {
    if (!id) return;
    const currentRequest = ++requestVersion.current;
    const r = await createLeaderboardService(api).read(id, limit, undefined, snapshot?.topK ?? 100);
    if (currentRequest !== requestVersion.current) return;
    if (r.ok) {
      setSnapshot((current) =>
        !current || r.data.revision >= current.revision ? r.data : current
      );
      setError(undefined);
    } else {
      setError(
        r.error.retryable
          ? "Leaderboard đang tạm thời không khả dụng. Vui lòng thử lại."
          : "Không thể tải Leaderboard."
      );
    }
  }, [api, id, limit, snapshot?.topK]);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- leaderboard identity starts an external API synchronization
    void refresh();
  }, [refresh]);
  const notifyRevision = (revision: number) => {
    if (revision > (snapshot?.revision ?? 0)) void refresh();
  };
  return { snapshot, limit, setLimit, error, refresh, notifyRevision };
}
