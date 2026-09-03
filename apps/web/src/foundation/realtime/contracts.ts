export type RealtimeStatus = "disconnected" | "connecting" | "connected" | "reconnecting";
export type RealtimeStatusMetadata = Readonly<{
  status: RealtimeStatus;
  attempt: number;
  exhausted?: boolean;
  closeCode?: number;
  closeReason?: string;
}>;
export type RealtimeEnvelope<T = unknown> = Readonly<{
  eventType: string;
  eventVersion: number;
  eventId: string;
  occurredAt: string;
  correlationId: string;
  subscriptionId: string;
  payload: T;
}>;
export type LogicalSubscription = Readonly<{
  subscriptionId: string;
  eventType: string;
  payload: Record<string, unknown>;
}>;
export interface RealtimeClient {
  connect(): Promise<void>;
  disconnect(): void;
  subscribe(value: LogicalSubscription): void;
  unsubscribe(subscriptionId: string): void;
  status(): RealtimeStatus;
  onEnvelope(listener: (value: RealtimeEnvelope) => void): () => void;
  onStatus(listener: (value: RealtimeStatusMetadata) => void): () => void;
}
