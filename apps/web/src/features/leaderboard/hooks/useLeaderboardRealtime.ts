"use client";
import { useEffect } from "react";
import { z } from "zod";
import type { RealtimeClient } from "@/src/foundation/realtime/contracts";
import { RealtimeReconciler } from "../../experiments/hooks/realtime-reconciler";

const eventSchema = z.discriminatedUnion("eventType", [
  z.object({
    eventType: z.literal("SUBSCRIPTION_CONFIRMED"),
    eventVersion: z.literal(1),
    payload: z.object({}).passthrough()
  }),
  z.object({
    eventType: z.literal("LEADERBOARD_UPDATED"),
    eventVersion: z.literal(1),
    payload: z
      .object({ experimentId: z.string().min(1), revision: z.number().int().positive() })
      .passthrough()
  })
]);
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
      if (
        event.subscriptionId !== subscriptionId ||
        !["SUBSCRIPTION_CONFIRMED", "LEADERBOARD_UPDATED"].includes(event.eventType) ||
        !reconciler.accept(event, id)
      )
        return;
      const parsed = eventSchema.safeParse(event);
      if (!parsed.success) {
        onRefresh();
        return;
      }
      if (event.eventType === "SUBSCRIPTION_CONFIRMED") {
        recovering = true;
        Promise.resolve(onRefresh()).then(() => {
          recovering = false;
          if (bufferedRevision > renderedRevision) onRefresh();
        });
        return;
      }
      if (parsed.data.eventType === "LEADERBOARD_UPDATED") {
        const revision = parsed.data.payload.revision;
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
