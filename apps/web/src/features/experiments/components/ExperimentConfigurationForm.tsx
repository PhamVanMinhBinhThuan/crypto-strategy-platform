"use client";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import type { ApiClient, PublicError } from "@/src/foundation/http/contracts";
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
      parameter.type === "DECIMAL" ||
      ((parameter.type === "ENUM" || parameter.type === "TEXT") &&
        parameter.allowedValues.length > 0)
  );

const searchDomains = (strategy: StrategyDescriptor): Record<string, SearchParameterDomain> =>
  Object.fromEntries(
    strategy.parameters.map((parameter) => [
      parameter.name,
      parameter.type === "INTEGER" || parameter.type === "DECIMAL"
        ? {
            kind: "RANGE" as const,
            valueType: parameter.type,
            minimum: parameter.minimum ?? parameter.defaultValue ?? "0",
            maximum: parameter.maximum ?? parameter.defaultValue ?? "0",
            step: parameter.type === "DECIMAL" ? "0.1" : "1"
          }
        : { kind: "OPTIONS" as const, options: parameter.allowedValues }
    ])
  );

type DatasetResponse = Readonly<{
  datasetId: string;
  provider: string;
  pair: string;
  timeframe: string;
  startTime: string;
  endTime: string;
  membershipCount: number;
  checksum: string;
  status: string;
  createdAt: string;
}>;
type DatasetListResponse = Readonly<{ items: DatasetResponse[] }>;
type GeneratorResponse = Readonly<{
  generatorId: string;
  version: string;
  displayName: string;
}>;
type GeneratorListResponse = Readonly<{ items: GeneratorResponse[] }>;

const cardinality = (
  pool: ReadonlyArray<{
    parameters: Record<string, SearchParameterDomain>;
    constraints?: ReadonlyArray<{ lowerParameter: string; upperParameter: string }>;
  }>,
  minimum: number,
  maximum: number
) => {
  const options = (domain: SearchParameterDomain) =>
    domain.kind === "OPTIONS"
      ? domain.options
      : Array.from(
          {
            length: Math.max(
              0,
              Math.floor(
                (Number(domain.maximum) - Number(domain.minimum)) / Number(domain.step ?? "1")
              ) + 1
            )
          },
          (_, index) => String(Number(domain.minimum) + index * Number(domain.step ?? "1"))
        );
  const sizes = pool.map((entry) => {
    const base = Object.values(entry.parameters).reduce(
      (total, domain) => total * BigInt(options(domain).length),
      1n
    );
    const constraint = entry.constraints?.[0];
    if (!constraint) return base;
    const lower = options(entry.parameters[constraint.lowerParameter]);
    const upper = options(entry.parameters[constraint.upperParameter]);
    const validPairs = lower.reduce(
      (count, left) => count + upper.filter((right) => Number(left) < Number(right)).length,
      0
    );
    return (base / BigInt(lower.length || 1) / BigInt(upper.length || 1)) * BigInt(validPairs);
  });
  const count = (start: number, remaining: number, product: bigint): bigint => {
    if (remaining === 0) return product;
    let total = 0n;
    for (let index = start; index <= sizes.length - remaining; index += 1)
      total += count(index + 1, remaining - 1, product * sizes[index]);
    return total;
  };
  let total = 0n;
  for (let size = minimum; size <= Math.min(maximum, sizes.length); size += 1)
    total += count(0, size, 1n);
  return total;
};

const datasetMatchesDraft = (
  dataset: DatasetResponse,
  pair: string,
  timeframe: string,
  startUtc: string,
  endUtc: string
) => {
  const selectedStart = Date.parse(`${startUtc}Z`);
  const selectedEnd = Date.parse(`${endUtc}Z`);
  return (
    Number.isFinite(selectedStart) &&
    Number.isFinite(selectedEnd) &&
    dataset.pair.toUpperCase() === pair.trim().toUpperCase() &&
    dataset.timeframe === timeframe &&
    Date.parse(dataset.startTime) === selectedStart &&
    Date.parse(dataset.endTime) === selectedEnd &&
    dataset.status === "READY"
  );
};

