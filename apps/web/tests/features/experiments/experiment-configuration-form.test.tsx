import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { ExperimentConfigurationForm } from "@/src/features/experiments/components/ExperimentConfigurationForm";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
describe("Experiment configuration form", () => {
  it("has semantic labels, fixture source indicators, parameter ranges and keyboard-reachable actions", () => {
    render(
      <ExperimentConfigurationForm api={new MockApiClient()} fixture reproduceId="experiment-013" />
    );
    for (const name of [
      "Name",
      "Dataset ID",
      "Generator",
      "Generator version",
      "Seed",
      "Strategy",
      "Strategy version",
      "fastPeriod minimum",
      "fastPeriod maximum",
      "Maximum candidates",
      "Maximum duration (seconds)",
      "Top-K"
    ])
      expect(screen.getByLabelText(name, { exact: true })).toBeInTheDocument();
    expect(screen.getByText("FIXTURE DATA")).toBeInTheDocument();
    expect(screen.getByText(/Fixture-only discovery/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start Experiment" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Reproduce Experiment" })).toBeEnabled();
  });
  it("announces validation and preserves draft after dependency failure", async () => {
    const api = new MockApiClient().respond("POST /api/v1/experiments", {
      ok: false,
      error: {
        code: "DEPENDENCY_UNAVAILABLE",
        message: "BLOCKED_SEARCH_COORDINATOR",
        retryable: true
      }
    });
    render(<ExperimentConfigurationForm api={api} fixture={false} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Start Experiment" }));
    expect(screen.getAllByRole("alert").length).toBeGreaterThan(0);
    await user.type(screen.getByLabelText("Name"), "My experiment");
    await user.type(screen.getByLabelText("Dataset ID", { exact: true }), "dataset-known");
    await user.click(screen.getByRole("button", { name: "Start Experiment" }));
    await screen.findByText(/Production start remains blocked/);
    expect(screen.getByLabelText("Name")).toHaveValue("My experiment");
    expect(screen.getByLabelText("Dataset ID", { exact: true })).toHaveValue("dataset-known");
  });
});
