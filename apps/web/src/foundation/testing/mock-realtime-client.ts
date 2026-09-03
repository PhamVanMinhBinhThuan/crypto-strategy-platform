import type {
  LogicalSubscription,
  RealtimeClient,
  RealtimeEnvelope,
  RealtimeStatus,
  RealtimeStatusMetadata
} from "../realtime/contracts";
export class MockRealtimeClient implements RealtimeClient {
  private value: RealtimeStatus = "disconnected";
  readonly subscriptions = new Map<string, LogicalSubscription>();
  private readonly envelopeListeners = new Set<(value: RealtimeEnvelope) => void>();
  private readonly statusListeners = new Set<(value: RealtimeStatusMetadata) => void>();
  private transition(status: RealtimeStatus, metadata: Partial<RealtimeStatusMetadata> = {}) {
    this.value = status;
    this.statusListeners.forEach((listener) => listener({ status, attempt: 0, ...metadata }));
  }
  async connect() {
    this.transition("connecting");
    this.transition("connected");
  }
  disconnect() {
    this.transition("disconnected");
    this.subscriptions.clear();
    this.envelopeListeners.clear();
    this.statusListeners.clear();
  }
  subscribe(v: LogicalSubscription) {
    this.subscriptions.set(v.subscriptionId, v);
  }
  unsubscribe(id: string) {
    this.subscriptions.delete(id);
  }
  status() {
    return this.value;
  }
  onEnvelope(listener: (value: RealtimeEnvelope) => void) {
    this.envelopeListeners.add(listener);
    return () => this.envelopeListeners.delete(listener);
  }
  onEvent(listener: (value: RealtimeEnvelope) => void) {
    this.envelopeListeners.add(listener);
    return () => this.envelopeListeners.delete(listener);
  }
  onStatus(listener: (value: RealtimeStatusMetadata) => void) {
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }
  emit(value: RealtimeEnvelope) {
    this.envelopeListeners.forEach((listener) => listener(value));
  }
  emitStatus(status: RealtimeStatus, metadata: Partial<RealtimeStatusMetadata> = {}) {
    this.transition(status, metadata);
  }
  confirm(subscriptionId: string) {
    this.emit(this.envelope("SUBSCRIPTION_CONFIRMED", subscriptionId, {}));
  }
  fail(subscriptionId: string, code = "SUBSCRIPTION_ERROR") {
    this.emit(this.envelope("SUBSCRIPTION_ERROR", subscriptionId, { code }));
  }
  private envelope(eventType: string, subscriptionId: string, payload: unknown): RealtimeEnvelope {
    return {
      eventType,
      eventVersion: 1,
      eventId: `fixture-${eventType}-${subscriptionId}`,
      occurredAt: "2026-09-03T00:00:00Z",
      correlationId: "fixture-correlation",
      subscriptionId,
      payload
    };
  }
}
