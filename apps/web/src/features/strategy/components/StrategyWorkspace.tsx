"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useClients } from "@/src/foundation/composition/client-provider";
import type {
  StrategyDescriptor,
  UserStrategy,
  UserStrategySummary,
  UserStrategyVersion
} from "../model/strategy";
import type { StrategyDraft } from "../model/strategy-draft";
import {
  archiveUserStrategy,
  createUserStrategyVersion,
  createUserStrategy,
  getUserStrategy,
  listSystemStrategies,
  listUserStrategyVersions,
  listUserStrategies,
  publishUserStrategyVersion
} from "../api/strategy-api";
import { StrategyMutationController } from "../state/strategy-controller";
import { StrategyCatalog } from "./StrategyCatalog";
import { StrategyDetail } from "./StrategyDetail";
import { StrategyForm } from "./StrategyForm";
import { StrategyActions } from "./StrategyActions";
import { StrategyVersionForm } from "./StrategyVersionForm";
import { StrategyVersionHistory } from "./StrategyVersionHistory";
import { StrategyParameters } from "./StrategyParameters";
import { AsyncStatus } from "../../shared/AsyncStatus";

type MutationOutcome = { ok: boolean; error?: { code: string } };
type WorkspaceTab = "overview" | "parameters" | "versions" | "backtests";
const workspaceTabs: readonly { id: WorkspaceTab; label: string }[] = [
  { id: "overview", label: "Overview" },
  { id: "parameters", label: "Parameters" },
  { id: "versions", label: "Versions" },
  { id: "backtests", label: "Backtests" }
];
export function StrategyWorkspace() {
  const { api } = useClients();
  const controller = useRef(new StrategyMutationController());
  const [system, setSystem] = useState<StrategyDescriptor[]>([]),
    [owned, setOwned] = useState<UserStrategySummary[]>([]),
    [selectedSystem, setSelectedSystem] = useState<StrategyDescriptor>(),
    [selectedOwned, setSelectedOwned] = useState<UserStrategy>(),
    [versions, setVersions] = useState<UserStrategyVersion[]>([]);
  const [systemLoading, setSystemLoading] = useState(true),
    [ownedLoading, setOwnedLoading] = useState(true),
    [systemError, setSystemError] = useState<string>(),
    [ownedError, setOwnedError] = useState<string>(),
    [pending, setPending] = useState(false),
    [feedback, setFeedback] = useState<string>(),
    [editingVersion, setEditingVersion] = useState(false),
    [versionsLoading, setVersionsLoading] = useState(false),
    [activeTab, setActiveTab] = useState<WorkspaceTab>("overview");
  const loadSystem = useCallback(async () => {
    const result = await listSystemStrategies(api);
    if (result.ok) setSystem(result.data.items);
    else
      setSystemError(
        result.error.retryable
          ? "The system strategy catalog is temporarily unavailable. Please try again."
          : "Unable to load the system strategy catalog."
      );
    setSystemLoading(false);
  }, [api]);
  const loadOwned = useCallback(async () => {
    const result = await listUserStrategies(api);
    if (result.ok) setOwned(result.data.items);
    else
      setOwnedError(
        result.error.retryable
          ? "Your strategies are temporarily unavailable. Please try again."
          : "Unable to load your strategies."
      );
    setOwnedLoading(false);
  }, [api]);
  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadSystem();
      void loadOwned();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadSystem, loadOwned]);
  const selectOwned = async (id: string) => {
    setVersionsLoading(true);
    setFeedback(undefined);
    try {
      const result = await getUserStrategy(api, id);
      if (!result.ok) {
        setFeedback("Unable to access this strategy. Please try again.");
        return;
      }
      setSelectedOwned(result.data);
      setSelectedSystem(undefined);
      setEditingVersion(false);
      setActiveTab("overview");
      const history = await listUserStrategyVersions(api, id);
      if (history.ok) setVersions(history.data.items);
      else {
        setVersions([]);
        setFeedback("The strategy opened, but its version history could not be loaded.");
      }
    } catch {
      setVersions([]);
      setFeedback("Unable to connect to the backend. Please try again.");
    } finally {
      setVersionsLoading(false);
    }
  };
  const mutate = async (operation: () => Promise<MutationOutcome>) => {
    setPending(true);
    setFeedback(undefined);
    try {
      const result = await controller.current.run(operation, async () => {
        await loadOwned();
        if (selectedOwned) await selectOwned(selectedOwned.userStrategyId);
      });
      if (result && !result.ok) throw new Error(result.error?.code ?? "MUTATION_FAILED");
      setFeedback("The latest state has been synchronized.");
      return Boolean(result);
    } catch {
      setFeedback("The outcome is unknown. The authoritative state has been reloaded.");
      return false;
    } finally {
      setPending(false);
    }
  };
  const create = async (draft: StrategyDraft) => {
    await mutate(async () => {
      const result = await createUserStrategy(api, draft);
      if (result.ok) {
        setSelectedOwned(result.data);
        setSelectedSystem(undefined);
        setVersions([result.data.latestVersion]);
        setActiveTab("overview");
      }
      return result;
    });
  };
  return (
    <main className="strategy-workspace">
      <AsyncStatus message={pending ? "Updating strategy" : feedback} />
      <header className="strategy-hero">
        <h1>Strategy Composer</h1>
      </header>
      {feedback && (
        <p className="strategy-feedback" role="status">
          {feedback}
        </p>
      )}
      <div className="strategy-layout">
        <StrategyCatalog
          system={system}
          owned={owned}
          loadingSystem={systemLoading}
          loadingOwned={ownedLoading}
          systemError={systemError}
          ownedError={ownedError}
          selectedSystemId={selectedSystem?.strategyVersionId}
          selectedOwnedId={selectedOwned?.userStrategyId}
          onSelectSystem={(item) => {
            setSelectedSystem(item);
            setSelectedOwned(undefined);
            setVersions([]);
            setEditingVersion(false);
            setActiveTab("overview");
          }}
          onSelectOwned={(id) => void selectOwned(id)}
        />
        <section className="strategy-workbench" aria-label="Selected strategy workspace">
          <header className="strategy-workbench-header">
            <div>
              <span className="strategy-section-kicker">Workspace</span>
              <h2>{selectedOwned?.name ?? selectedSystem?.displayName ?? "Choose a strategy"}</h2>
            </div>
            <span className="strategy-context-badge">
              {selectedOwned ? "Personal strategy" : selectedSystem ? "System template" : "Ready"}
            </span>
          </header>
          <nav className="strategy-tabs" role="tablist" aria-label="Strategy workspace views">
            {workspaceTabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={activeTab === tab.id}
                aria-controls={`strategy-tab-${tab.id}`}
                disabled={!selectedOwned && !selectedSystem}
                onClick={() => {
                  setEditingVersion(false);
                  setActiveTab(tab.id);
                }}
              >
                {tab.label}
                {tab.id === "versions" && selectedOwned ? <span>{versions.length}</span> : null}
              </button>
            ))}
          </nav>
          {!selectedOwned && !selectedSystem ? <StrategyDetail systemStrategies={system} /> : null}
          {activeTab === "overview" && (selectedOwned || selectedSystem) ? (
            <div
              className="strategy-center strategy-tab-content"
              id="strategy-tab-overview"
              role="tabpanel"
              aria-label="Overview"
            >
              <StrategyDetail
                descriptor={selectedSystem}
                owned={selectedOwned}
                systemStrategies={system}
              />
              {selectedOwned ? (
                <StrategyActions
                  pending={pending}
                  archived={selectedOwned.status === "ARCHIVED"}
                  canPublish={selectedOwned.latestVersion.status === "DRAFT"}
                  backtestVersionId={
                    selectedOwned.latestVersion.status === "PUBLISHED"
                      ? selectedOwned.latestVersion.userStrategyVersionId
                      : undefined
                  }
                  onPublish={() =>
                    void mutate(() =>
                      publishUserStrategyVersion(
                        api,
                        selectedOwned.userStrategyId,
                        selectedOwned.latestVersion.userStrategyVersionId,
                        selectedOwned.latestVersion.versionNo
                      )
                    )
                  }
                  onArchive={() =>
                    void mutate(() => archiveUserStrategy(api, selectedOwned.userStrategyId))
                  }
                  onNewVersion={() => {
                    setEditingVersion(true);
                    setActiveTab("parameters");
                  }}
                />
              ) : (
                <button
                  className="button strategy-primary-action"
                  type="button"
                  onClick={() => setActiveTab("parameters")}
                >
                  Configure this strategy
                </button>
              )}
            </div>
          ) : null}
          {activeTab === "parameters" && editingVersion && selectedOwned ? (
            <div
              className="strategy-tab-content"
              id="strategy-tab-parameters"
              role="tabpanel"
              aria-label="Parameters"
            >
              <StrategyVersionForm
                key={`${selectedOwned.userStrategyId}:${selectedOwned.latestVersion.versionNo}`}
                owned={selectedOwned}
                systemStrategies={system}
                pending={pending}
                onCancel={() => {
                  setEditingVersion(false);
                  setActiveTab("overview");
                }}
                onSubmit={async (source) => {
                  const succeeded = await mutate(() =>
                    createUserStrategyVersion(
                      api,
                      selectedOwned.userStrategyId,
                      selectedOwned.latestVersion.versionNo,
                      source
                    )
                  );
                  if (succeeded) setEditingVersion(false);
                }}
              />
            </div>
          ) : activeTab === "parameters" && (selectedOwned || selectedSystem) ? (
            <div
              className="strategy-tab-content"
              id="strategy-tab-parameters"
              role="tabpanel"
              aria-label="Parameters"
            >
              {selectedOwned ? (
                <StrategyParameters owned={selectedOwned} systemStrategies={system} />
              ) : (
                <>
                  <StrategyParameters descriptor={selectedSystem} systemStrategies={system} />
                  <StrategyForm
                    descriptor={selectedSystem}
                    systemStrategies={system}
                    pending={pending}
                    onSubmit={create}
                  />
                </>
              )}
            </div>
          ) : null}
          {activeTab === "versions" && (selectedOwned || selectedSystem) ? (
            <div
              className="strategy-tab-content"
              id="strategy-tab-versions"
              role="tabpanel"
              aria-label="Versions"
            >
              {selectedOwned ? (
                <StrategyVersionHistory versions={versions} loading={versionsLoading} />
              ) : (
                <section className="strategy-tab-message">
                  <span className="strategy-section-kicker">System release</span>
                  <h2>Version {selectedSystem!.version}</h2>
                  <p>
                    System strategies are released with the application. Create a personal strategy
                    to manage your own immutable version history.
                  </p>
                  <code>{selectedSystem!.descriptorFingerprint}</code>
                </section>
              )}
            </div>
          ) : null}
          {activeTab === "backtests" && (selectedOwned || selectedSystem) ? (
            <section
              className="strategy-tab-message strategy-backtest-entry strategy-tab-content"
              id="strategy-tab-backtests"
              role="tabpanel"
              aria-label="Backtests"
            >
              <span className="strategy-section-kicker">Experiment entry point</span>
              <h2>Test this strategy on historical market data</h2>
              <p>
                Search creates candidates and sends them through Backtest, Evaluation and
                Leaderboard using the selected strategy version.
              </p>
              {selectedOwned && selectedOwned.latestVersion.status !== "PUBLISHED" ? (
                <p role="note">Publish the latest version before using it in an experiment.</p>
              ) : (
                <Link
                  className="button strategy-primary-action"
                  href={
                    selectedOwned
                      ? `/search?userStrategyVersionId=${encodeURIComponent(selectedOwned.latestVersion.userStrategyVersionId)}`
                      : "/search"
                  }
                >
                  Open Search &amp; Backtest
                </Link>
              )}
              <Link className="strategy-history-link" href="/backtests">
                View completed backtest results →
              </Link>
            </section>
          ) : null}
        </section>
      </div>
    </main>
  );
}
