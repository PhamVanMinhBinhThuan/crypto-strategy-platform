export const DEFAULT_MAX_RECONNECT_ATTEMPTS = 6;
export function reconnectDelay(attempt: number, random = Math.random) {
  const cap = 30_000,
    base = Math.min(cap, 500 * 2 ** Math.min(attempt, 6));
  return Math.round(base * (0.75 + random() * 0.5));
}
