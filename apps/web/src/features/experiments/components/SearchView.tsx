"use client";
import { useCallback } from "react";
import { useClients } from "@/src/foundation/composition/client-provider";
import { terminalExperiment } from "../types/experiment";
import { useExperimentMonitor } from "../hooks/useExperimentMonitor";
import { useLeaderboard } from "../../leaderboard/hooks/useLeaderboard";
import { useExperimentRealtime } from "../hooks/useExperimentRealtime";
import { useLeaderboardRealtime } from "../../leaderboard/hooks/useLeaderboardRealtime";
import { ExperimentStatus } from "./ExperimentStatus";
import { JobProgressList } from "./JobProgressList";
import { CandidateDiscoveryTimeline } from "./CandidateDiscoveryTimeline";
import { ExperimentActions } from "./ExperimentActions";
import { ExperimentConfigurationForm } from "./ExperimentConfigurationForm";
import { RealtimeStatus } from "./RealtimeStatus";
import { LeaderboardControls } from "../../leaderboard/components/LeaderboardControls";
import { LeaderboardTable } from "../../leaderboard/components/LeaderboardTable";
import { CandidateDetailPanel } from "./CandidateDetailPanel";
export function SearchView({ id, candidateId }: { id?: string; candidateId?: string }) {
  const { api, realtime, fixtures } = useClients();
  const monitor = useExperimentMonitor(api, id);
  const board = useLeaderboard(api, id);
  const monitorRefresh = monitor.refresh;
  const boardRefresh = board.refresh;
  const refreshExperiment = useCallback(() => {
    void monitorRefresh();
  }, [monitorRefresh]);
  const refreshBoard = useCallback(() => {
    void boardRefresh();
  }, [boardRefresh]);
  const rt = useExperimentRealtime(
    realtime,
    id,
    refreshExperiment,
    refreshExperiment,
    monitor.experiment ? terminalExperiment(monitor.experiment.status) : false
  );
  useLeaderboardRealtime(realtime, id, board.snapshot?.revision ?? 0, refreshBoard);
  if (!id)
    return (
      <main className="feature-page">
        <header className="feature-header">
          <div>
            <p className="eyebrow">F-013 · contract-driven search</p>
            <h1>Search &amp; Leaderboard</h1>
            <p className="muted">
              Configure a reproducible experiment or open an existing experiment ID.
            </p>
          </div>
        </header>
        <ExperimentConfigurationForm api={api} fixture={fixtures} />
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
          {monitor.error}
        </section>
      )}
      {monitor.experiment && (
        <>
          <ExperimentStatus experiment={monitor.experiment} />
          <ExperimentActions
            api={api}
            experiment={monitor.experiment}
            onRefresh={refreshExperiment}
          />
          <div className="search-grid">
            <JobProgressList jobs={monitor.jobs} />
            <CandidateDiscoveryTimeline candidates={monitor.candidates} />
          </div>
        </>
      )}
      {board.snapshot && (
        <>
          <LeaderboardControls
            limit={board.limit}
            configuredTopK={board.snapshot.topK}
            onChange={board.setLimit}
          />
          <LeaderboardTable snapshot={board.snapshot} />
        </>
      )}
      {id && candidateId && (
        <CandidateDetailPanel api={api} experimentId={id} candidateId={candidateId} />
      )}
      {board.error && <p role="alert">{board.error}</p>}
    </main>
  );
}
