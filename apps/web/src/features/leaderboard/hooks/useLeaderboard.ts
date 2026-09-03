"use client";
import { useCallback, useEffect, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createLeaderboardService } from "../service/leaderboard-service";
import type { LeaderboardSnapshot } from "../types/leaderboard";
export function useLeaderboard(api: ApiClient, id?: string) {
  const [snapshot, setSnapshot] = useState<LeaderboardSnapshot>();
  const [limit, setLimit] = useState(10);
  const [error, setError] = useState<string>();
  const refresh = useCallback(async () => {
    if (!id) return;
    const r = await createLeaderboardService(api).read(id, limit, undefined, snapshot?.topK ?? 100);
    if (r.ok) {
      setSnapshot(r.data);
      setError(undefined);
    } else setError(r.error.message);
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
