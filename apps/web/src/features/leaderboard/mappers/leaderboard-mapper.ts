import { z } from "zod";
const decimal = z.string().regex(/^-?\d+(?:\.\d+)?$/);
const entry = z
  .object({
    rank: z.number().int().positive(),
    evaluationResultId: z.string().min(1),
    backtestResultId: z.string().min(1),
    score: decimal,
    maximumDrawdown: decimal,
    evaluationFingerprint: z.string().min(1)
  })
  .strict();
const schema = z
  .object({
    experimentId: z.string().min(1),
    revisionId: z.string().min(1),
    revision: z.number().int().positive(),
    topK: z.number().int().positive(),
    rankingPolicyVersion: z.string().min(1),
    fingerprint: z.string().min(1),
    createdAt: z.string().datetime(),
    items: z.array(entry),
    nextCursor: z.string().nullable(),
    hasMore: z.boolean()
  })
  .superRefine((value, context) => {
    if (value.items.length > value.topK)
      context.addIssue({ code: "custom", message: "Leaderboard exceeds configured Top-K." });
    const ranks = new Set<number>();
    const evaluations = new Set<string>();
    for (const item of value.items) {
      if (
        item.rank > value.topK ||
        ranks.has(item.rank) ||
        evaluations.has(item.evaluationResultId)
      )
        context.addIssue({ code: "custom", message: "Leaderboard entry identity is invalid." });
      ranks.add(item.rank);
      evaluations.add(item.evaluationResultId);
    }
  });
export const mapLeaderboard = (value: unknown) => schema.parse(value);
