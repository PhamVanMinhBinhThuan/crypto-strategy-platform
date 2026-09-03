import { z } from "zod";

export const decimalStringSchema = z.string().regex(/^-?(0|[1-9][0-9]*)(\.[0-9]+)?$/);
export const nonNegativeDecimalStringSchema = z.string().regex(/^(0|[1-9][0-9]*)(\.[0-9]+)?$/);
export const utcInstantSchema = z.iso.datetime({ offset: true });
export const cursorSchema = z.string().min(1);
export const paginationSchema = z.object({
  nextCursor: cursorSchema.nullable(),
  hasMore: z.boolean()
});
