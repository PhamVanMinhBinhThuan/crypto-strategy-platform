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
  it("announces real acceptance and preserves the submitted draft", async () => {
    const api = new MockApiClient().respond("POST /api/v1/experiments", {
      experimentId: "experiment-new",
      jobId: "job-new",
      status: "QUEUED"
    });
    render(<ExperimentConfigurationForm api={api} fixture={false} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Start Experiment" }));
    expect(screen.getAllByRole("alert").length).toBeGreaterThan(0);
    await user.type(screen.getByLabelText("Name"), "My experiment");
    await user.type(screen.getByLabelText("Dataset ID", { exact: true }), "dataset-known");
    await user.click(screen.getByRole("button", { name: "Start Experiment" }));
    expect(
      await screen.findByRole("link", { name: /Open Experiment experiment-new/ })
    ).toHaveAttribute("href", "/search?id=experiment-new");
    expect(screen.getByLabelText("Name")).toHaveValue("My experiment");
    expect(screen.getByLabelText("Dataset ID", { exact: true })).toHaveValue("dataset-known");
  });

  it("creates a real immutable dataset and selects it for the experiment", async () => {
    const api = new MockApiClient().respond("POST /api/v1/datasets", {
      datasetId: "01M1M383AJRDGS3BVC4KCE84Q2",
      version: "candle-v1",
      provider: "BINANCE",
      pair: "BTC/USDT",
      timeframe: "5m",
      normalizationVersion: "binance-v1",
      startTime: "2026-09-03T00:00:00Z",
      endTime: "2026-09-04T00:00:00Z",
      membershipCount: 288,
      checksum: `sha256:${"a".repeat(64)}`,
      status: "READY",
      createdAt: "2026-09-04T00:00:01Z"
    });
    render(<ExperimentConfigurationForm api={api} fixture={false} />);

    await userEvent.click(screen.getByRole("button", { name: "Create dataset" }));

    expect(await screen.findByRole("status")).toHaveTextContent("288 candles");
    expect(screen.getByLabelText("Dataset ID", { exact: true })).toHaveValue(
      "01M1M383AJRDGS3BVC4KCE84Q2"
    );
  });
});
