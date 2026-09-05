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
import { StrategyVersionForm } from "./StrategyVersionForm";
import { AsyncStatus } from "../../shared/AsyncStatus";

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
    [feedback, setFeedback] = useState<string>(),
    [editingVersion, setEditingVersion] = useState(false);
  const loadSystem = useCallback(async () => {
    const result = await listSystemStrategies(api);
    if (result.ok) setSystem(result.data.items);
    else
      setSystemError(
        result.error.retryable
          ? "Danh mục Strategy hệ thống đang tạm gián đoạn. Vui lòng thử lại."
          : "Không thể tải danh mục Strategy hệ thống."
      );
    setSystemLoading(false);
  }, [api]);
  const loadOwned = useCallback(async () => {
    const result = await listUserStrategies(api);
    if (result.ok) setOwned(result.data.items);
    else
      setOwnedError(
        result.error.retryable
          ? "Strategy cá nhân đang tạm gián đoạn. Vui lòng thử lại."
          : "Không thể tải Strategy cá nhân."
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
    const result = await getUserStrategy(api, id);
    if (result.ok) {
      setSelectedOwned(result.data);
      setSelectedSystem(undefined);
      setEditingVersion(false);
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
      return Boolean(result);
    } catch {
      setFeedback("Chưa xác định kết quả. Đã tải lại trạng thái authoritative.");
      return false;
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
      <AsyncStatus message={pending ? "Đang cập nhật Strategy" : feedback} />
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
            setEditingVersion(false);
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
              onNewVersion={() => setEditingVersion(true)}
            />
          )}
        </div>
        {editingVersion && selectedOwned ? (
          <StrategyVersionForm
            key={`${selectedOwned.userStrategyId}:${selectedOwned.latestVersion.versionNo}`}
            owned={selectedOwned}
            systemStrategies={system}
            pending={pending}
            onCancel={() => setEditingVersion(false)}
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
        ) : (
          <StrategyForm
            descriptor={selectedSystem}
            systemStrategies={system}
            pending={pending}
            onSubmit={create}
          />
        )}
      </div>
    </main>
  );
}
