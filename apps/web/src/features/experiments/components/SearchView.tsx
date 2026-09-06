"use client";
import { useCallback, useEffect, useState } from "react";
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
  const activeView: CandidatePipelineView =
    view?.toUpperCase() === "FAILED" ? "FAILED" : view?.toUpperCase() === "ALL" ? "ALL" : "RESULTS";
  const [pipelineSignal, setPipelineSignal] = useState(0);
  const [selectedCandidate, setSelectedCandidate] = useState<CandidatePipelineItem>();
  useEffect(() => {
    if (monitor.status === "success" && monitor.experiment) {
      rememberExperiment(monitor.experiment.experimentId, activeView.toLowerCase());
    }
  }, [activeView, monitor.experiment, monitor.status]);
  const monitorRefresh = monitor.refresh;
  const refreshExperiment = useCallback(() => {
    void monitorRefresh();
  }, [monitorRefresh]);
  const refreshPipeline = useCallback(() => setPipelineSignal((value) => value + 1), []);
  const rt = useExperimentRealtime(
    realtime,
    id,
    refreshExperiment,
    refreshPipeline,
    monitor.experiment ? terminalExperiment(monitor.experiment.status) : false
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
            onRefresh={refreshExperiment}
          />
          {monitor.experiment.searchJob && (
            <JobProgressList jobs={[monitor.experiment.searchJob]} title="Search coordinator" />
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