const serverField = (field: string) => {
  const direct: Record<string, string> = {
    name: "name",
    datasetId: "datasetId",
    "backtestConfiguration.initialCapital": "initialCapital",
    "backtestConfiguration.feeRate": "feePercent",
    "backtestConfiguration.slippageRate": "slippagePercent",
    "generator.generatorId": "generatorId",
    "generator.seed": "seed",
    "searchSpace.strategyPool": "strategyPool",
    "searchSpace.minComponents": "componentBounds",
    "searchSpace.maxComponents": "componentBounds",
    requestedConcurrency: "requestedConcurrency",
    "stopConditions.maximumWithoutImprovement": "maximumWithoutImprovement",
    topK: "topK"
  };
  return direct[field] ?? (field.startsWith("searchSpace.strategyPool") ? "strategyPool" : null);
};

const datasetErrorMessage = (error: PublicError) => {
  if (error.code === "DATABASE_SCHEMA_UNAVAILABLE")
    return "Database schema is missing. Apply the pending F-015 migration, restart the API, and retry.";
  if (error.code === "DATABASE_UNAVAILABLE")
    return "Database is unavailable. Check the server database connection and retry.";
  if (error.code === "MARKET_DATA_GAP")
    return "No complete candles were returned for this range. Choose an older aligned range and retry.";
  if (error.code === "MARKET_PROVIDER_UNAVAILABLE")
    return "Binance is unavailable. Check the API network connection and retry.";
  return error.message;
};

