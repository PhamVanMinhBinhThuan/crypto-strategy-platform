import { describe, expect, it, vi } from "vitest";
import { MockRealtimeClient } from "@/src/foundation/testing/mock-realtime-client";
import type { RealtimeClient, RealtimeEnvelope } from "@/src/foundation/realtime/contracts";
const envelope: RealtimeEnvelope = {
  eventType: "LEADERBOARD_UPDATED",
  eventVersion: 1,
  eventId: "event-1",
  occurredAt: "2026-09-03T00:00:00Z",
  correlationId: "correlation-1",
  subscriptionId: "leaderboard-1",
  payload: { experimentId: "experiment-1", revision: 2 }
};
const exercise = (
  client: RealtimeClient & {
    emit?: (e: RealtimeEnvelope) => void;
    confirm?: (id: string) => void;
    fail?: (id: string) => void;
  }
) => {
  const event = vi.fn(),
    status = vi.fn();
  const offEvent = client.onEnvelope(event),
    offStatus = client.onStatus(status);
  client.subscribe({
    subscriptionId: "leaderboard-1",
    eventType: "SUBSCRIBE_LEADERBOARD",
    payload: { experimentId: "experiment-1" }
  });
  client.emit?.(envelope);
  expect(event).toHaveBeenCalledWith(envelope);
  client.confirm?.("leaderboard-1");
  client.fail?.("leaderboard-1");
  offEvent();
  offStatus();
  client.emit?.({ ...envelope, eventId: "event-2" });
  expect(event).toHaveBeenCalledTimes(3);
  client.unsubscribe("leaderboard-1");
};
describe("public realtime listeners", () => {
  it("delivers envelopes/status and removes listeners/subscriptions", async () => {
    const client = new MockRealtimeClient();
    exercise(client);
    await client.connect();
    expect(client.status()).toBe("connected");
    client.disconnect();
    expect(client.subscriptions.size).toBe(0);
  });
  it("is assignable through the published interface", () => {
    const client: RealtimeClient = new MockRealtimeClient();
    expect(client.onEnvelope).toBeTypeOf("function");
  });
});
