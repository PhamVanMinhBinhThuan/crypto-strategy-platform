import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { createRealtimeClient } from "@/src/foundation/realtime/realtime-client";
import { useExperimentRealtime } from "@/src/features/experiments/hooks/useExperimentRealtime";
import { useLeaderboardRealtime } from "@/src/features/leaderboard/hooks/useLeaderboardRealtime";
import { FakeSocket, ticketApi } from "./helpers";

const wireEvent = (
  eventType: string,
  subscriptionId: string,
  payload: object,
  eventId: string
) => ({
  eventType,
  eventVersion: 1,
  eventId,
  occurredAt: "2026-09-03T00:00:00Z",
  correlationId: "correlation",
  subscriptionId,
  payload
});

describe("F-011/F-009 realtime integration", () => {
  it("keeps experiment and leaderboard recovery REST-authoritative", async () => {
    const socket = new FakeSocket("wss://api.test/ws");
    const realtime = createRealtimeClient(
      "wss://api.test/ws",
      ticketApi("once"),
      vi.fn(),
      () => socket
    );
    const experimentRefresh = vi.fn();
    const candidateRefresh = vi.fn();
    const leaderboardRefresh = vi.fn();
    renderHook(() =>
      useExperimentRealtime(realtime, "experiment-013", experimentRefresh, candidateRefresh)
    );
    renderHook(() => useLeaderboardRealtime(realtime, "experiment-013", 7, leaderboardRefresh));
    await waitFor(() => expect(realtime.status()).toBe("connecting"));
    act(() => socket.open());
    act(() =>
      socket.message(
        wireEvent(
          "SUBSCRIPTION_CONFIRMED",
          "experiment-experiment-013",
          { subscriptionType: "EXPERIMENT", status: "ACTIVE", syncMarker: "m1" },
          "c1"
        )
      )
    );
    act(() =>
      socket.message(
        wireEvent(
          "SUBSCRIPTION_CONFIRMED",
          "leaderboard-experiment-013",
          { subscriptionType: "LEADERBOARD", status: "ACTIVE", syncMarker: "m2" },
          "c2"
        )
      )
    );
    await waitFor(() => expect(experimentRefresh).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(leaderboardRefresh).toHaveBeenCalledTimes(1));
    act(() =>
      socket.message(
        wireEvent(
          "EXPERIMENT_PROGRESS_UPDATED",
          "experiment-experiment-013",
          { experimentId: "experiment-013", completedWork: 999 },
          "p1"
        )
      )
    );
    act(() =>
      socket.message(
        wireEvent(
          "LEADERBOARD_UPDATED",
          "leaderboard-experiment-013",
          { experimentId: "experiment-013", revision: 8 },
          "l1"
        )
      )
    );
    await waitFor(() => expect(experimentRefresh).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(leaderboardRefresh).toHaveBeenCalledTimes(2));
  });
});
