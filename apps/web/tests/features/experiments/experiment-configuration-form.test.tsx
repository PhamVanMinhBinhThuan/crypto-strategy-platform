import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { ExperimentConfigurationForm } from "@/src/features/experiments/components/ExperimentConfigurationForm";
import { strategyDescriptorPage } from "@/src/features/experiments/fixtures/experiment-configuration-fixtures";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";

const withCatalog = (api = new MockApiClient()) =>
  api
    .respond("GET /api/v1/strategies", strategyDescriptorPage)
    .respond("GET /api/v1/user-strategies", {
      items: [],
      nextCursor: null,
      hasMore: false
    });

describe("Experiment configuration form", () => {
  it("has semantic labels, fixture source indicators, parameter ranges and keyboard-reachable actions", async () => {
    render(<ExperimentConfigurationForm api={withCatalog()} fixture />);
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
      expect(await screen.findByLabelText(name, { exact: true })).toBeInTheDocument();
    expect(screen.getByText("FIXTURE DATA")).toBeInTheDocument();
    expect(screen.getByText(/Fixture profile/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start Experiment" })).toBeEnabled();
  });
  it("announces real acceptance and preserves the submitted draft", async () => {
    const api = withCatalog().respond("POST /api/v1/experiments", {
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
    await screen.findByRole("option", { name: "Moving Average Crossover" });
    await user.click(screen.getByRole("button", { name: "Start Experiment" }));
    expect(
      await screen.findByRole("link", { name: /Open Experiment experiment-new/ })
    ).toHaveAttribute("href", "/search?id=experiment-new");
    expect(screen.getByLabelText("Name")).toHaveValue("My experiment");
    expect(screen.getByLabelText("Dataset ID", { exact: true })).toHaveValue("dataset-known");
    const request = api.requests.find((item) => item.path === "/api/v1/experiments");
    expect(JSON.parse(String(request?.init.body))).toMatchObject({
      generator: { generatorId: "random-search", version: "1.0.0" },
      searchSpace: {
        strategyId: "ma-crossover",
        strategyVersion: "1.0.0",
        parameters: {
          fastPeriod: { minimum: 2, maximum: 50 },
          slowPeriod: { minimum: 10, maximum: 200 },
          priceSource: { options: ["OPEN", "CLOSE"] }
        }
      },
      stopCondition: { maximumCandidates: 100, maximumDurationSeconds: 300 }
    });
  });

  it("creates a real immutable dataset and selects it for the experiment", async () => {
    const api = withCatalog().respond("POST /api/v1/datasets", {
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
