import type {
  LogicalSubscription,
  RealtimeClient,
  RealtimeEnvelope,
  RealtimeStatus
} from "../realtime/contracts";
export class MockRealtimeClient implements RealtimeClient {
  private value: RealtimeStatus = "disconnected";
  private readonly eventListeners = new Set<(event: RealtimeEnvelope) => void>();
  private readonly statusListeners = new Set<(status: RealtimeStatus) => void>();
  readonly subscriptions = new Map<string, LogicalSubscription>();
  async connect() {
    this.value = "connected";
    this.statusListeners.forEach((listener) => listener(this.value));
  }
  disconnect() {
    this.value = "disconnected";
    this.statusListeners.forEach((listener) => listener(this.value));
    this.subscriptions.clear();
    this.eventListeners.clear();
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
  onEvent(listener: (event: RealtimeEnvelope) => void) {
    this.eventListeners.add(listener);
    return () => this.eventListeners.delete(listener);
  }
  onStatus(listener: (status: RealtimeStatus) => void) {
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }
  emit(event: RealtimeEnvelope) {
    this.eventListeners.forEach((listener) => listener(event));
  }
}
