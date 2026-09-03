import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ExperimentActions } from "@/src/features/experiments/components/ExperimentActions";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { mapExperiment } from "@/src/features/experiments/mappers/experiment-job-mappers";
import { runningExperiment } from "@/src/features/experiments/fixtures/experiment-job-fixtures";
import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";
describe("Experiment Stop actions", () => {
  it("confirms, suppresses rapid submission and restores focus without fabricating STOPPED", async () => {
    let resolve!: (v: ApiResult<unknown>) => void;
    const request = vi.fn(() => new Promise<ApiResult<unknown>>((next) => (resolve = next)));
    const api = { request } as ApiClient;
    render(
      <ExperimentActions
        api={api}
        experiment={mapExperiment(runningExperiment)}
        onRefresh={() => {}}
      />
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole("button", { name: "Stop Experiment" });
    await user.click(trigger);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    const confirm = screen.getByRole("button", { name: "Confirm stop" });
    await user.dblClick(confirm);
    await waitFor(() => expect(request).toHaveBeenCalledTimes(1));
    expect(trigger).toHaveFocus();
    resolve({ ok: true, data: { status: "STOP_REQUESTED" } });
    await screen.findByText(/Stop requested/);
    expect(screen.queryByText(/^STOPPED$/)).not.toBeInTheDocument();
  });
  it("refreshes on conflict", async () => {
    const refresh = vi.fn();
    const api = new MockApiClient().respond("POST /api/v1/experiments/experiment-013/stop", {
      ok: false,
      error: { code: "INVALID_STATE_TRANSITION", message: "changed", retryable: false }
    });
    render(
      <ExperimentActions
        api={api}
        experiment={mapExperiment(runningExperiment)}
        onRefresh={refresh}
      />
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Stop Experiment" }));
    await user.click(screen.getByRole("button", { name: "Confirm stop" }));
    await waitFor(() => expect(refresh).toHaveBeenCalledOnce());
  });
});
