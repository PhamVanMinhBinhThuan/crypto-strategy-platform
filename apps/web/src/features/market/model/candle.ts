import { candleSchema } from "../api/schemas";
import type { z } from "zod";
export type Candle = z.infer<typeof candleSchema>;
export const candleIdentity = (candle: Candle) =>
  `${candle.pair}|${candle.timeframe}|${candle.openTime}`;
export function parseCandle(value: unknown): Candle {
  return candleSchema.parse(value);
}
