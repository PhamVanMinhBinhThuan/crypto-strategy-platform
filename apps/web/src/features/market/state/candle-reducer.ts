import type { Candle } from "../model/candle";
import { candleIdentity } from "../model/candle";
export type CandleState = Readonly<{
  items: readonly Candle[];
  eventTimes: Readonly<Record<string, string>>;
}>;
export const emptyCandleState: CandleState = { items: [], eventTimes: {} };
export function mergeCandles(
  state: CandleState,
  incoming: readonly Candle[],
  eventAt?: string,
  limit = 200
): CandleState {
  const items = new Map(state.items.map((item) => [candleIdentity(item), item]));
  const eventTimes = { ...state.eventTimes };
  for (const candle of incoming) {
    const id = candleIdentity(candle),
      current = items.get(id),
      previousEvent = eventTimes[id];
    if (eventAt && previousEvent && eventAt <= previousEvent) continue;
    if (current?.closed && !candle.closed) continue;
    items.set(id, candle);
    if (eventAt) eventTimes[id] = eventAt;
  }
  const ordered = [...items.values()]
    .sort((a, b) => a.openTime.localeCompare(b.openTime))
    .slice(-limit);
  return { items: ordered, eventTimes };
}
