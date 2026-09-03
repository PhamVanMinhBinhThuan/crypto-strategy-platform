import { vi } from "vitest";
import type { ApiClient } from "@/src/foundation/http/contracts";
export class FakeSocket {
  onopen: ((e: Event) => void) | null = null;
  onclose: ((e: CloseEvent) => void) | null = null;
  onmessage: ((e: MessageEvent) => void) | null = null;
  sent: string[] = [];
  closed = false;
  constructor(readonly url: string) {}
  send(v: string) {
    this.sent.push(v);
  }
  close() {
    this.closed = true;
  }
  open() {
    this.onopen?.(new Event("open"));
  }
  message(value: unknown) {
    this.onmessage?.({ data: JSON.stringify(value) } as MessageEvent);
  }
  closeWith(code = 1006, reason = "lost") {
    this.onclose?.({ code, reason } as CloseEvent);
  }
}
export const ticketApi = (...tickets: string[]) =>
  ({
    request: vi.fn(async () => ({ ok: true, data: { ticket: tickets.shift() ?? "fresh" } }))
  }) as unknown as ApiClient;
