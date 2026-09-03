"use client";
import { useEffect } from "react";
import type { RealtimeClient } from "@/src/foundation/realtime/contracts";
import { RealtimeReconciler } from "../../experiments/hooks/realtime-reconciler";
export function useLeaderboardRealtime(
  realtime: RealtimeClient,
  id: string | undefined,
  renderedRevision: number,
  onRefresh: () => void
) {
  useEffect(() => {
    if (!id) return;
    const subscriptionId = `leaderboard-${id}`,
      reconciler = new RealtimeReconciler();
    let recovering = true;
    let bufferedRevision = 0;
    const off = realtime.onEnvelope((event) => {
      if (event.subscriptionId !== subscriptionId || !reconciler.accept(event, id)) return;
      if (event.eventType === "SUBSCRIPTION_CONFIRMED") {
        recovering = true;
        Promise.resolve(onRefresh()).then(() => {
          recovering = false;
          if (bufferedRevision > renderedRevision) onRefresh();
        });
        return;
      }
      if (event.eventType === "LEADERBOARD_UPDATED") {
        const revision = Number((event.payload as Record<string, unknown>).revision);
        if (recovering) bufferedRevision = Math.max(bufferedRevision, revision);
        else if (revision > renderedRevision) onRefresh();
      }
    });
    realtime.subscribe({
      subscriptionId,
      eventType: "SUBSCRIBE_LEADERBOARD",
      payload: { experimentId: id }
    });
    return () => {
      off();
      realtime.unsubscribe(subscriptionId);
      reconciler.clear();
    };
  }, [id, onRefresh, realtime, renderedRevision]);
}
