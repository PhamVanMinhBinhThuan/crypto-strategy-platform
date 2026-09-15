import type { z } from "zod";
import type {
  newsItemSchema,
  newsPageSchema,
  sentimentSchema,
  sentimentServiceStatusSchema
} from "../api/schemas";

export type Sentiment = z.infer<typeof sentimentSchema>;
export type NewsItem = z.infer<typeof newsItemSchema>;
export type NewsPage = z.infer<typeof newsPageSchema>;
export type SentimentServiceStatus = z.infer<typeof sentimentServiceStatusSchema>;

export type NewsAnalysisStatus = NewsItem["analysisStatus"];

export type NewsQuery = Readonly<{
  statuses?: ReadonlyArray<NewsAnalysisStatus>;
  cursor?: string;
  limit?: number;
}>;
