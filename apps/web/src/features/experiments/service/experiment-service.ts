import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";
import { mapCandidatePage, mapExperiment, mapJob } from "../mappers/experiment-job-mappers";
const safe = async <T>(
  promise: Promise<ApiResult<unknown>>,
  mapper: (v: unknown) => T
): Promise<ApiResult<T>> => {
  const r = await promise;
  if (!r.ok)
    return ["RESOURCE_NOT_FOUND", "FORBIDDEN", "EXPERIMENT_NOT_FOUND", "JOB_NOT_FOUND"].includes(
      r.error.code
    )
      ? {
          ok: false,
          error: { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false }
        }
      : r;
  try {
    return { ...r, data: mapper(r.data) };
  } catch {
    return {
      ok: false,
      error: {
        code: "INVALID_RESPONSE",
        message: "The service returned an invalid snapshot.",
        retryable: false
      }
    };
  }
};
export const createExperimentService = (api: ApiClient) => ({
  readExperiment: (id: string) =>
    safe(api.request(`/api/v1/experiments/${encodeURIComponent(id)}`), mapExperiment),
  readJob: (id: string) => safe(api.request(`/api/v1/jobs/${encodeURIComponent(id)}`), mapJob),
  readCandidates: (id: string, cursor?: string) =>
    safe(
      api.request(
        `/api/v1/experiments/${encodeURIComponent(id)}/candidates?limit=50${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ""}`
      ),
      mapCandidatePage
    ),
  readCandidate: (eid: string, cid: string) =>
    api.request(
      `/api/v1/experiments/${encodeURIComponent(eid)}/candidates/${encodeURIComponent(cid)}`
    )
});
