"use client";
import { useEffect, useMemo, useState } from "react";
import type { RealtimeClient, RealtimeStatusMetadata } from "@/src/foundation/realtime/contracts";
import { RealtimeReconciler } from "./realtime-reconciler";
export function useExperimentRealtime(
  realtime: RealtimeClient,
  id: string | undefined,
  onExperimentRefresh: () => void,
  onCandidateRefresh: () => void,
  terminal = false
) {
  const subscriptionId = id ? `experiment-${id}` : "";
  const reconciler = useMemo(() => new RealtimeReconciler(), []);
  const [connection, setConnection] = useState<RealtimeStatusMetadata>({
    status: realtime.status(),
    attempt: 0
  });
  const [subscriptionError, setSubscriptionError] = useState<string>();
  useEffect(() => {
    if (!id) return;
    let recovering = true;
    const buffered: Parameters<Parameters<typeof realtime.onEnvelope>[0]>[0][] = [];
    const offStatus = realtime.onStatus(setConnection);
    const offEnvelope = realtime.onEnvelope((event) => {
      if (event.subscriptionId !== subscriptionId || !reconciler.accept(event, id)) return;
      if (recovering && event.eventType !== "SUBSCRIPTION_CONFIRMED") {
        buffered.push(event);
        return;
      }
      if (event.eventType === "SUBSCRIPTION_CONFIRMED") {
        recovering = true;
        Promise.resolve(onExperimentRefresh()).then(() => {
          recovering = false;
          buffered.splice(0).forEach((e) => {
            if (e.eventType === "BACKTEST_COMPLETED") onCandidateRefresh();
            else onExperimentRefresh();
          });
        });
        return;
      }
      if (event.eventType === "SUBSCRIPTION_ERROR")
        setSubscriptionError(
          String((event.payload as Record<string, unknown>).code ?? "Subscription failed")
        );
      else if (event.eventType === "BACKTEST_COMPLETED") onCandidateRefresh();
      else if (event.eventType === "EXPERIMENT_PROGRESS_UPDATED") onExperimentRefresh();
    });
    realtime.subscribe({
      subscriptionId,
      eventType: "SUBSCRIBE_EXPERIMENT",
      payload: { experimentId: id }
    });
    void realtime.connect();
    return () => {
      offEnvelope();
      offStatus();
      realtime.unsubscribe(subscriptionId);
      reconciler.clear();
    };
  }, [id, onCandidateRefresh, onExperimentRefresh, realtime, reconciler, subscriptionId]);
  useEffect(() => {
    if (terminal) onExperimentRefresh();
  }, [terminal, onExperimentRefresh]);
  return { connection, subscriptionError, reconnect: () => realtime.connect() };
}
