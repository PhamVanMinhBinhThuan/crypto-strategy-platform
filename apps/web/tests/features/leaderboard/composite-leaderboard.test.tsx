import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CandidateDetailPanel } from "@/src/features/experiments/components/CandidateDetailPanel";
import { LeaderboardTable } from "@/src/features/leaderboard/components/LeaderboardTable";
import { leaderboardPage } from "@/src/features/leaderboard/fixtures/leaderboard-fixtures";
import { mapLeaderboard } from "@/src/features/leaderboard/mappers/leaderboard-mapper";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";

vi.mock("next/navigation", () => ({
  usePathname: () => "/search/experiment-013",
  useRouter: () => ({ replace: vi.fn() }),
  useSearchParams: () => new URLSearchParams("view=results&candidateId=candidate-013")
}));

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
        version: "candle-v1",
        checksum: `sha256:${"d".repeat(64)}`,
        provider: "binance",
        pair: "BTC/USDT",
        timeframe: "1h",
        normalizationVersion: "binance-v1",
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
      },
      backtest: {
        jobId: "job-013",
        status: "SUCCEEDED",
        backtestResultId: "result-013",
        startedAt: "2026-01-01T00:00:00Z",
        finishedAt: "2026-07-01T00:00:00Z",
        attemptNo: 1,
        nextRetryAt: null,
        retryable: false,
        failure: null
      },
      evaluation: {
        status: "SUCCEEDED",
        evaluationResultId: "evaluation-013",
        score: "0.84",
        totalReturn: "0.425",
        winRate: "0.582",
        maximumDrawdown: "0.0831",
        numberOfTrades: 1245,
        metricVersion: "metric-v1",
        eligible: true,
        eligibilityReason: null,
        evaluatedAt: "2026-07-01T00:00:00Z"
      },
      ranking: { status: "RANKED", rank: 1, rankingVersion: "ranking-v1" },
      failureStage: null
    });

    render(
      <CandidateDetailPanel api={api} experimentId="experiment-013" candidateId="candidate-013" />
    );

    expect(await screen.findByText("Candidate #8")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Moving Average Crossover/ })).toBeInTheDocument();
    expect(screen.getByText(/Fast period 10/)).toBeInTheDocument();
    expect(screen.getByText(`sha256:${"d".repeat(64)}`)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "View full backtest result" })).toHaveAttribute(
      "href",
      "/backtests?resultId=result-013&returnTo=%2Fsearch%2Fexperiment-013%3Fview%3Dresults"
    );
    expect(screen.queryByText(/Sharpe/i)).not.toBeInTheDocument();
  });

  it("shows the pipeline failure snapshot when the candidate detail request is unavailable", async () => {
    const api = new MockApiClient();
    const failedCandidate = {
      candidateId: "candidate-failed",
      generationIndex: 8,
      definition: { strategyId: "rsi", parameters: { period: 14 } },
      candidateSummary: "rsi",
      candidateFingerprint: "sha256:failed",
      createdAt: "2026-07-01T00:00:00Z",
      backtest: {
        jobId: "job-failed",
        status: "FAILED",
        backtestResultId: null,
        startedAt: "2026-07-01T00:00:00Z",
        finishedAt: "2026-07-01T00:01:00Z",
        attemptNo: 3,
        nextRetryAt: null,
        retryable: false,
        failure: { code: "BACKTEST_FAILED", message: "Backtest execution failed." }
      },
      evaluation: {
        status: "NOT_STARTED",
        evaluationResultId: null,
        score: null,
        totalReturn: null,
        winRate: null,
        maximumDrawdown: null,
        numberOfTrades: null,
        metricVersion: null,
        eligible: null,
        eligibilityReason: null,
        evaluatedAt: null
      },
      ranking: { status: "NOT_STARTED", rank: null, rankingVersion: null },
      failureStage: "BACKTEST"
    } as const;

    render(
      <CandidateDetailPanel
        api={api}
        experimentId="experiment-013"
        candidateId="candidate-failed"
        fallbackCandidate={failedCandidate}
        returnView="failed"
      />
    );

    expect(await screen.findByText("Backtest execution failed.")).toBeInTheDocument();
    expect(screen.queryByText("Unable to load candidate details")).not.toBeInTheDocument();
    expect(screen.getByText("candidate-failed")).toBeInTheDocument();
  });
});