export function ExperimentConfigurationForm({
  api,
  fixture
}: {
  api: ApiClient;
  fixture: boolean;
}) {
  const router = useRouter();
  const {
    draft,
    errors,
    update,
    updatePoolParameter,
    selectStrategy,
    validate,
    applyServerErrors
  } = useExperimentConfiguration();
  const commands = useExperimentCommands(api);
  const [systemStrategies, setSystemStrategies] = useState<StrategyDescriptor[]>([]);
  const [publishedStrategies, setPublishedStrategies] = useState<UserStrategy[]>([]);
  const [catalogState, setCatalogState] = useState<"loading" | "ready" | "error">("loading");
  const [datasetState, setDatasetState] = useState<
    | { status: "idle" | "creating" }
    | { status: "ready"; membershipCount: number }
    | { status: "error"; message: string; correlationId?: string }
  >({ status: "idle" });
  const [datasetCatalogState, setDatasetCatalogState] = useState<
    "loading" | "ready" | "error"
  >("loading");
  const [datasets, setDatasets] = useState<DatasetResponse[]>([]);
  const [generators, setGenerators] = useState<GeneratorResponse[]>([]);

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
          displayName: first.displayName,
          userStrategyVersionId: undefined,
          parameters: searchDomains(first),
          constraints: first.constraints
        });
    };
    void load();
    return () => {
      active = false;
    };
  }, [api, selectStrategy]);

  useEffect(() => {
    let active = true;
    void api.request<DatasetListResponse>("/api/v1/datasets?limit=50").then((result) => {
      if (!active) return;
      if (result.ok) {
        setDatasets(result.data.items);
        setDatasetCatalogState("ready");
      } else setDatasetCatalogState("error");
    });
    return () => {
      active = false;
    };
  }, [api]);

  useEffect(() => {
    let active = true;
    void api.request<GeneratorListResponse>("/api/v1/search/generators").then((result) => {
      if (!active || !result.ok) return;
      setGenerators(result.data.items);
      const first = result.data.items[0];
      if (first) {
        update("generatorId", first.generatorId);
        update("generatorVersion", first.version);
      }
    });
    return () => {
      active = false;
    };
  }, [api, update]);

  useEffect(() => {
    if (commands.start.status === "accepted") {
      router.push(`/search/${encodeURIComponent(commands.start.experimentId)}`);
    }
  }, [commands.start, router]);

  const toggleSystemStrategy = (strategy: StrategyDescriptor) => {
    const key = `system:${strategy.strategyId}:${strategy.version}`;
    const selected = draft.strategyPool.some((entry) => entry.key === key);
    const strategyPool = selected
      ? draft.strategyPool.filter((entry) => entry.key !== key)
      : [
          ...draft.strategyPool,
          {
            key,
            displayName: strategy.displayName,
            strategyId: strategy.strategyId,
            strategyVersion: strategy.version,
            parameters: searchDomains(strategy),
            constraints: strategy.constraints
          }
        ];
    update("strategyPool", strategyPool);
    update(
      "maximumComponents",
      Math.min(Math.max(1, draft.maximumComponents), strategyPool.length || 1)
    );
  };

  const toggleUserStrategy = (strategy: UserStrategy) => {
    const versionId = strategy.latestVersion.userStrategyVersionId;
    const key = `user:${versionId}`;
    const selected = draft.strategyPool.some((entry) => entry.key === key);
    const strategyPool = selected
      ? draft.strategyPool.filter((entry) => entry.key !== key)
      : [
          ...draft.strategyPool,
          {
            key,
            displayName: strategy.name,
            userStrategyVersionId: versionId,
            parameters: {}
          }
        ];
    update("strategyPool", strategyPool);
    update(
      "maximumComponents",
      Math.min(Math.max(1, draft.maximumComponents), strategyPool.length || 1)
    );
  };

  const searchSpaceCardinality = useMemo(
    () => cardinality(draft.strategyPool, draft.minimumComponents, draft.maximumComponents),
    [draft.strategyPool, draft.minimumComponents, draft.maximumComponents]
  );
  const compatibleDatasets = useMemo(
    () =>
      datasets.filter((dataset) =>
        datasetMatchesDraft(
          dataset,
          draft.pair,
          draft.timeframe,
          draft.startUtc,
          draft.endUtc
        )
      ),
    [datasets, draft.pair, draft.timeframe, draft.startUtc, draft.endUtc]
  );
  const selectedDataset = useMemo(
    () => datasets.find((dataset) => dataset.datasetId === draft.datasetId),
    [datasets, draft.datasetId]
  );
  const quoteAsset = draft.pair.split("/")[1]?.trim() || "quote asset";

  useEffect(() => {
    if (
      selectedDataset &&
      !datasetMatchesDraft(
        selectedDataset,
        draft.pair,
        draft.timeframe,
        draft.startUtc,
        draft.endUtc
      )
    ) {
      update("datasetId", "");
      setDatasetState({ status: "idle" });
    }
  }, [draft.pair, draft.timeframe, draft.startUtc, draft.endUtc, selectedDataset, update]);

  useEffect(() => {
    if (
      commands.start.status !== "terminal-failure" &&
      commands.start.status !== "retryable-failure"
    )
      return;
    const mapped = Object.fromEntries(
      (commands.start.error.fieldErrors ?? []).flatMap(({ field, reason }) => {
        const key = serverField(field);
        return key ? [[key, reason]] : [];
      })
    );
    if (Object.keys(mapped).length > 0) applyServerErrors(mapped);
  }, [commands.start, applyServerErrors]);

  const createDataset = async () => {
    const start = Date.parse(`${draft.startUtc}Z`);
    const end = Date.parse(`${draft.endUtc}Z`);
    if (!draft.pair.trim() || !Number.isFinite(start) || !Number.isFinite(end) || end <= start) {
      setDatasetState({
        status: "error",
        message: "Enter a valid pair and an End UTC later than Start UTC."
      });
      return;
    }
    setDatasetState({ status: "creating" });
    try {
      const result = await api.request<DatasetResponse>("/api/v1/datasets", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Idempotency-Key": globalThis.crypto.randomUUID()
        },
        body: JSON.stringify({
          pair: draft.pair,
          timeframe: draft.timeframe,
          startTime: new Date(`${draft.startUtc}Z`).toISOString(),
          endTime: new Date(`${draft.endUtc}Z`).toISOString()
        })
      });
      if (result.ok) {
        update("datasetId", result.data.datasetId);
        setDatasets((current) => [
          result.data,
          ...current.filter((item) => item.datasetId !== result.data.datasetId)
        ]);
        setDatasetState({ status: "ready", membershipCount: result.data.membershipCount });
        return;
      }
      setDatasetState({
        status: "error",
        message: datasetErrorMessage(result.error),
        correlationId: result.error.correlationId
      });
    } catch {
      setDatasetState({
        status: "error",
        message: "The API connection failed before a response was received. Check the API and retry."
      });
    }
  };
  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const retry =
      commands.start.status === "uncertain" ||
      commands.start.status === "retryable-failure" ||
      commands.start.status === "dependency-unavailable";
    if (validate()) void commands.startExperiment(draft, retry);
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
            Pair
            <input
              aria-label="Pair"
              value={draft.pair}
              onChange={(e) => update("pair", e.target.value.toUpperCase())}
              aria-invalid={!!errors.pair}
            />
            {errors.pair && <small role="alert">{errors.pair}</small>}
          </label>
          <label>
            Timeframe
            <select
              aria-label="Timeframe"
              value={draft.timeframe}
              onChange={(e) => update("timeframe", e.target.value)}
            >
              {["1m", "5m", "15m", "1h", "4h", "1d"].map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label>
            Start UTC
            <input
              aria-label="Start UTC"
              type="datetime-local"
              value={draft.startUtc}
              onChange={(e) => update("startUtc", e.target.value)}
              aria-invalid={!!errors.startUtc}
            />
            {errors.startUtc && <small role="alert">{errors.startUtc}</small>}
          </label>
          <label>
            End UTC <small>Dataset uses [start, end)</small>
            <input
              aria-label="End UTC"
              type="datetime-local"
              value={draft.endUtc}
              onChange={(e) => update("endUtc", e.target.value)}
              aria-invalid={!!errors.endUtc}
            />
            {errors.endUtc && <small role="alert">{errors.endUtc}</small>}
          </label>
          <label>
            Frozen Dataset
            <select
              aria-label="Frozen Dataset"
              value={draft.datasetId}
              onChange={(e) => {
                const dataset = datasets.find((item) => item.datasetId === e.target.value);
                update("datasetId", e.target.value);
                setDatasetState(
                  dataset
                    ? { status: "ready", membershipCount: dataset.membershipCount }
                    : { status: "idle" }
                );
              }}
              aria-invalid={!!errors.datasetId}
              disabled={datasetCatalogState === "loading"}
            >
              <option value="">Create or select a frozen dataset</option>
              {compatibleDatasets.map((dataset) => (
                <option key={dataset.datasetId} value={dataset.datasetId}>
                  {dataset.pair} · {dataset.timeframe} · {dataset.startTime} → {dataset.endTime}
                </option>
              ))}
            </select>
            {errors.datasetId && <small role="alert">{errors.datasetId}</small>}
            <small>
              {datasetCatalogState === "loading"
                ? "Loading frozen datasets…"
                : `${compatibleDatasets.length} compatible READY snapshot(s) for this exact market and [start, end) range.`}
            </small>
            {datasetCatalogState === "error" && (
              <small role="alert">Frozen datasets could not be loaded. You may create one.</small>
            )}
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
                Frozen dataset ready with {datasetState.membershipCount} candles. Candidate
                backtests will reuse this snapshot.
              </small>
            )}
            {datasetState.status === "error" && (
              <small role="alert">
                {datasetState.message}
                {datasetState.correlationId ? ` Reference: ${datasetState.correlationId}` : ""}
              </small>
            )}
          </div>
          {selectedDataset && (
            <section aria-label="Selected dataset provenance" className="panel">
              <h3>Frozen dataset provenance</h3>
              <dl>
                <div>
                  <dt>Provider</dt>
                  <dd>{selectedDataset.provider}</dd>
                </div>
                <div>
                  <dt>Market</dt>
                  <dd>
                    {selectedDataset.pair} · {selectedDataset.timeframe}
                  </dd>
                </div>
                <div>
                  <dt>UTC range</dt>
                  <dd>
                    {selectedDataset.startTime} → {selectedDataset.endTime}
                  </dd>
                </div>
                <div>
                  <dt>Candles / status</dt>
                  <dd>
                    {selectedDataset.membershipCount} / {selectedDataset.status}
                  </dd>
                </div>
                <div>
                  <dt>Checksum</dt>
                  <dd>{selectedDataset.checksum}</dd>
                </div>
                <div>
                  <dt>Created</dt>
                  <dd>{selectedDataset.createdAt}</dd>
                </div>
              </dl>
            </section>
          )}
          <fieldset>
            <legend>Backtest assumptions</legend>
            <p>
              Simulated values shared by every candidate. They do not represent an exchange wallet
              balance.
            </p>
            <label>
              Initial simulated capital <small>{quoteAsset}</small>
              <input
                aria-label="Initial simulated capital"
                inputMode="decimal"
                value={draft.initialCapital}
                onChange={(e) => update("initialCapital", e.target.value)}
                aria-invalid={!!errors.initialCapital}
              />
              {errors.initialCapital && <small role="alert">{errors.initialCapital}</small>}
            </label>
            <label>
              Transaction fee (%)
              <input
                aria-label="Transaction fee (%)"
                inputMode="decimal"
                value={draft.feePercent}
                onChange={(e) => update("feePercent", e.target.value)}
                aria-invalid={!!errors.feePercent}
              />
              {errors.feePercent && <small role="alert">{errors.feePercent}</small>}
            </label>
            <label>
              Slippage (%)
              <input
                aria-label="Slippage (%)"
                inputMode="decimal"
                value={draft.slippagePercent}
                onChange={(e) => update("slippagePercent", e.target.value)}
                aria-invalid={!!errors.slippagePercent}
              />
              {errors.slippagePercent && <small role="alert">{errors.slippagePercent}</small>}
            </label>
          </fieldset>
          <label>
            Generator <small>{fixture ? "Fixture profile" : "Live API"}</small>
            <select
              aria-label="Generator"
              value={draft.generatorId}
              onChange={(e) => {
                const selected = generators.find(
                  (generator) => generator.generatorId === e.target.value
                );
                update("generatorId", e.target.value);
                if (selected) update("generatorVersion", selected.version);
              }}
            >
              {(generators.length > 0
                ? generators
                : [
                    {
                      generatorId: draft.generatorId,
                      version: draft.generatorVersion,
                      displayName: "Loading…"
                    }
                  ]
              ).map((generator) => (
                <option
                  key={`${generator.generatorId}:${generator.version}`}
                  value={generator.generatorId}
                >
                  {generator.displayName}
                </option>
              ))}
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
          <fieldset disabled={catalogState === "loading"}>
            <legend>Strategy pool</legend>
            {systemStrategies.map((strategy) => {
              const key = `system:${strategy.strategyId}:${strategy.version}`;
              return (
                <label key={strategy.strategyVersionId}>
                  <input
                    type="checkbox"
                    aria-label={`Include ${strategy.displayName}`}
                    checked={draft.strategyPool.some((entry) => entry.key === key)}
                    disabled={!supportedForSearch(strategy)}
                    onChange={() => toggleSystemStrategy(strategy)}
                  />
                  {strategy.displayName} ({strategy.version})
                </label>
              );
            })}
            {publishedStrategies.map((strategy) => (
              <label key={strategy.latestVersion.userStrategyVersionId}>
                <input
                  type="checkbox"
                  aria-label={`Include ${strategy.name}`}
                  checked={draft.strategyPool.some(
                    (entry) => entry.key === `user:${strategy.latestVersion.userStrategyVersionId}`
                  )}
                  onChange={() => toggleUserStrategy(strategy)}
                />
                {strategy.name} (published)
              </label>
            ))}
            {catalogState === "error" && (
              <small role="alert">Strategy catalog is unavailable.</small>
            )}
            {errors.strategyPool && <small role="alert">{errors.strategyPool}</small>}
          </fieldset>
          {draft.strategyPool.flatMap((entry) =>
            Object.entries(entry.parameters).map(([name, domain]) => (
              <fieldset key={`${entry.key}:${name}`}>
                <legend>
                  {entry.displayName}: {name} {domain.kind === "RANGE" ? "range" : "options"}
                </legend>
                {domain.kind === "RANGE" ? (
                  <>
                    <label>
                      Minimum
                      <input
                        aria-label={`${entry.strategyId} ${name} minimum`}
                        inputMode={domain.valueType === "DECIMAL" ? "decimal" : "numeric"}
                        value={domain.minimum}
                        onChange={(e) =>
                          updatePoolParameter(entry.key, name, {
                            ...domain,
                            minimum: e.target.value
                          })
                        }
                      />
                    </label>
                    <label>
                      Maximum
                      <input
                        aria-label={`${entry.strategyId} ${name} maximum`}
                        inputMode={domain.valueType === "DECIMAL" ? "decimal" : "numeric"}
                        value={domain.maximum}
                        onChange={(e) =>
                          updatePoolParameter(entry.key, name, {
                            ...domain,
                            maximum: e.target.value
                          })
                        }
                      />
                    </label>
                    <label>
                      Step
                      <input
                        aria-label={`${entry.strategyId} ${name} step`}
                        inputMode={domain.valueType === "DECIMAL" ? "decimal" : "numeric"}
                        value={domain.step ?? (domain.valueType === "DECIMAL" ? "0.1" : "1")}
                        onChange={(e) =>
                          updatePoolParameter(entry.key, name, {
                            ...domain,
                            step: e.target.value
                          })
                        }
                      />
                    </label>
                  </>
                ) : (
                  <label>
                    Options (comma separated)
                    <input
                      aria-label={`${entry.strategyId} ${name} options`}
                      value={domain.options.join(", ")}
                      onChange={(e) =>
                        updatePoolParameter(entry.key, name, {
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
                {errors[`parameter-${entry.key}-${name}`] && (
                  <small role="alert">{errors[`parameter-${entry.key}-${name}`]}</small>
                )}
              </fieldset>
            ))
          )}
          {draft.strategyPool.flatMap((entry) =>
            (entry.constraints ?? []).map((constraint) => (
              <p key={`${entry.key}:${constraint.lowerParameter}:${constraint.upperParameter}`}>
                Constraint: {entry.displayName}.{constraint.lowerParameter} &lt; {entry.displayName}
                .{constraint.upperParameter}
              </p>
            ))
          )}
          <label>
            Minimum components
            <input
              aria-label="Minimum components"
              type="number"
              min="1"
              max={draft.strategyPool.length || 1}
              value={draft.minimumComponents}
              onChange={(e) => update("minimumComponents", Number(e.target.value))}
            />
          </label>
          <label>
            Maximum components
            <input
              aria-label="Maximum components"
              type="number"
              min="1"
              max={draft.strategyPool.length || 1}
              value={draft.maximumComponents}
              onChange={(e) => update("maximumComponents", Number(e.target.value))}
            />
            {errors.componentBounds && <small role="alert">{errors.componentBounds}</small>}
          </label>
          <label>
            Combination policy
            <select
              aria-label="Combination policy"
              value={draft.combinationPolicyId}
              onChange={() => undefined}
            >
              <option value="majority-vote">Majority Vote 1.0.0</option>
            </select>
          </label>
          <label>
            Worker concurrency
            <input
              aria-label="Worker concurrency"
              type="number"
              min="1"
              max="64"
              value={draft.requestedConcurrency}
              onChange={(e) => update("requestedConcurrency", Number(e.target.value))}
            />
            {errors.requestedConcurrency && (
              <small role="alert">{errors.requestedConcurrency}</small>
            )}
          </label>
          <div>
            <span>Search-space cardinality</span>
            <output aria-label="Search space cardinality">
              {searchSpaceCardinality.toString()}
            </output>
            <small>Unique composite candidates before stop limits are applied.</small>
          </div>
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
            Stop after candidates without improvement (optional)
            <input
              aria-label="Candidates without improvement"
              type="number"
              min="1"
              value={draft.maximumWithoutImprovement}
              onChange={(e) => update("maximumWithoutImprovement", e.target.value)}
            />
            {errors.maximumWithoutImprovement && (
              <small role="alert">{errors.maximumWithoutImprovement}</small>
            )}
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
        <section aria-label="Search configuration review" className="panel">
          <h3>Review Search configuration</h3>
          <dl>
            <div>
              <dt>Dataset</dt>
              <dd>
                {selectedDataset
                  ? `${selectedDataset.pair} · ${selectedDataset.timeframe}`
                  : "Not selected"}
              </dd>
            </div>
            <div>
              <dt>Backtest assumptions</dt>
              <dd>
                {draft.initialCapital} {quoteAsset}; fee {draft.feePercent}%; slippage{" "}
                {draft.slippagePercent}%
              </dd>
            </div>
            <div>
              <dt>Strategy pool</dt>
              <dd>
                {draft.strategyPool.map((entry) => entry.displayName).join(" + ") || "Empty"}
              </dd>
            </div>
            <div>
              <dt>Composition</dt>
              <dd>
                {draft.minimumComponents}–{draft.maximumComponents} components · Majority Vote
                1.0.0
              </dd>
            </div>
            <div>
              <dt>Generator</dt>
              <dd>
                {draft.generatorId} {draft.generatorVersion} · seed {draft.seed}
              </dd>
            </div>
            <div>
              <dt>Budget</dt>
              <dd>
                {draft.maximumCandidates || "No candidate bound"} candidates ·{" "}
                {draft.maximumDurationSeconds || "No time bound"} seconds · concurrency{" "}
                {draft.requestedConcurrency} · Top-{draft.topK}
              </dd>
            </div>
            <div>
              <dt>Search-space cardinality</dt>
              <dd>{searchSpaceCardinality.toString()}</dd>
            </div>
          </dl>
        </section>
        <button className="button primary" disabled={commands.start.status === "submitting"}>
          {commands.start.status === "uncertain" ||
          commands.start.status === "retryable-failure" ||
          commands.start.status === "dependency-unavailable"
            ? "Retry Search"
            : "Start Experiment"}
        </button>
        {commands.start.status === "accepted" && (
          <p role="status">Experiment accepted. Opening its authoritative monitor…</p>
        )}
        {commands.start.status === "dependency-unavailable" && (
          <p role="alert">The Search service is temporarily unavailable. Try again later.</p>
        )}
        {commands.start.status === "conflict" && (
          <p role="alert">This submission key conflicts with an earlier request. Submit again.</p>
        )}
        {commands.start.status === "uncertain" && (
          <p role="alert">
            The submission result is uncertain. Retry preserves the same idempotency key.
          </p>
        )}
        {(commands.start.status === "terminal-failure" ||
          commands.start.status === "retryable-failure") && (
          <p role="alert">{commands.start.error.message} Your configuration has been preserved.</p>
        )}
      </form>
    </section>
  );
}
