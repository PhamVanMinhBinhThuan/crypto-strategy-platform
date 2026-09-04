import { z } from "zod";
import type { RealtimeEnvelope } from "@/src/foundation/realtime/contracts";

const targetPayload = z.object({ experimentId: z.string().min(1) }).passthrough();
const experimentEventSchema = z.discriminatedUnion("eventType", [
  z.object({
    eventType: z.literal("SUBSCRIPTION_CONFIRMED"),
    eventVersion: z.literal(1),
    payload: z.object({}).passthrough()
  }),
  z.object({
    eventType: z.literal("SUBSCRIPTION_ERROR"),
    eventVersion: z.literal(1),
    payload: z.object({ code: z.string().min(1) }).passthrough()
  }),
  z.object({
    eventType: z.literal("EXPERIMENT_PROGRESS_UPDATED"),
    eventVersion: z.literal(1),
    payload: targetPayload
  }),
  z.object({
    eventType: z.literal("BACKTEST_COMPLETED"),
    eventVersion: z.literal(1),
    payload: targetPayload.extend({
      candidateId: z.string().min(1),
      backtestResultId: z.string().min(1)
    })
  })
]);

export const experimentEventTypes = new Set([
  "SUBSCRIPTION_CONFIRMED",
  "SUBSCRIPTION_ERROR",
  "EXPERIMENT_PROGRESS_UPDATED",
  "BACKTEST_COMPLETED"
]);

export const validExperimentRealtimeEvent = (event: RealtimeEnvelope) =>
  experimentEventSchema.safeParse(event).success;

export class RealtimeReconciler {
  private ids: string[] = [];
  constructor(private readonly capacity = 128) {}
  accept(event: RealtimeEnvelope, target: string) {
    if (!event.eventId || this.ids.includes(event.eventId)) return false;
    const payload = event.payload as Record<string, unknown>;
    if (payload.experimentId && payload.experimentId !== target) return false;
    this.ids.push(event.eventId);
    if (this.ids.length > this.capacity) this.ids.shift();
    return true;
  }
  clear() {
    this.ids = [];
  }
}
