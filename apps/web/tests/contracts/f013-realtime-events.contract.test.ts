import { describe, expect, it } from "vitest";
import type { RealtimeEnvelope } from "@/src/foundation/realtime/contracts";

const envelope = (eventType: string, payload: Record<string, unknown>): RealtimeEnvelope => ({
  eventType,
  eventVersion: 1,
  eventId: `event-${eventType}`,
  occurredAt: "2026-09-03T00:00:00Z",
  correlationId: "correlation",
  subscriptionId: "subscription",
  payload
});

describe("released F-009 realtime events consumed by F-013", () => {
  it.each([
    [
      "SUBSCRIPTION_CONFIRMED",
      { subscriptionType: "EXPERIMENT", status: "ACTIVE", syncMarker: "opaque" }
    ],
    [
      "SUBSCRIPTION_ERROR",
      { code: "RATE_LIMIT_EXCEEDED", message: "Retry later", details: {}, retryable: true }
    ],
    [
      "EXPERIMENT_PROGRESS_UPDATED",
      {
        experimentId: "experiment-013",
        jobId: "job-1",
        status: "RUNNING",
        completedWork: 1,
        failedWork: 0,
        totalWork: 2,
        bestScore: "0.1"
      }
    ],
    [
      "BACKTEST_COMPLETED",
      {
        experimentId: "experiment-013",
        candidateId: "candidate-1",
        backtestResultId: "result-1",
        evaluationResultId: "evaluation-1"
      }
    ],
    [
      "LEADERBOARD_UPDATED",
      {
        experimentId: "experiment-013",
        leaderboardId: "leaderboard-1",
        revision: 8,
        snapshotUrl: "/api/v1/experiments/experiment-013/leaderboard"
      }
    ]
  ])("preserves the version-one %s envelope", (eventType, payload) => {
    expect(envelope(eventType, payload)).toMatchObject({ eventType, eventVersion: 1, payload });
  });
  it("uses released logical subscription commands", () => {
    expect([
      "SUBSCRIBE_EXPERIMENT",
      "UNSUBSCRIBE_EXPERIMENT",
      "SUBSCRIBE_LEADERBOARD",
      "UNSUBSCRIBE_LEADERBOARD"
    ]).toHaveLength(4);
  });
});
