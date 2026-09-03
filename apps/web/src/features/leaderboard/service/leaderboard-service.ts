import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";
import { mapLeaderboard } from "../mappers/leaderboard-mapper";
import { capLeaderboardLimit, type LeaderboardSnapshot } from "../types/leaderboard";
export const createLeaderboardService = (api: ApiClient) => ({
  async read(
    experimentId: string,
    limit = 10,
    cursor?: string,
    configuredTopK = 100
  ): Promise<ApiResult<LeaderboardSnapshot>> {
    const safe = capLeaderboardLimit(limit, configuredTopK);
    const result = await api.request(
      `/api/v1/experiments/${encodeURIComponent(experimentId)}/leaderboard?limit=${safe}${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ""}`
    );
    if (!result.ok) return result;
    try {
      return { ...result, data: mapLeaderboard(result.data) };
    } catch {
      return {
        ok: false,
        error: {
          code: "INVALID_RESPONSE",
          message: "The service returned an invalid leaderboard.",
          retryable: false
        }
      };
    }
  }
});
