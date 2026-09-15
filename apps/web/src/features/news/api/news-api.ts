import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";
import { requestPublic } from "../../shared/feature-api";
import { newsPageSchema, sentimentServiceStatusSchema } from "./schemas";
import type { NewsPage, NewsQuery, SentimentServiceStatus } from "../model/news";

export async function listNewsItems(
  api: ApiClient,
  query: NewsQuery
): Promise<ApiResult<NewsPage>> {
  const searchParams = new URLSearchParams();
  if (query.limit) searchParams.set("limit", query.limit.toString());
  if (query.cursor) searchParams.set("cursor", query.cursor);
  query.statuses?.forEach((status) => searchParams.append("analysisStatus", status));
  const suffix = searchParams.size ? `?${searchParams}` : "";
  return requestPublic(api, newsPageSchema, `/api/v1/news-items${suffix}`);
}

export async function getSentimentServiceStatus(
  api: ApiClient
): Promise<ApiResult<SentimentServiceStatus>> {
  return requestPublic(api, sentimentServiceStatusSchema, "/api/v1/news-items/sentiment-status");
}
