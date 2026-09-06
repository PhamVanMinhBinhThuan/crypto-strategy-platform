import { MockApiClient } from "../testing/mock-api-client";
import { MockRealtimeClient } from "../testing/mock-realtime-client";
import {
  backtestResultHistoryPage,
  normalBacktestResult
} from "../../features/backtests/fixtures/backtest-result-fixtures";
import {
  runningExperiment,
  runningJob,
  candidatePage,
  candidatePipelinePage,
  experimentHistoryPage
} from "../../features/experiments/fixtures/experiment-job-fixtures";
import { leaderboardPage } from "../../features/leaderboard/fixtures/leaderboard-fixtures";
import {
  emptyUserStrategyPage,
  frozenDatasetFixture,
  generatorPage,
  strategyDescriptorPage
} from "../../features/experiments/fixtures/experiment-configuration-fixtures";
export function createFixtureClients() {
  if (process.env.NODE_ENV === "production")
    throw new Error("Fixture clients cannot be composed in production");
  const api = new MockApiClient();
  api
    .respond("/api/v1/experiments?limit=10", experimentHistoryPage)
    .respond("/api/v1/backtest-results?limit=10", backtestResultHistoryPage)
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
    .respond(
      "/api/v1/experiments/experiment-013/candidate-pipeline?view=RESULTS&limit=10",
      candidatePipelinePage
    )
    .respond("/api/v1/experiments/experiment-013/candidate-pipeline?view=FAILED&limit=10", {
      ...candidatePipelinePage,
      items: []
    })
    .respond(
      "/api/v1/experiments/experiment-013/candidate-pipeline?view=ALL&limit=10",
      candidatePipelinePage
    )
    .respond("/api/v1/experiments/experiment-013/leaderboard?limit=10", leaderboardPage)
    .respond("/api/v1/strategies", strategyDescriptorPage)
    .respond("/api/v1/user-strategies", emptyUserStrategyPage)
    .respond("/api/v1/datasets?limit=50", {
      items: [frozenDatasetFixture],
      nextCursor: null,
      hasMore: false,
      totalCount: 1
    })
    .respond(`/api/v1/datasets/${frozenDatasetFixture.datasetId}`, frozenDatasetFixture)
    .respond("/api/v1/search/generators", generatorPage)
    .respond("POST /api/v1/datasets", frozenDatasetFixture)
    .respond("/api/v1/experiments/experiment-013/candidates/candidate-013", {
      candidateId: "candidate-013",
      generationIndex: 42,
      definition: {
        schemaVersion: 2,
        kind: "COMPOSITE",
        combinationPolicy: { policyId: "majority-vote", version: "1.0.0" },
        components: [
          {
            strategyId: "ma-crossover",
            version: "1.0.0",
            parameters: { fastPeriod: 12, slowPeriod: 64 }
          },
          {
            strategyId: "rsi",
            version: "1.0.0",
            parameters: { period: 14, buyThreshold: 30, sellThreshold: 70 }
          }
        ]
      },
      generatorState: { seed: 20260903, generationIndex: 42 },
      candidateFingerprint: "sha256:candidate013",
      dataset: {
        datasetId: frozenDatasetFixture.datasetId,
        version: frozenDatasetFixture.version,
        checksum: frozenDatasetFixture.checksum,
        provider: frozenDatasetFixture.provider,
        pair: frozenDatasetFixture.pair,
        timeframe: frozenDatasetFixture.timeframe,
        normalizationVersion: frozenDatasetFixture.normalizationVersion,
        startTime: frozenDatasetFixture.startTime,
        endTime: frozenDatasetFixture.endTime,
        candleCount: frozenDatasetFixture.membershipCount
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
      backtest: candidatePipelinePage.items[0].backtest,
      evaluation: candidatePipelinePage.items[0].evaluation,
      ranking: candidatePipelinePage.items[0].ranking,
      failureStage: null
    })
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
