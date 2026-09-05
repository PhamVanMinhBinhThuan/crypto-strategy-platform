import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CandidateDetailPanel } from "@/src/features/experiments/components/CandidateDetailPanel";
import { LeaderboardTable } from "@/src/features/leaderboard/components/LeaderboardTable";
import { leaderboardPage } from "@/src/features/leaderboard/fixtures/leaderboard-fixtures";
import { mapLeaderboard } from "@/src/features/leaderboard/mappers/leaderboard-mapper";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";

describe("F-015 composite leaderboard", () => {
  it("renders composite summary, four server metrics and authoritative actions without Sharpe", () => {
    render(<LeaderboardTable snapshot={mapLeaderboard(leaderboardPage)} />);

    expect(screen.getByText("ma-crossover + rsi")).toBeInTheDocument();
    expect(screen.getByText("0.425")).toBeInTheDocument();
    expect(screen.getByText("0.582")).toBeInTheDocument();
    expect(screen.getAllByText("0.0831").length).toBeGreaterThan(0);
    expect(screen.getByText("1245")).toBeInTheDocument();
    expect(screen.queryByText(/Sharpe/i)).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Candidate detail" })).toHaveAttribute(
      "href",
      "/search/experiment-013?candidateId=candidate-013"
    );
    expect(screen.getAllByRole("link", { name: "View Backtest" })[0]).toHaveAttribute(
      "href",
      "/backtests?resultId=result-013"
    );
  });

  it("loads immutable components, parameters, policy and frozen Backtest evidence", async () => {
    const detailPath = "/api/v1/experiments/experiment-013/candidates/candidate-013";
    const api = new MockApiClient().respond(detailPath, {
      candidateId: "candidate-013",
      generationIndex: 7,
      definition: {
        schemaVersion: 2,
        kind: "COMPOSITE",
        combinationPolicy: { policyId: "majority-vote", version: "1.0.0" },
        components: [
          {
            strategyId: "ma-crossover",
            strategyVersion: "1.0.0",
            parameters: { fastPeriod: { type: "INTEGER", value: 10 } }
          },
          {
            strategyId: "rsi",
            strategyVersion: "1.0.0",
            parameters: { period: { type: "INTEGER", value: 14 } }
          }
        ]
      },
      generatorState: { contractVersion: "random-state-v1" },
      candidateFingerprint: `sha256:${"c".repeat(64)}`,
      dataset: {
        datasetId: "dataset-013",
        checksum: `sha256:${"d".repeat(64)}`,
        provider: "binance",
        pair: "BTC/USDT",
        timeframe: "1h",
        startTime: "2026-01-01T00:00:00Z",
        endTime: "2026-07-01T00:00:00Z",
        candleCount: 4344
      },
      backtestResultId: "result-013",
      backtestStatus: "SUCCEEDED",
      metrics: {
        totalReturn: "0.425",
        winRate: "0.582",
        maximumDrawdown: "0.0831",
        numberOfTrades: 1245,
        metricVersion: "metric-v1"
      }
    });

    render(
      <CandidateDetailPanel
        api={api}
        experimentId="experiment-013"
        candidateId="candidate-013"
      />
    );

    expect(await screen.findByRole("heading", { name: "Candidate #8" })).toBeInTheDocument();
    expect(screen.getByText(/ma-crossover/)).toBeInTheDocument();
    expect(screen.getByText(/majority-vote/)).toBeInTheDocument();
    expect(screen.getByText(/fastPeriod/)).toBeInTheDocument();
    expect(screen.getByText(`sha256:${"d".repeat(64)}`)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "View authoritative Backtest" })).toHaveAttribute(
      "href",
      "/backtests?resultId=result-013"
    );
    expect(screen.queryByText(/Sharpe/i)).not.toBeInTheDocument();
  });
});
