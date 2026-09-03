import { z } from "zod";
const decimal = z.string().regex(/^-?\d+(?:\.\d+)?$/);
const entry = z
  .object({
    rank: z.number().int().positive(),
    evaluationResultId: z.string(),
    backtestResultId: z.string(),
    score: decimal,
    maximumDrawdown: decimal,
    evaluationFingerprint: z.string()
  })
  .strict();
const schema = z.object({
  experimentId: z.string(),
  revisionId: z.string(),
  revision: z.number().int().positive(),
  topK: z.number().int().positive(),
  rankingPolicyVersion: z.string(),
  fingerprint: z.string(),
  createdAt: z.string().datetime(),
  items: z.array(entry),
  nextCursor: z.string().nullable(),
  hasMore: z.boolean()
});
export const mapLeaderboard = (value: unknown) => schema.parse(value);
