"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { useClients } from "@/src/foundation/composition/client-provider";
import type { StrategyDescriptor, UserStrategy, UserStrategySummary } from "../model/strategy";
import type { StrategyDraft } from "../model/strategy-draft";
import {
  archiveUserStrategy,
  createUserStrategyVersion,
  createUserStrategy,
  getUserStrategy,
  listSystemStrategies,
  listUserStrategies,
  publishUserStrategyVersion
} from "../api/strategy-api";
import { StrategyMutationController } from "../state/strategy-controller";
import { StrategyCatalog } from "./StrategyCatalog";
import { StrategyDetail } from "./StrategyDetail";
import { StrategyForm } from "./StrategyForm";
import { StrategyActions } from "./StrategyActions";
type MutationOutcome = { ok: boolean; error?: { code: string } };
export function StrategyWorkspace() {
  const { api } = useClients();
  const controller = useRef(new StrategyMutationController());
  const [system, setSystem] = useState<StrategyDescriptor[]>([]),
    [owned, setOwned] = useState<UserStrategySummary[]>([]),
    [selectedSystem, setSelectedSystem] = useState<StrategyDescriptor>(),
    [selectedOwned, setSelectedOwned] = useState<UserStrategy>();
  const [systemLoading, setSystemLoading] = useState(true),
    [ownedLoading, setOwnedLoading] = useState(true),
    [systemError, setSystemError] = useState<string>(),
    [ownedError, setOwnedError] = useState<string>(),
    [pending, setPending] = useState(false),
    [feedback, setFeedback] = useState<string>();
  const loadSystem = useCallback(async () => {
    const result = await listSystemStrategies(api);
    if (result.ok) setSystem(result.data.items);
    else setSystemError(result.error.message);
    setSystemLoading(false);
  }, [api]);
  const loadOwned = useCallback(async () => {
    const result = await listUserStrategies(api);
    if (result.ok) setOwned(result.data.items);
    else setOwnedError(result.error.message);
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
    const result = await getUserStrategy(api, id);
    if (result.ok) {
      setSelectedOwned(result.data);
      setSelectedSystem(undefined);
    } else setFeedback("Không thể truy cập Strategy này.");
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
      setFeedback("Đã đồng bộ trạng thái mới nhất.");
    } catch {
      setFeedback("Chưa xác định kết quả. Đã tải lại trạng thái authoritative.");
    } finally {
      setPending(false);
    }
  };
  const create = async (draft: StrategyDraft) => {
    await mutate(async () => {
      const result = await createUserStrategy(api, draft);
      if (result.ok) setSelectedOwned(result.data);
      return result;
    });
  };
  return (
    <main className="strategy-workspace">
      <header>
        <p className="eyebrow">F-012 · Strategy</p>
        <h1>Strategy Composer</h1>
        <p>Khám phá catalog và quản lý các version Strategy riêng.</p>
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
          onSelectSystem={(item) => {
            setSelectedSystem(item);
            setSelectedOwned(undefined);
          }}
          onSelectOwned={(id) => void selectOwned(id)}
        />
        <div className="strategy-center">
          <StrategyDetail descriptor={selectedSystem} owned={selectedOwned} />
          {selectedOwned && (
            <StrategyActions
              pending={pending}
              archived={selectedOwned.status === "ARCHIVED"}
              canPublish={selectedOwned.latestVersion.status === "DRAFT"}
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
              onNewVersion={() =>
                void mutate(() =>
                  createUserStrategyVersion(
                    api,
                    selectedOwned.userStrategyId,
                    selectedOwned.latestVersion.versionNo,
                    selectedOwned.latestVersion.source.type === "SINGLE"
                      ? {
                          type: "SINGLE",
                          strategy: {
                            strategyId: selectedOwned.latestVersion.source.strategy.strategyId,
                            version: selectedOwned.latestVersion.source.strategy.version,
                            parameters: selectedOwned.latestVersion.source.strategy.parameters
                          }
                        }
                      : {
                          type: "COMPOSITE",
                          policyId: selectedOwned.latestVersion.source.policyId,
                          policyVersion: selectedOwned.latestVersion.source.policyVersion,
                          policyParameters: selectedOwned.latestVersion.source.policyParameters,
                          components: selectedOwned.latestVersion.source.components.map((item) => ({
                            strategyId: item.strategyId,
                            version: item.version,
                            parameters: item.parameters
                          }))
                        }
                  )
                )
              }
            />
          )}
        </div>
        <StrategyForm
          descriptor={selectedSystem}
          systemStrategies={system}
          pending={pending}
          onSubmit={create}
        />
      </div>
    </main>
  );
}
