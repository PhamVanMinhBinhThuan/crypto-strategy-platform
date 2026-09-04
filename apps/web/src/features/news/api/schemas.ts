import { z } from "zod";
import {
  decimalStringSchema,
  paginationSchema,
  utcInstantSchema
} from "../../shared/public-contract";
const confidence = decimalStringSchema.refine((v) => Number(v) >= 0 && Number(v) <= 1);
const polarity = decimalStringSchema.refine((v) => Number(v) >= -1 && Number(v) <= 1);
export const sentimentSchema = z
  .object({
    label: z.enum(["POSITIVE", "NEUTRAL", "NEGATIVE"]),
    confidence,
    polarityScore: polarity
  })
  .strict();
export const newsItemSchema = z
  .object({
    newsId: z.string().min(1).max(128),
    title: z.string().min(1),
    source: z.string().min(1),
    url: z.url().refine((value) => /^https?:\/\//.test(value), "Must use HTTP(S)"),
    publishedAt: utcInstantSchema,
    analysisStatus: z.enum(["PENDING", "ANALYZING", "ANALYZED", "FAILED_RETRYABLE", "FAILED"]),
    relatedAssetIds: z.array(z.string().min(1).max(128)),
    sentiment: sentimentSchema.nullable()
  })
  .strict()
  .superRefine((item, context) => {
    if (item.analysisStatus === "ANALYZED" && item.sentiment === null)
      context.addIssue({ code: "custom", message: "Analyzed News requires sentiment." });
    if (item.analysisStatus !== "ANALYZED" && item.sentiment !== null)
      context.addIssue({ code: "custom", message: "Incomplete analysis cannot expose sentiment." });
  });
export const newsPageSchema = paginationSchema.extend({ items: z.array(newsItemSchema) });
