"use client";
import { useEffect, useState } from "react";
import { useClients } from "@/src/foundation/composition/client-provider";
import { terminalExperiment } from "../types/experiment";
import { useExperimentMonitor } from "../hooks/useExperimentMonitor";
import { useExperimentRealtime } from "../hooks/useExperimentRealtime";
import { ExperimentSummary } from "./ExperimentSummary";
import { JobProgressList } from "./JobProgressList";
import { ExperimentActions } from "./ExperimentActions";
import { ExperimentConfigurationForm } from "./ExperimentConfigurationForm";
import { RealtimeStatus } from "./RealtimeStatus";
import { CandidateDetailPanel } from "./CandidateDetailPanel";
import { CandidatePipelineTabs } from "./CandidatePipelineTabs";
import type { CandidatePipelineItem, CandidatePipelineView } from "../types/experiment";
import { rememberExperiment } from "@/src/foundation/navigation/resource-history";
import Link from "next/link";
import { RecentExperiments } from "./RecentExperiments";
import { useLeaderboard } from "@/src/features/leaderboard/hooks/useLeaderboard";
import { useLeaderboardRealtime } from "@/src/features/leaderboard/hooks/useLeaderboardRealtime";
import { LeaderboardControls } from "@/src/features/leaderboard/components/LeaderboardControls";
import { LeaderboardTable } from "@/src/features/leaderboard/components/LeaderboardTable";
import { useDebouncedRefresh } from "@/src/foundation/ui/useDebouncedRefresh";
export function SearchView({
  id,
  candidateId,
  view,
  mode,
  initialUserStrategyVersionId
}: {
  id?: string;
  candidateId?: string;
  view?: string;
  mode?: string;
  initialUserStrategyVersionId?: string;
}) {
  const { api, realtime, fixtures } = useClients();
  const monitor = useExperimentMonitor(api, id);
  const leaderboard = useLeaderboard(api, id);
  const activeView: CandidatePipelineView =
    view?.toUpperCase() === "FAILED" ? "FAILED" : view?.toUpperCase() === "ALL" ? "ALL" : "RESULTS";
  const [pipelineSignal, setPipelineSignal] = useState(0);
  const [selectedCandidate, setSelectedCandidate] = useState<CandidatePipelineItem>();
  useEffect(() => {
    if (monitor.status === "success" && monitor.experiment) {
      rememberExperiment(monitor.experiment.experimentId, activeView.toLowerCase());
    }
  }, [activeView, monitor.experiment, monitor.status]);
  const experimentRefresh = useDebouncedRefresh(monitor.refresh);
  const pipelineRefresh = useDebouncedRefresh(() => setPipelineSignal((value) => value + 1));
  const leaderboardRefresh = useDebouncedRefresh(leaderboard.refresh);
  const rt = useExperimentRealtime(
    realtime,
    id,
    experimentRefresh.schedule,
    pipelineRefresh.schedule,
    monitor.experiment ? terminalExperiment(monitor.experiment.status) : false
  );
  useLeaderboardRealtime(
    realtime,
    id,
    leaderboard.snapshot?.revision ?? 0,
    leaderboardRefresh.schedule
  );
  if (!id)
    return (
      <main className="feature-page">
        <header className="feature-header">
          <div>
            <p className="eyebrow">Contract-driven search</p>
            <h1>Search &amp; Leaderboard</h1>
            <p className="muted">
              Review previous searches or configure a reproducible experiment.
            </p>
          </div>
        </header>
        {mode === "new" || initialUserStrategyVersionId ? (
          <>
            <div className="page-actions">
              <Link className="button secondary" href="/search">
                All experiments
              </Link>
            </div>
            <ExperimentConfigurationForm
              api={api}
              fixture={fixtures}
              initialUserStrategyVersionId={initialUserStrategyVersionId}
            />
          </>
        ) : (
          <RecentExperiments api={api} />
        )}
      </main>
    );
  return (
    <main className="feature-page">
      <RealtimeStatus
        value={rt.connection}
        error={rt.subscriptionError}
        onReconnect={() => void rt.reconnect()}
      />
      {(experimentRefresh.pending || pipelineRefresh.pending || leaderboardRefresh.pending) && (
        <p className="live-update-status" role="status">
          Updating live experiment data…
        </p>
      )}
      {monitor.status === "loading" && !monitor.experiment && (
        <p role="status">Loading authoritative experiment snapshot…</p>
      )}
      {monitor.error && (
        <section role="alert" className="panel error-state">
          <p>{monitor.error}</p>
          <Link className="button secondary" href="/search">
            All experiments
          </Link>
        </section>
      )}
      {monitor.experiment && (
        <>
          <ExperimentSummary experiment={monitor.experiment} />
          <nav className="page-actions" aria-label="Experiment navigation">
            <Link className="button secondary" href="/search">
              All experiments
            </Link>
            <Link className="button secondary" href="/search?mode=new">
              New experiment
            </Link>
          </nav>
          <ExperimentActions
            api={api}
            experiment={monitor.experiment}
            onRefresh={() => void monitor.refresh()}
          />
          {monitor.experiment.searchJob && (
            <JobProgressList jobs={[monitor.experiment.searchJob]} title="Search coordinator" />
          )}
          {leaderboard.error && (
            <section className="panel error-state" role="alert">
              <p>{leaderboard.error}</p>
              <button className="button secondary" onClick={() => void leaderboard.refresh()}>
                Retry leaderboard
              </button>
            </section>
          )}
          {leaderboard.snapshot ? (
            <>
              <LeaderboardControls
                limit={leaderboard.limit}
                configuredTopK={leaderboard.snapshot.topK}
                onChange={leaderboard.setLimit}
              />
              <LeaderboardTable snapshot={leaderboard.snapshot} />
            </>
          ) : (
            !leaderboard.error && (
              <section className="panel" aria-live="polite">
                <h2>Leaderboard</h2>
                <p className="muted">Waiting for the first eligible candidate…</p>
              </section>
            )
          )}
          <CandidatePipelineTabs
            api={api}
            experimentId={monitor.experiment.experimentId}
            view={activeView}
            refreshVersion={pipelineSignal}
            selectedCandidateId={candidateId}
            onSelectedCandidateAvailable={setSelectedCandidate}
          />
        </>
      )}
      {id && candidateId && (
        <CandidateDetailPanel
          api={api}
          experimentId={id}
          candidateId={candidateId}
          fallbackCandidate={
            selectedCandidate?.candidateId === candidateId ? selectedCandidate : undefined
          }
          returnView={activeView.toLowerCase()}
        />
      )}
    </main>
  );
}
