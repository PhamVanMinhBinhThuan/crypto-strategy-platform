import type { ApiClient } from "@/src/foundation/http/contracts";
import type { ExperimentDraft } from "../types/experiment-configuration";
import { draftPayload } from "../types/experiment-configuration";
export type ExperimentAcceptedResponse = Readonly<{
  experimentId: string;
  jobId: string;
  status: string;
}>;
const json = (key: string, body: unknown): RequestInit => ({
  method: "POST",
  headers: { "Content-Type": "application/json", "Idempotency-Key": key },
  body: JSON.stringify(body)
});
export const createExperimentCommandService = (api: ApiClient) => ({
  stop: (id: string, key: string) =>
    api.request(`/api/v1/experiments/${encodeURIComponent(id)}/stop`, json(key, {})),
  start: (draft: ExperimentDraft, key: string) =>
    api.request<ExperimentAcceptedResponse>("/api/v1/experiments", json(key, draftPayload(draft))),
  reproduce: (id: string, key: string) =>
    api.request<ExperimentAcceptedResponse>(
      `/api/v1/experiments/${encodeURIComponent(id)}/reproductions`,
      json(key, {})
    ),
  strategies: () => api.request("/api/v1/strategies")
});
