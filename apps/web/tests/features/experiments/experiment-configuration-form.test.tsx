import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { ExperimentConfigurationForm } from "@/src/features/experiments/components/ExperimentConfigurationForm";
import { strategyDescriptorPage } from "@/src/features/experiments/fixtures/experiment-configuration-fixtures";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";

const frozenDataset = {
  datasetId: "01M1M383AJRDGS3BVC4KCE84Q2",
  version: "candle-v1",
  provider: "BINANCE",
  pair: "BTC/USDT",
  timeframe: "1h",
  normalizationVersion: "binance-v1",
  startTime: "2026-09-03T00:00:00Z",
  endTime: "2026-09-04T00:00:00Z",
  membershipCount: 24,
  checksum: `sha256:${"a".repeat(64)}`,
  status: "READY",
  createdAt: "2026-09-04T00:00:01Z"
};

const withCatalog = (api = new MockApiClient()) =>
  api
    .respond("GET /api/v1/strategies", strategyDescriptorPage)
    .respond("GET /api/v1/datasets?limit=50", { items: [] })
    .respond("GET /api/v1/search/generators", {
      items: [{ generatorId: "random-search", version: "1.0.0", displayName: "Random Search" }]
    })
    .respond("GET /api/v1/user-strategies", {
      items: [],
      nextCursor: null,
      hasMore: false
    });

describe("Experiment configuration form", () => {
  it("has semantic labels, fixture source indicators, parameter ranges and keyboard-reachable actions", async () => {
    render(<ExperimentConfigurationForm api={withCatalog()} fixture />);
    await screen.findByLabelText("Include Moving Average Crossover", { exact: true });
    for (const name of [
      "Name",
      "Pair",
      "Timeframe",
      "Start UTC",
      "End UTC",
      "Frozen Dataset",
      "Initial simulated capital",
      "Transaction fee (%)",
      "Slippage (%)",
      "Generator",
      "Generator version",
      "Seed",
      "Include Moving Average Crossover",
      "ma-crossover fastPeriod minimum",
      "ma-crossover fastPeriod maximum",
      "Minimum components",
      "Maximum components",
      "Combination policy",
      "Worker concurrency",
      "Maximum candidates",
      "Maximum duration (seconds)",
      "Candidates without improvement",
      "Top-K"
    ])
      expect(screen.getByLabelText(name, { exact: true })).toBeInTheDocument();
    expect(screen.getByText("FIXTURE DATA")).toBeInTheDocument();
    expect(screen.getByText(/Fixture profile/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start Experiment" })).toBeEnabled();
  });
  it("announces real acceptance and preserves the submitted draft", async () => {
    const api = withCatalog()
      .respond("POST /api/v1/datasets", frozenDataset)
      .respond("POST /api/v1/experiments", {
        experimentId: "experiment-new",
        jobId: "job-new",
        status: "QUEUED"
      });
    render(<ExperimentConfigurationForm api={api} fixture={false} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Start Experiment" }));
    expect(screen.getAllByRole("alert").length).toBeGreaterThan(0);
    await user.type(screen.getByLabelText("Name"), "My experiment");
    await user.clear(screen.getByLabelText("Initial simulated capital"));
    await user.type(screen.getByLabelText("Initial simulated capital"), "25000.50");
    await user.clear(screen.getByLabelText("Transaction fee (%)"));
    await user.type(screen.getByLabelText("Transaction fee (%)"), "0.1");
    await user.clear(screen.getByLabelText("Slippage (%)"));
    await user.type(screen.getByLabelText("Slippage (%)"), "0.05");
    const select = screen.getByLabelText("Frozen Dataset", { exact: true });
    expect(select).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Create dataset" }));
    expect(await screen.findByText(/Frozen dataset ready with 24 candles/)).toBeInTheDocument();
    await screen.findByLabelText("Include Moving Average Crossover");
    await user.click(screen.getByRole("button", { name: "Start Experiment" }));
    expect(
      await screen.findByText("Experiment accepted. Opening its authoritative monitor…")
    ).toBeInTheDocument();
    expect(window.location.pathname).toBe("/search/experiment-new");
    expect(screen.getByLabelText("Name")).toHaveValue("My experiment");
    const request = api.requests.find((item) => item.path === "/api/v1/experiments");
    expect(JSON.parse(String(request?.init.body))).toMatchObject({
      generator: { generatorId: "random-search", version: "1.0.0" },
      configurationVersion: 2,
      backtestConfiguration: {
        initialCapital: "25000.50",
        feeRate: "0.001",
        slippageRate: "0.0005"
      },
      searchSpace: {
        schemaVersion: 2,
        strategyPool: [
          {
            artifactType: "BUILT_IN",
            strategyId: "ma-crossover",
            version: "1.0.0",
            parameterDomains: {
              fastPeriod: { kind: "INTEGER_RANGE", min: 2, max: 50, step: 1 },
              slowPeriod: { kind: "INTEGER_RANGE", min: 10, max: 200, step: 1 },
              priceSource: { kind: "CHOICES", values: ["OPEN", "CLOSE"] }
            }
          }
        ],
        minComponents: 1,
        maxComponents: 1,
        combinationPolicy: { policyId: "majority-vote", version: "1.0.0", configuration: {} },
        constraints: [
          {
            kind: "PARAMETER_LT",
            left: "ma-crossover.fastPeriod",
            right: "ma-crossover.slowPeriod"
          }
        ]
      },
      stopConditions: { maximumCandidates: 100, maximumDurationSeconds: 300 },
      requestedConcurrency: 4
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

    expect(await screen.findByText(/Frozen dataset ready with 288 candles/)).toBeInTheDocument();
    expect(screen.getByLabelText("Frozen Dataset", { exact: true })).toHaveValue(
      "01M1M383AJRDGS3BVC4KCE84Q2"
    );
    const request = api.requests.find((item) => item.path === "/api/v1/datasets");
    expect(JSON.parse(String(request?.init.body))).toMatchObject({
      pair: "BTC/USDT",
      timeframe: "1h"
    });
    expect(JSON.parse(String(request?.init.body)).startTime).not.toBe(
      JSON.parse(String(request?.init.body)).endTime
    );
  });
});
