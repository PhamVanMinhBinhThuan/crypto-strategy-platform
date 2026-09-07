import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { candidatePipelinePage } from "@/src/features/experiments/fixtures/experiment-job-fixtures";
import { useCandidatePipeline } from "@/src/features/experiments/hooks/useCandidatePipeline";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";

describe("Candidate pipeline realtime reconciliation", () => {
  it("reloads the visible candidate page when a realtime update is announced", async () => {
    const path = "/api/v1/experiments/experiment-013/candidate-pipeline?view=RESULTS&limit=10";
    const api = new MockApiClient().respond(path, candidatePipelinePage);
    const { result } = renderHook(() => useCandidatePipeline(api, "experiment-013", "RESULTS"));

    await waitFor(() => expect(result.current.page?.items).toHaveLength(1));
    expect(api.requests).toHaveLength(1);

    act(() => result.current.notifyUpdate());

    await waitFor(() => expect(api.requests).toHaveLength(2));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.stale).toBe(false);
  });
});
