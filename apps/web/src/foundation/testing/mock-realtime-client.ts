import type { LogicalSubscription, RealtimeClient, RealtimeStatus } from "../realtime/contracts";
export class MockRealtimeClient implements RealtimeClient {
  private value: RealtimeStatus = "disconnected";
  readonly subscriptions = new Map<string, LogicalSubscription>();
  async connect() {
    this.value = "connected";
  }
  disconnect() {
    this.value = "disconnected";
    this.subscriptions.clear();
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
}
