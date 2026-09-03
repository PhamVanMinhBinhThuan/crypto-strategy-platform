import type { LogicalSubscription } from "./contracts";
export class SubscriptionRegistry {
  private readonly values = new Map<string, LogicalSubscription>();
  set(v: LogicalSubscription) {
    this.values.set(v.subscriptionId, v);
  }
  delete(id: string) {
    this.values.delete(id);
  }
  all() {
    return [...this.values.values()];
  }
  clear() {
    this.values.clear();
  }
  get(id: string) {
    return this.values.get(id);
  }
}
