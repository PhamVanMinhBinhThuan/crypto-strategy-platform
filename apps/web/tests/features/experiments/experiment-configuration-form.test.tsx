import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ExperimentConfigurationForm } from "@/src/features/experiments/components/ExperimentConfigurationForm";
import { strategyDescriptorPage } from "@/src/features/experiments/fixtures/experiment-configuration-fixtures";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";

const { pushMock } = vi.hoisted(() => ({ pushMock: vi.fn() }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: pushMock }) }));

const frozenDataset = {
  datasetId: "01M1M383AJRDGS3BVC4KCE84Q2",
  version: "candle-v1",
  provider: "BINANCE",
  pair: "BTC/USDT",
  timeframe: "1h",
  normalizationVersion: "binance-v1",
  startTime: "2026-09-03T00:00:00Z",
  endTime: "2026-09-04T00:00:00Z",
  membershipCount: 120,
  checksum: `sha256:${"a".repeat(64)}`,
  status: "READY",
  createdAt: "2026-09-04T00:00:01Z"
};

const publishedUserStrategyVersion = {
  userStrategyVersionId: "user-strategy-version-1",
  userStrategyId: "user-strategy-1",
  versionNo: 1,
  kind: "SINGLE" as const,
  source: {
    type: "SINGLE" as const,
    strategy: {
      strategyId: "ma-crossover",
      strategyVersionId: "strategy-version-013",
      version: "1.0.0",
      parameters: { fastPeriod: "12", slowPeriod: "64", priceSource: "CLOSE" }
    }
  },
  status: "PUBLISHED" as const,
  fingerprint: "strategy-v1:published-user-strategy",
  publishedAt: "2026-09-04T01:01:00Z",
  createdAt: "2026-09-04T01:00:00Z"
};

const withPublishedUserStrategy = () =>
  withCatalog()
    .respond("GET /api/v1/user-strategies", {
      items: [
        {
          userStrategyId: "user-strategy-1",
          kind: "SINGLE",
          name: "My published strategy",
          description: "Fixed configuration",
          createdAt: "2026-09-04T01:00:00Z"
        }
      ],
      nextCursor: null,
      hasMore: false
    })
    .respond("GET /api/v1/user-strategies/user-strategy-1", {
      userStrategyId: "user-strategy-1",
      kind: "SINGLE",
      name: "My published strategy",
      description: "Fixed configuration",
      status: "ACTIVE",
      archivedAt: null,
      createdAt: "2026-09-04T01:00:00Z",
      updatedAt: "2026-09-04T01:01:00Z",
      latestVersion: publishedUserStrategyVersion
    })
    .respond("GET /api/v1/user-strategies/user-strategy-1/versions", {
      items: [publishedUserStrategyVersion]
    });

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
  beforeEach(() => {
    pushMock.mockReset();
    window.localStorage.clear();
  });

  it("has semantic labels, fixture source indicators, parameter ranges and keyboard-reachable actions", async () => {
    render(<ExperimentConfigurationForm api={withCatalog()} fixture />);
    await screen.findByLabelText("Remove Moving Average Crossover", { exact: true });
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
      "Seed",
      "Remove Moving Average Crossover",
      "Moving Average Crossover fastPeriod minimum",
      "Moving Average Crossover fastPeriod maximum",
      "Parallel backtests",
      "Candidate limit",
      "Time limit (minutes)",
      "Leaderboard size"
    ])
      expect(screen.getByLabelText(name, { exact: true })).toBeInTheDocument();
    expect(screen.getByText("FIXTURE DATA")).toBeInTheDocument();
    expect(screen.getByText("Deterministic fixture mode")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start experiment" })).toBeEnabled();
  });

  it("automatically uses a leaderboard size of one for a fixed published strategy", async () => {
    render(
      <ExperimentConfigurationForm
        api={withPublishedUserStrategy()}
        fixture
        initialUserStrategyVersionId="user-strategy-version-1"
      />
    );

    const input = screen.getByLabelText("Leaderboard size");
    await waitFor(() => expect(input).toHaveValue(1));
    expect(input).toHaveAttribute("max", "1");
    expect(input).toHaveAttribute("readonly");
    expect(
      screen.getByText(
        "The selected search space has one fixed configuration, so the leaderboard size is 1."
      )
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Leaderboard size cannot exceed the candidates that can be evaluated.")
    ).not.toBeInTheDocument();
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
    await user.click(await screen.findByRole("button", { name: "Start experiment" }));
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
    await user.click(screen.getByRole("button", { name: "Create new frozen dataset" }));
    const selectedDataset = await screen.findByRole("region", {
      name: "Selected frozen dataset"
    });
    expect(within(selectedDataset).getByText("120")).toBeInTheDocument();
    await screen.findByLabelText("Remove Moving Average Crossover");
    await user.click(screen.getByRole("button", { name: "Start experiment" }));
    expect(
      await screen.findByText("Experiment accepted. Opening its authoritative monitor…")
    ).toBeInTheDocument();
    expect(pushMock).toHaveBeenCalledWith("/search/experiment-new");
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
              fastPeriod: { kind: "INTEGER_RANGE", min: 5, max: 20, step: 5 },
              slowPeriod: { kind: "INTEGER_RANGE", min: 20, max: 100, step: 10 },
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

    await userEvent.click(screen.getByRole("button", { name: "Create new frozen dataset" }));

    const selectedDataset = await screen.findByRole("region", {
      name: "Selected frozen dataset"
    });
    expect(within(selectedDataset).getByText("288")).toBeInTheDocument();
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
