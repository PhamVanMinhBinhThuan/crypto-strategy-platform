"use client";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import {
  getUserStrategy,
  listSystemStrategies,
  listUserStrategies
} from "../../strategy/api/strategy-api";
import type { StrategyDescriptor, UserStrategy } from "../../strategy/model/strategy";
import { useExperimentConfiguration } from "../hooks/useExperimentConfiguration";
import { useExperimentCommands } from "../hooks/useExperimentCommands";
import type { SearchParameterDomain } from "../types/experiment-configuration";
import { DependencyGateNotice } from "./DependencyGateNotice";

const supportedForSearch = (strategy: StrategyDescriptor) =>
  strategy.parameters.every(
    (parameter) =>
      parameter.type === "INTEGER" ||
      ((parameter.type === "ENUM" || parameter.type === "TEXT") &&
        parameter.allowedValues.length > 0)
  );

const searchDomains = (strategy: StrategyDescriptor): Record<string, SearchParameterDomain> =>
  Object.fromEntries(
    strategy.parameters.map((parameter) => [
      parameter.name,
      parameter.type === "INTEGER"
        ? {
            kind: "RANGE" as const,
            minimum: parameter.minimum ?? parameter.defaultValue ?? "0",
            maximum: parameter.maximum ?? parameter.defaultValue ?? "0"
          }
        : { kind: "OPTIONS" as const, options: parameter.allowedValues }
    ])
  );

type DatasetResponse = Readonly<{ datasetId: string; membershipCount: number }>;

