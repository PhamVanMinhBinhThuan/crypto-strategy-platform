import type { RealtimeEnvelope } from "@/src/foundation/realtime/contracts";
export class RealtimeReconciler {
  private ids: string[] = [];
  constructor(private readonly capacity = 128) {}
  accept(event: RealtimeEnvelope, target: string) {
    if (this.ids.includes(event.eventId)) return false;
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
