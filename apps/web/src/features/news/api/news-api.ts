import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";
import { requestPublic } from "../../shared/feature-api";
import { newsPageSchema } from "./schemas";
import type { NewsPage, NewsQuery } from "../model/news";

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