export function ExperimentConfigurationForm({
  api,
  fixture
}: {
  api: ApiClient;
  fixture: boolean;
}) {
  const { draft, errors, update, updateParameter, selectStrategy, validate } =
    useExperimentConfiguration();
  const commands = useExperimentCommands(api);
  const [systemStrategies, setSystemStrategies] = useState<StrategyDescriptor[]>([]);
  const [publishedStrategies, setPublishedStrategies] = useState<UserStrategy[]>([]);
  const [catalogState, setCatalogState] = useState<"loading" | "ready" | "error">("loading");
  const [datasetState, setDatasetState] = useState<
    | { status: "idle" | "creating" }
    | { status: "ready"; membershipCount: number }
    | { status: "error" }
  >({ status: "idle" });

  useEffect(() => {
    let active = true;
    const load = async () => {
      const [systemResult, ownedResult] = await Promise.all([
        listSystemStrategies(api),
        listUserStrategies(api)
      ]);
      if (!active) return;
      if (!systemResult.ok || !ownedResult.ok) {
        setCatalogState("error");
        return;
      }
      const details = await Promise.all(
        ownedResult.data.items.map((item) => getUserStrategy(api, item.userStrategyId))
      );
      if (!active) return;
      const published = details.flatMap((result) =>
        result.ok &&
        result.data.status === "ACTIVE" &&
        result.data.latestVersion.status === "PUBLISHED"
          ? [result.data]
          : []
      );
      setSystemStrategies(systemResult.data.items);
      setPublishedStrategies(published);
      setCatalogState("ready");
      const first = systemResult.data.items.find(supportedForSearch);
      if (first)
        selectStrategy({
          strategyId: first.strategyId,
          strategyVersion: first.version,
          userStrategyVersionId: undefined,
          parameters: searchDomains(first)
        });
    };
    void load();
    return () => {
      active = false;
    };
  }, [api, selectStrategy]);

  const selectedStrategy = useMemo(
    () =>
      draft.userStrategyVersionId
        ? `user:${draft.userStrategyVersionId}`
        : draft.strategyId
          ? `system:${draft.strategyId}:${draft.strategyVersion}`
          : "",
    [draft.strategyId, draft.strategyVersion, draft.userStrategyVersionId]
  );

  const chooseStrategy = (value: string) => {
    if (value.startsWith("user:")) {
      const versionId = value.slice("user:".length);
      selectStrategy({
        strategyId: "",
        strategyVersion: "",
        userStrategyVersionId: versionId,
        parameters: {}
      });
      return;
    }
    const strategy = systemStrategies.find(
      (candidate) => `system:${candidate.strategyId}:${candidate.version}` === value
    );
    if (strategy)
      selectStrategy({
        strategyId: strategy.strategyId,
        strategyVersion: strategy.version,
        userStrategyVersionId: undefined,
        parameters: searchDomains(strategy)
      });
  };

  const createDataset = async () => {
    setDatasetState({ status: "creating" });
    const end = new Date();
    end.setUTCMinutes(0, 0, 0);
    const start = new Date(end.getTime() - 30 * 24 * 60 * 60 * 1000);
    const result = await api.request<DatasetResponse>("/api/v1/datasets", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": globalThis.crypto.randomUUID()
      },
      body: JSON.stringify({
        pair: "BTC/USDT",
        timeframe: "1h",
        startTime: start.toISOString(),
        endTime: end.toISOString()
      })
    });
    if (result.ok) {
      update("datasetId", result.data.datasetId);
      setDatasetState({ status: "ready", membershipCount: result.data.membershipCount });
    } else setDatasetState({ status: "error" });
  };
  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) void commands.startExperiment(draft);
  };
  return (
    <section className="panel config-panel">
      <div className="section-heading">
        <h2>Configure Experiment</h2>
        {fixture && <span className="fixture-badge">FIXTURE DATA</span>}
      </div>
      {fixture && <DependencyGateNotice />}
      <form onSubmit={submit} noValidate>
        <div className="form-grid">
          <label>
            Name
            <input
              aria-label="Name"
              value={draft.name}
              onChange={(e) => update("name", e.target.value)}
              aria-invalid={!!errors.name}
            />
            {errors.name && <small role="alert">{errors.name}</small>}
          </label>
          <label>
            Dataset ID <small>Immutable candle dataset</small>
            <input
              aria-label="Dataset ID"
              value={draft.datasetId}
              onChange={(e) => update("datasetId", e.target.value)}
              aria-invalid={!!errors.datasetId}
            />
            {errors.datasetId && <small role="alert">{errors.datasetId}</small>}
          </label>
          <div>
            <button
              type="button"
              className="button secondary"
              disabled={datasetState.status === "creating"}
              onClick={() => void createDataset()}
            >
              Create dataset
            </button>
            {datasetState.status === "ready" && (
              <small role="status">
                Dataset ready with {datasetState.membershipCount} candles.
              </small>
            )}
            {datasetState.status === "error" && (
              <small role="alert">Dataset creation failed. Check Market Data and retry.</small>
            )}
          </div>
          <label>
            Generator <small>{fixture ? "Fixture profile" : "Live API"}</small>
            <select
              aria-label="Generator"
              value={draft.generatorId}
              onChange={(e) => update("generatorId", e.target.value)}
            >
              <option value="random-search">Random Search</option>
            </select>
          </label>
          <label>
            Generator version
            <input aria-label="Generator version" value={draft.generatorVersion} readOnly />
          </label>
          <label>
            Seed
            <input
              aria-label="Seed"
              inputMode="numeric"
              value={draft.seed}
              onChange={(e) => update("seed", e.target.value)}
            />
          </label>
          <label>
            Strategy
            <select
              aria-label="Strategy"
              value={selectedStrategy}
              disabled={catalogState === "loading"}
              onChange={(e) => chooseStrategy(e.target.value)}
            >
              {!selectedStrategy && <option value="">Select a Strategy</option>}
              <optgroup label="System Strategies">
                {systemStrategies.map((strategy) => (
                  <option
                    key={strategy.strategyVersionId}
                    value={`system:${strategy.strategyId}:${strategy.version}`}
                    disabled={!supportedForSearch(strategy)}
                  >
                    {strategy.displayName}
                    {!supportedForSearch(strategy) ? " (not searchable in MVP)" : ""}
                  </option>
                ))}
              </optgroup>
              {publishedStrategies.length > 0 && (
                <optgroup label="Published personal Strategies">
                  {publishedStrategies.map((strategy) => (
                    <option
                      key={strategy.latestVersion.userStrategyVersionId}
                      value={`user:${strategy.latestVersion.userStrategyVersionId}`}
                    >
                      {strategy.name}
                    </option>
                  ))}
                </optgroup>
              )}
            </select>
            {catalogState === "error" && (
              <small role="alert">Strategy catalog is unavailable.</small>
            )}
            {errors.strategyId && <small role="alert">{errors.strategyId}</small>}
          </label>
          <label>
            Strategy version
            <input
              aria-label="Strategy version"
              value={draft.userStrategyVersionId ? "Published snapshot" : draft.strategyVersion}
              readOnly
            />
          </label>
          {Object.entries(draft.parameters).map(([name, domain]) => (
            <fieldset key={name}>
              <legend>
                {name} {domain.kind === "RANGE" ? "range" : "options"}
              </legend>
              {domain.kind === "RANGE" ? (
                <>
                  <label>
                    Minimum
                    <input
                      aria-label={`${name} minimum`}
                      inputMode="numeric"
                      value={domain.minimum}
                      onChange={(e) =>
                        updateParameter(name, { ...domain, minimum: e.target.value })
                      }
                    />
                  </label>
                  <label>
                    Maximum
                    <input
                      aria-label={`${name} maximum`}
                      inputMode="numeric"
                      value={domain.maximum}
                      onChange={(e) =>
                        updateParameter(name, { ...domain, maximum: e.target.value })
                      }
                    />
                  </label>
                </>
              ) : (
                <label>
                  Options (comma separated)
                  <input
                    aria-label={`${name} options`}
                    value={domain.options.join(", ")}
                    onChange={(e) =>
                      updateParameter(name, {
                        kind: "OPTIONS",
                        options: e.target.value
                          .split(",")
                          .map((option) => option.trim())
                          .filter(Boolean)
                      })
                    }
                  />
                </label>
              )}
              {errors[`parameter-${name}`] && (
                <small role="alert">{errors[`parameter-${name}`]}</small>
              )}
            </fieldset>
          ))}
          <label>
            Maximum candidates
            <input
              aria-label="Maximum candidates"
              type="number"
              min="1"
              value={draft.maximumCandidates}
              onChange={(e) => update("maximumCandidates", e.target.value)}
            />
          </label>
          <label>
            Maximum duration (seconds)
            <input
              aria-label="Maximum duration (seconds)"
              type="number"
              min="1"
              value={draft.maximumDurationSeconds}
              onChange={(e) => update("maximumDurationSeconds", e.target.value)}
            />
          </label>
          <label>
            Top-K
            <select
              aria-label="Top-K"
              value={draft.topK}
              onChange={(e) => update("topK", Number(e.target.value))}
            >
              {[10, 25, 50].map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
            {errors.topK && <small role="alert">{errors.topK}</small>}
          </label>
        </div>
        {errors.stop && <p role="alert">{errors.stop}</p>}
        <button className="button primary" disabled={commands.start.status === "submitting"}>
          Start Experiment
        </button>
        {commands.start.status === "accepted" && (
          <p role="status">
            Experiment accepted with status {commands.start.acceptedStatus}.{" "}
            <Link href={`/search?id=${encodeURIComponent(commands.start.experimentId)}`}>
              Open Experiment {commands.start.experimentId}
            </Link>
          </p>
        )}
        {commands.start.status === "dependency-unavailable" && (
          <p role="alert">The Search service is temporarily unavailable. Try again later.</p>
        )}
      </form>
    </section>
  );
}
