import { MockApiClient } from "../testing/mock-api-client";
import { MockRealtimeClient } from "../testing/mock-realtime-client";
import { normalBacktestResult } from "../../features/backtests/fixtures/backtest-result-fixtures";
import {
  runningExperiment,
  runningJob,
  candidatePage
} from "../../features/experiments/fixtures/experiment-job-fixtures";
import { leaderboardPage } from "../../features/leaderboard/fixtures/leaderboard-fixtures";
export function createFixtureClients() {
  if (process.env.NODE_ENV === "production")
    throw new Error("Fixture clients cannot be composed in production");
  const api = new MockApiClient();
  api
    .respond("/api/v1/backtests/backtest-013/result", normalBacktestResult)
    .respond("/api/v1/backtest-results/result-013", {
      ...normalBacktestResult,
      backtestResultId: "result-013",
      backtestId: null
    })
    .respond("/api/v1/backtest-results/result-014", {
      ...normalBacktestResult,
      backtestResultId: "result-014",
      backtestId: null
    })
    .respond("/api/v1/experiments/experiment-013", runningExperiment)
    .respond("/api/v1/jobs/job-search-013", runningJob)
    .respond("/api/v1/experiments/experiment-013/candidates?limit=50", candidatePage)
    .respond("/api/v1/experiments/experiment-013/leaderboard?limit=10", leaderboardPage)
    .respond("POST /api/v1/experiments/experiment-013/stop", {
      experimentId: "experiment-013",
      status: "STOP_REQUESTED"
    })
    .respond("POST /api/v1/experiments", {
      experimentId: "experiment-fixture-new",
      jobId: "job-fixture-new",
      status: "QUEUED"
    })
    .respond("POST /api/v1/experiments/experiment-013/reproductions", {
      experimentId: "experiment-fixture-copy",
      jobId: "job-fixture-copy",
      status: "QUEUED"
    });
  return { api, realtime: new MockRealtimeClient(), fixtures: true as const };
}
