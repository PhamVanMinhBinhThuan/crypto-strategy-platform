import { z } from "zod";
import {
  nonNegativeDecimalStringSchema,
  paginationSchema,
  utcInstantSchema
} from "../../shared/public-contract";

export const candleSchema = z
  .object({
    pair: z.string().regex(/^[A-Z0-9]+\/[A-Z0-9]+$/),
    timeframe: z.enum(["1m", "5m", "15m", "30m", "1h", "2h", "4h", "1d"]),
    openTime: utcInstantSchema,
    closeTime: utcInstantSchema,
    open: nonNegativeDecimalStringSchema,
    high: nonNegativeDecimalStringSchema,
    low: nonNegativeDecimalStringSchema,
    close: nonNegativeDecimalStringSchema,
    volume: nonNegativeDecimalStringSchema,
    closed: z.boolean()
  })
  .strict();
export const candlePageSchema = paginationSchema.extend({ items: z.array(candleSchema) });
