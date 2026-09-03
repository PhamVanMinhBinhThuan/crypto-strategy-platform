import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RealtimeReconciler } from "@/src/features/experiments/hooks/realtime-reconciler";
import { useExperimentRealtime } from "@/src/features/experiments/hooks/useExperimentRealtime";
import { useLeaderboardRealtime } from "@/src/features/leaderboard/hooks/useLeaderboardRealtime";
import { MockRealtimeClient } from "@/src/foundation/testing/mock-realtime-client";
import type { RealtimeEnvelope } from "@/src/foundation/realtime/contracts";
const event = (eventId: string, experimentId = "experiment-013"): RealtimeEnvelope => ({
  eventType: "EXPERIMENT_PROGRESS_UPDATED",
  eventVersion: 1,
  eventId,
  occurredAt: "2026-09-03T00:00:00Z",
  correlationId: "c",
  subscriptionId: "s",
  payload: { experimentId }
});
describe("realtime reconciliation", () => {
  it("rejects duplicate IDs and wrong targets", () => {
    const r = new RealtimeReconciler(2);
    expect(r.accept(event("1"), "experiment-013")).toBe(true);
    expect(r.accept(event("1"), "experiment-013")).toBe(false);
    expect(r.accept(event("2", "other"), "experiment-013")).toBe(false);
  });
  it("uses a bounded dedup window", () => {
    const r = new RealtimeReconciler(2);
    r.accept(event("1"), "experiment-013");
    r.accept(event("2"), "experiment-013");
    r.accept(event("3"), "experiment-013");
    expect(r.accept(event("1"), "experiment-013")).toBe(true);
  });
  it("recovers authoritative experiment state before buffered hints", async () => {
    const realtime = new MockRealtimeClient();
    let release!: () => void;
    const refresh = vi.fn(() => new Promise<void>((resolve) => (release = resolve)));
    const candidates = vi.fn();
    const { unmount } = renderHook(() =>
      useExperimentRealtime(realtime, "experiment-013", refresh, candidates)
    );
    await waitFor(() => expect(realtime.subscriptions.size).toBe(1));
    act(() => realtime.confirm("experiment-experiment-013"));
    const buffered = { ...event("buffered"), subscriptionId: "experiment-experiment-013" };
    act(() => realtime.emit(buffered));
    expect(refresh).toHaveBeenCalledTimes(1);
    act(() => release());
    await waitFor(() => expect(refresh).toHaveBeenCalledTimes(2));
    act(() => realtime.emit(buffered));
    expect(refresh).toHaveBeenCalledTimes(2);
    unmount();
    expect(realtime.subscriptions.size).toBe(0);
  });
  it("recovers leaderboard snapshots and gates revisions", async () => {
    const realtime = new MockRealtimeClient();
    const refresh = vi.fn();
    renderHook(() => useLeaderboardRealtime(realtime, "experiment-013", 7, refresh));
    act(() => realtime.confirm("leaderboard-experiment-013"));
    await waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
    const leaderboard = (id: string, revision: number): RealtimeEnvelope => ({
      ...event(id),
      eventType: "LEADERBOARD_UPDATED",
      subscriptionId: "leaderboard-experiment-013",
      payload: { experimentId: "experiment-013", revision }
    });
    act(() => realtime.emit(leaderboard("stale", 7)));
    expect(refresh).toHaveBeenCalledTimes(1);
    act(() => realtime.emit(leaderboard("new", 8)));
    expect(refresh).toHaveBeenCalledTimes(2);
  });
});
