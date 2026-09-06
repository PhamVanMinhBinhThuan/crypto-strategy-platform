"use client";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ApiClient, ApiResult, PublicError } from "@/src/foundation/http/contracts";
import {
  forgetDataset,
  readRememberedDatasetId,
  rememberDataset
} from "@/src/foundation/navigation/dataset-selection";
import {
  getUserStrategy,
  listSystemStrategies,
  listUserStrategyVersions,
  listUserStrategies
} from "../../strategy/api/strategy-api";
import type { StrategyDescriptor, UserStrategyVersion } from "../../strategy/model/strategy";
import { useExperimentConfiguration } from "../hooks/useExperimentConfiguration";
import { useExperimentCommands } from "../hooks/useExperimentCommands";
import {
  validateExperimentDraft,
  type SearchParameterDomain
} from "../types/experiment-configuration";
import { DependencyGateNotice } from "./DependencyGateNotice";
import {
  ExperimentConfigSection,
  ExperimentConfigurationSummary,
  SearchParameterEditor,
  StrategySelectionCard
} from "./ExperimentConfigurationSections";
import { TechnicalDetails } from "./TechnicalDetails";

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
            minimum:
              parameter.searchRangeHint?.minimum ??
              parameter.minimum ??
              parameter.defaultValue ??
              "0",
            maximum:
              parameter.searchRangeHint?.maximum ??
              parameter.maximum ??
              parameter.defaultValue ??
              "0",
            step: parameter.searchRangeHint?.step ?? (parameter.type === "DECIMAL" ? "0.1" : "1")
          }
        : { kind: "OPTIONS" as const, options: parameter.allowedValues }
    ])
  );
const parameterInfo = (strategy: StrategyDescriptor) =>
  Object.fromEntries(
    strategy.parameters.map((parameter) => [parameter.name, { description: parameter.description }])
  );
const parameterLabels: Readonly<Record<string, string>> = {
  standardDeviation: "Standard deviation multiplier",
  fastPeriod: "Fast period",
  slowPeriod: "Slow period",
  period: "Period",
  buyThreshold: "Buy threshold",
  sellThreshold: "Sell threshold",
  lookback: "Lookback period",
  tolerance: "Price tolerance",
  tolerancePercent: "Price tolerance (%)"
};
const humanize = (value: string) =>
  parameterLabels[value] ??
  value.replace(/([a-z])([A-Z])/g, "$1 $2").replace(/^./, (letter) => letter.toUpperCase());
const formatNumber = (value: number | bigint) =>
  new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
const secondsToMinutes = (value: string) => {
  const seconds = Number(value);
  if (!value || !Number.isFinite(seconds)) return "";
  return String(seconds / 60);
};
const minutesToSeconds = (value: string) => {
  const minutes = Number(value);
  if (!value.trim() || !Number.isFinite(minutes) || minutes <= 0) return "";
  return String(Math.round(minutes * 60));
};
const timeframeMilliseconds: Readonly<Record<string, number>> = {
  "1m": 60_000,
  "5m": 5 * 60_000,
  "15m": 15 * 60_000,
  "1h": 60 * 60_000,
  "4h": 4 * 60 * 60_000,
  "1d": 24 * 60 * 60_000
};
const formatUtc = (value: string) =>
  new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "UTC"
  }).format(new Date(value));
const datasetOptionLabel = (dataset: DatasetResponse) =>
  `${dataset.pair} · ${dataset.timeframe} · ${formatUtc(dataset.startTime)} → ${formatUtc(dataset.endTime)} UTC · ${dataset.membershipCount} candles · created ${formatUtc(dataset.createdAt)}`;

type DatasetResponse = Readonly<{
  datasetId: string;
  provider: string;
  pair: string;
  timeframe: string;
  startTime: string;
  endTime: string;
  membershipCount: number;
  checksum: string;
  normalizationVersion: string;
  status: string;
  createdAt: string;
}>;
type DatasetListResponse = Readonly<{
  items: DatasetResponse[];
  nextCursor?: string | null;
  hasMore?: boolean;
  totalCount?: number;
}>;
type GeneratorResponse = Readonly<{
  generatorId: string;
  version: string;
  displayName: string;
}>;
type GeneratorListResponse = Readonly<{ items: GeneratorResponse[] }>;
type PublishedStrategyOption = Readonly<{
  name: string;
  description: string;
  version: UserStrategyVersion;
}>;

const cardinality = (
  pool: ReadonlyArray<{
    parameters: Record<string, SearchParameterDomain>;
    constraints?: ReadonlyArray<{ lowerParameter: string; upperParameter: string }>;
  }>,
  minimum: number,
  maximum: number
) => {
  const options = (domain: SearchParameterDomain) => {
    if (domain.kind === "OPTIONS") return domain.options;

    const minimum = Number(domain.minimum);
    const maximum = Number(domain.maximum);
    const step = Number(domain.step ?? "1");
    if (
      !Number.isFinite(minimum) ||
      !Number.isFinite(maximum) ||
      !Number.isFinite(step) ||
      step <= 0 ||
      maximum < minimum
    )
      return [];

    const length = Math.floor((maximum - minimum) / step) + 1;
    if (!Number.isSafeInteger(length) || length > 10_000) return [];
    return Array.from({ length }, (_, index) => String(minimum + index * step));
  };
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
    return "The market data provider is unavailable. Check the API network connection and retry.";
  return error.message;
};

export function ExperimentConfigurationForm({
  api,
  fixture,
  initialUserStrategyVersionId
}: {
  api: ApiClient;
  fixture: boolean;
  initialUserStrategyVersionId?: string;
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
  const [publishedStrategies, setPublishedStrategies] = useState<PublishedStrategyOption[]>([]);
  const [catalogState, setCatalogState] = useState<"loading" | "ready" | "error">("loading");
  const [datasetState, setDatasetState] = useState<
    | { status: "idle" | "creating" }
    | { status: "ready"; membershipCount: number }
    | { status: "error"; message: string; correlationId?: string }
  >({ status: "idle" });
  const [datasetCatalogState, setDatasetCatalogState] = useState<"loading" | "ready" | "error">(
    "loading"
  );
  const [datasets, setDatasets] = useState<DatasetResponse[]>([]);
  const [datasetNextCursor, setDatasetNextCursor] = useState<string | null>(null);
  const [datasetHasMore, setDatasetHasMore] = useState(false);
  const [datasetTotalCount, setDatasetTotalCount] = useState(0);
  const [datasetPageLoading, setDatasetPageLoading] = useState(false);
  const [datasetRestorePending, setDatasetRestorePending] = useState(true);
  const datasetRestoreAttempted = useRef(false);
  const compatibleAutoSelectionAttempted = useRef(false);
  const [generators, setGenerators] = useState<GeneratorResponse[]>([]);
  const [showDatasetSelector, setShowDatasetSelector] = useState(true);
  const [showMarketConfiguration, setShowMarketConfiguration] = useState(true);
  const [expandedStrategies, setExpandedStrategies] = useState<ReadonlySet<string>>(
    () => new Set()
  );
  const [touched, setTouched] = useState<ReadonlySet<string>>(() => new Set());
  const [submitted, setSubmitted] = useState(false);
  const [timeLimitMinutes, setTimeLimitMinutes] = useState(() =>
    secondsToMinutes(draft.maximumDurationSeconds)
  );
  const [stopWithoutImprovementEnabled, setStopWithoutImprovementEnabled] = useState(
    Boolean(draft.maximumWithoutImprovement)
  );

  const selectDataset = useCallback(
    (dataset: DatasetResponse) => {
      compatibleAutoSelectionAttempted.current = true;
      update("datasetId", dataset.datasetId);
      update("pair", dataset.pair);
      update("timeframe", dataset.timeframe);
      update("startUtc", new Date(dataset.startTime).toISOString().slice(0, 16));
      update("endUtc", new Date(dataset.endTime).toISOString().slice(0, 16));
      setDatasetState({ status: "ready", membershipCount: dataset.membershipCount });
      setShowDatasetSelector(false);
      setShowMarketConfiguration(false);
      rememberDataset(dataset.datasetId);
    },
    [update]
  );

  const loadDatasets = useCallback(
    async (cursor?: string) => {
      if (cursor) setDatasetPageLoading(true);
      else {
        datasetRestoreAttempted.current = false;
        setDatasetCatalogState("loading");
        setDatasetRestorePending(true);
      }
      const path = `/api/v1/datasets?limit=50${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ""}`;
      let result: ApiResult<DatasetListResponse>;
      try {
        result = await api.request<DatasetListResponse>(path);
      } catch {
        setDatasetCatalogState("error");
        setDatasetPageLoading(false);
        setDatasetRestorePending(false);
        return;
      }
      if (!result.ok) {
        setDatasetCatalogState("error");
        setDatasetPageLoading(false);
        setDatasetRestorePending(false);
        return;
      }

      setDatasets((current) =>
        cursor
          ? [...current, ...result.data.items].filter(
              (dataset, index, all) =>
                all.findIndex((item) => item.datasetId === dataset.datasetId) === index
            )
          : result.data.items
      );
      setDatasetNextCursor(result.data.nextCursor ?? null);
      setDatasetHasMore(Boolean(result.data.hasMore && result.data.nextCursor));
      setDatasetTotalCount(
        (current) =>
          result.data.totalCount ??
          (cursor ? current + result.data.items.length : result.data.items.length)
      );
      setDatasetCatalogState("ready");
      setDatasetPageLoading(false);

      if (cursor || datasetRestoreAttempted.current) {
        setDatasetRestorePending(false);
        return;
      }
      datasetRestoreAttempted.current = true;
      const rememberedId = readRememberedDatasetId();
      if (!rememberedId) {
        setDatasetRestorePending(false);
        return;
      }
      const listed = result.data.items.find((dataset) => dataset.datasetId === rememberedId);
      if (listed) {
        selectDataset(listed);
        setDatasetRestorePending(false);
        return;
      }
      let remembered: ApiResult<DatasetResponse>;
      try {
        remembered = await api.request<DatasetResponse>(
          `/api/v1/datasets/${encodeURIComponent(rememberedId)}`
        );
      } catch {
        datasetRestoreAttempted.current = false;
        setDatasetCatalogState("error");
        setDatasetRestorePending(false);
        return;
      }
      if (!remembered.ok) {
        if (
          ["RESOURCE_NOT_FOUND", "RESOURCE_INACCESSIBLE", "DATASET_NOT_FOUND"].includes(
            remembered.error.code
          )
        )
          forgetDataset();
        else {
          datasetRestoreAttempted.current = false;
          setDatasetCatalogState("error");
        }
        setDatasetRestorePending(false);
        return;
      }
      setDatasets((current) => [
        remembered.data,
        ...current.filter((item) => item.datasetId !== remembered.data.datasetId)
      ]);
      selectDataset(remembered.data);
      setDatasetRestorePending(false);
    },
    [api, selectDataset]
  );

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
      const strategies = await Promise.all(
        ownedResult.data.items.map(async (item) => ({
          details: await getUserStrategy(api, item.userStrategyId),
          history: await listUserStrategyVersions(api, item.userStrategyId)
        }))
      );
      if (!active) return;
      const published = strategies.flatMap(({ details, history }) => {
        if (!details.ok || !history.ok || details.data.status !== "ACTIVE") return [];
        return history.data.items
          .filter((version) => version.status === "PUBLISHED")
          .map((version) => ({
            name: details.data.name,
            description: details.data.description,
            version
          }));
      });
      setSystemStrategies(systemResult.data.items);
      setPublishedStrategies(published);
      setCatalogState("ready");
      const requested = published.find(
        (item) => item.version.userStrategyVersionId === initialUserStrategyVersionId
      );
      if (requested) {
        selectStrategy({
          strategyId: "",
          strategyVersion: "",
          displayName: `${requested.name} · version ${requested.version.versionNo}`,
          userStrategyVersionId: requested.version.userStrategyVersionId,
          parameters: {}
        });
        return;
      }
      const first = systemResult.data.items.find(supportedForSearch);
      if (first)
        selectStrategy({
          strategyId: first.strategyId,
          strategyVersion: first.version,
          displayName: first.displayName,
          userStrategyVersionId: undefined,
          parameters: searchDomains(first),
          parameterInfo: parameterInfo(first),
          constraints: first.constraints
        });
    };
    void load();
    return () => {
      active = false;
    };
  }, [api, initialUserStrategyVersionId, selectStrategy]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- route entry loads the external dataset catalog
    void loadDatasets();
  }, [loadDatasets]);

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
            parameterInfo: parameterInfo(strategy),
            constraints: strategy.constraints
          }
        ];
    update("strategyPool", strategyPool);
    update(
      "minimumComponents",
      Math.min(Math.max(1, draft.minimumComponents), strategyPool.length || 1)
    );
    update(
      "maximumComponents",
      Math.min(Math.max(1, draft.maximumComponents), strategyPool.length || 1)
    );
  };

  const toggleUserStrategy = (strategy: PublishedStrategyOption) => {
    const versionId = strategy.version.userStrategyVersionId;
    const key = `user:${versionId}`;
    const selected = draft.strategyPool.some((entry) => entry.key === key);
    const strategyPool = selected
      ? draft.strategyPool.filter((entry) => entry.key !== key)
      : [
          ...draft.strategyPool,
          {
            key,
            displayName: `${strategy.name} · version ${strategy.version.versionNo}`,
            userStrategyVersionId: versionId,
            parameters: {}
          }
        ];
    update("strategyPool", strategyPool);
    update(
      "minimumComponents",
      Math.min(Math.max(1, draft.minimumComponents), strategyPool.length || 1)
    );
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
        datasetMatchesDraft(dataset, draft.pair, draft.timeframe, draft.startUtc, draft.endUtc)
      ),
    [datasets, draft.pair, draft.timeframe, draft.startUtc, draft.endUtc]
  );
  const otherDatasets = useMemo(
    () => datasets.filter((dataset) => !compatibleDatasets.includes(dataset)),
    [compatibleDatasets, datasets]
  );
  const selectedDataset = useMemo(
    () => datasets.find((dataset) => dataset.datasetId === draft.datasetId),
    [datasets, draft.datasetId]
  );
  const quoteAsset = draft.pair.split("/")[1]?.trim() || "quote asset";
  const estimatedCandles = useMemo(() => {
    const start = Date.parse(`${draft.startUtc}Z`);
    const end = Date.parse(`${draft.endUtc}Z`);
    const interval = timeframeMilliseconds[draft.timeframe];
    if (!Number.isFinite(start) || !Number.isFinite(end) || !interval || end <= start) return null;
    return Math.floor((end - start) / interval);
  }, [draft.endUtc, draft.startUtc, draft.timeframe]);
  const draftValidationErrors = useMemo(() => validateExperimentDraft(draft), [draft]);
  const maximumCandidates = Number(draft.maximumCandidates);
  const candidateLimitValid = Number.isInteger(maximumCandidates) && maximumCandidates > 0;
  const timeLimitValid =
    !!timeLimitMinutes.trim() &&
    Number.isFinite(Number(timeLimitMinutes)) &&
    Number(timeLimitMinutes) * 60 >= 1;
  const effectiveCandidateCount = candidateLimitValid
    ? searchSpaceCardinality < BigInt(maximumCandidates)
      ? searchSpaceCardinality
      : BigInt(maximumCandidates)
    : 0n;
  const maximumLeaderboardSize = Number(
    effectiveCandidateCount > 100n ? 100n : effectiveCandidateCount
  );
  const leaderboardSizeValid =
    Number.isInteger(draft.topK) && draft.topK > 0 && effectiveCandidateCount >= BigInt(draft.topK);

  const fieldError = (key: string) => {
    if (!submitted && !touched.has(key)) return undefined;
    if (key === "maximumCandidates" && !candidateLimitValid)
      return "Candidate limit must be a positive whole number.";
    if (key === "maximumDurationSeconds" && !timeLimitValid)
      return "Time limit must be a positive number of minutes.";
    if (key === "topK" && !leaderboardSizeValid)
      return "Leaderboard size cannot exceed the candidates that can be evaluated.";
    return errors[key] ?? draftValidationErrors[key];
  };
  const touch = (key: string) => {
    setTouched((current) => new Set(current).add(key));
    validate();
  };

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
      // eslint-disable-next-line react-hooks/set-state-in-effect -- invalidate a selected snapshot that no longer matches the form
      setDatasetState({ status: "idle" });
      forgetDataset();
    }
  }, [draft.pair, draft.timeframe, draft.startUtc, draft.endUtc, selectedDataset, update]);

  useEffect(() => {
    if (
      datasetCatalogState !== "ready" ||
      datasetRestorePending ||
      compatibleAutoSelectionAttempted.current ||
      draft.datasetId
    )
      return;
    compatibleAutoSelectionAttempted.current = true;
    const compatible = compatibleDatasets[0];
    // eslint-disable-next-line react-hooks/set-state-in-effect -- restored catalog state selects one compatible external snapshot
    if (compatible) selectDataset(compatible);
  }, [
    compatibleDatasets,
    datasetCatalogState,
    datasetRestorePending,
    draft.datasetId,
    selectDataset
  ]);

  useEffect(() => {
    if (draft.strategyPool.length <= 1) {
      if (draft.minimumComponents !== 1) update("minimumComponents", 1);
      if (draft.maximumComponents !== 1) update("maximumComponents", 1);
    }
  }, [draft.maximumComponents, draft.minimumComponents, draft.strategyPool.length, update]);

  useEffect(() => {
    if (maximumLeaderboardSize > 0 && draft.topK > maximumLeaderboardSize)
      update("topK", maximumLeaderboardSize);
  }, [draft.topK, maximumLeaderboardSize, update]);

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
        setDatasets((current) => [
          result.data,
          ...current.filter((item) => item.datasetId !== result.data.datasetId)
        ]);
        selectDataset(result.data);
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
        message:
          "The API connection failed before a response was received. Check the API and retry."
      });
    }
  };
  const validateDatasetLookback = () => {
    if (!selectedDataset) return true;
    const lookbackParameters: Record<string, readonly [string, number]> = {
      "bollinger-bands": ["period", 0],
      "ma-crossover": ["slowPeriod", 0],
      "rsi-threshold": ["period", 1],
      "support-resistance": ["lookback", 1]
    };
    const lookbackErrors: Record<string, string> = {};
    for (const entry of draft.strategyPool) {
      const rule = entry.strategyId ? lookbackParameters[entry.strategyId] : undefined;
      if (!rule) continue;
      const domain = entry.parameters[rule[0]];
      if (domain?.kind !== "RANGE") continue;
      const required = Number(domain.maximum) + rule[1];
      if (Number.isFinite(required) && required > selectedDataset.membershipCount)
        lookbackErrors[`parameter-${entry.key}-${rule[0]}`] =
          `Maximum lookback requires ${required} candles, but this dataset has ${selectedDataset.membershipCount}.`;
    }
    if (Object.keys(lookbackErrors).length) applyServerErrors(lookbackErrors);
    return Object.keys(lookbackErrors).length === 0;
  };
  const validateCrossParameterConstraints = () => {
    const constraintErrors: Record<string, string> = {};
    const extrema = (domain: SearchParameterDomain | undefined) => {
      if (!domain) return null;
      const values =
        domain.kind === "RANGE"
          ? [Number(domain.minimum), Number(domain.maximum)]
          : domain.options.map(Number).filter(Number.isFinite);
      if (!values.length || values.some((value) => !Number.isFinite(value))) return null;
      return { minimum: Math.min(...values), maximum: Math.max(...values) };
    };
    for (const entry of draft.strategyPool) {
      for (const constraint of entry.constraints ?? []) {
        const lower = extrema(entry.parameters[constraint.lowerParameter]);
        const upper = extrema(entry.parameters[constraint.upperParameter]);
        if (lower && upper && lower.minimum >= upper.maximum) {
          const message = `${humanize(constraint.lowerParameter)} must have at least one value lower than ${humanize(constraint.upperParameter)}.`;
          constraintErrors[`parameter-${entry.key}-${constraint.lowerParameter}`] = message;
          constraintErrors[`parameter-${entry.key}-${constraint.upperParameter}`] = message;
        }
      }
    }
    if (Object.keys(constraintErrors).length) applyServerErrors(constraintErrors);
    return Object.keys(constraintErrors).length === 0;
  };
  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    const localErrors: Record<string, string> = {};
    if (!candidateLimitValid)
      localErrors.maximumCandidates = "Candidate limit must be a positive whole number.";
    if (!timeLimitValid)
      localErrors.maximumDurationSeconds = "Time limit must be a positive number of minutes.";
    if (searchSpaceCardinality === 0n)
      localErrors.strategyPool =
        "The selected search space does not contain a valid configuration.";
    if (!leaderboardSizeValid)
      localErrors.topK = "Leaderboard size cannot exceed the candidates that can be evaluated.";
    if (Object.keys(localErrors).length) applyServerErrors(localErrors);
    const retry =
      commands.start.status === "uncertain" ||
      commands.start.status === "retryable-failure" ||
      commands.start.status === "dependency-unavailable";
    if (
      Object.keys(localErrors).length === 0 &&
      validate() &&
      validateCrossParameterConstraints() &&
      validateDatasetLookback()
    )
      void commands.startExperiment(draft, retry);
  };

  const selectedGenerator =
    generators.find((generator) => generator.generatorId === draft.generatorId) ?? generators[0];
  const generatorLabel = selectedGenerator
    ? `${selectedGenerator.displayName} · v${selectedGenerator.version}`
    : "Loading search method…";
  const combinedErrors = { ...draftValidationErrors, ...errors };
  if (!candidateLimitValid)
    combinedErrors.maximumCandidates = "Candidate limit must be a positive whole number.";
  if (!timeLimitValid)
    combinedErrors.maximumDurationSeconds = "Time limit must be a positive number of minutes.";
  if (searchSpaceCardinality === 0n && draft.strategyPool.length > 0)
    combinedErrors.strategyPool =
      "The selected search space does not contain a valid configuration.";
  if (!leaderboardSizeValid)
    combinedErrors.topK = "Leaderboard size cannot exceed the candidates that can be evaluated.";

  const operationallyBusy =
    commands.start.status === "submitting" ||
    catalogState === "loading" ||
    datasetCatalogState === "loading" ||
    datasetRestorePending ||
    datasetState.status === "creating";
  const formInvalid = Object.keys(combinedErrors).length > 0;
  const startDisabled = operationallyBusy || (submitted && formInvalid);
  const readinessMessage = operationallyBusy
    ? datasetState.status === "creating"
      ? "Creating the frozen dataset…"
      : "Loading configuration dependencies…"
    : !draft.name.trim()
      ? "Add an experiment name to continue."
      : !selectedDataset
        ? "Select or create a frozen dataset to continue."
        : draft.strategyPool.length === 0
          ? "Select at least one strategy to continue."
          : formInvalid
            ? "Review the highlighted configuration issues."
            : "Ready to start";
  const errorSections = Object.entries(combinedErrors).reduce<
    Array<{ key: string; section: string; label: string; message: string }>
  >((items, [key, message]) => {
    const section = ["name", "datasetId", "pair", "timeframe", "startUtc", "endUtc"].includes(key)
      ? "experiment-data"
      : key === "initialCapital" ||
          key === "feePercent" ||
          key === "slippagePercent" ||
          key === "componentBounds"
        ? "experiment-assumptions"
        : key.startsWith("parameter-") || key === "strategyPool"
          ? "experiment-strategies"
          : "experiment-search-limits";
    if (items.some((item) => item.section === section && item.message === message)) return items;
    items.push({ key, section, label: humanize(key), message });
    return items;
  }, []);
  const selectedDatasetSummary = selectedDataset
    ? `${selectedDataset.pair} · ${selectedDataset.timeframe} · ${formatNumber(selectedDataset.membershipCount)} candles`
    : "Not selected";
  const summaryTimeLimit = timeLimitValid
    ? `${formatNumber(Number(timeLimitMinutes))} min`
    : "Not set";

  return (
    <section className="panel config-panel experiment-config">
      <div className="section-heading experiment-config-heading">
        <div>
          <p className="eyebrow">Search configuration</p>
          <h2>Configure experiment</h2>
          <p className="experiment-config-intro">
            Choose frozen market data, define the strategy search space, and set the evaluation
            limits.
          </p>
        </div>
        {fixture && <span className="fixture-badge">FIXTURE DATA</span>}
      </div>
      {fixture && <DependencyGateNotice />}

      <form className="experiment-config-form" onSubmit={submit} noValidate>
        {submitted && errorSections.length > 0 && (
          <section
            className="experiment-config-error-summary"
            role="alert"
            aria-labelledby="configuration-errors-title"
          >
            <h3 id="configuration-errors-title">Review before starting</h3>
            <p>{errorSections.length} configuration section(s) need attention.</p>
            <ul>
              {errorSections.map((item) => (
                <li key={item.key}>
                  <a href={"#" + item.section}>{item.message}</a>
                </li>
              ))}
            </ul>
          </section>
        )}

        <div className="experiment-config-layout">
          <main className="experiment-config-main">
            <ExperimentConfigSection
              id="experiment-data"
              number={1}
              title="Experiment data"
              description="Name this run and select an immutable market snapshot."
              status={selectedDataset ? "Dataset ready" : "Dataset required"}
            >
              <label className="experiment-config-field">
                <span>Experiment name</span>
                <input
                  aria-label="Name"
                  value={draft.name}
                  onBlur={() => touch("name")}
                  onChange={(event) => update("name", event.target.value)}
                  aria-invalid={!!fieldError("name")}
                  placeholder="Example: BTC hourly strategy search"
                />
                {fieldError("name") && <small role="alert">{fieldError("name")}</small>}
              </label>

              {(showDatasetSelector || !selectedDataset) && (
                <div className="dataset-selector-field experiment-dataset-selector">
                  <div className="experiment-field-label-row">
                    <label htmlFor="frozen-dataset">Frozen dataset</label>
                    {selectedDataset && (
                      <button
                        type="button"
                        className="experiment-text-button"
                        onClick={() => setShowDatasetSelector(false)}
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                  <select
                    id="frozen-dataset"
                    aria-label="Frozen Dataset"
                    value={draft.datasetId}
                    onBlur={() => touch("datasetId")}
                    onChange={(event) => {
                      const dataset = datasets.find(
                        (item) => item.datasetId === event.target.value
                      );
                      if (dataset) selectDataset(dataset);
                      else {
                        update("datasetId", "");
                        setDatasetState({ status: "idle" });
                        setShowMarketConfiguration(true);
                        forgetDataset();
                      }
                    }}
                    aria-invalid={!!fieldError("datasetId")}
                    disabled={
                      datasetCatalogState === "loading" ||
                      datasetRestorePending ||
                      datasetState.status === "creating"
                    }
                  >
                    <option value="">Select a frozen dataset</option>
                    {compatibleDatasets.length > 0 && (
                      <optgroup label="Recommended · compatible with current configuration">
                        {compatibleDatasets.map((dataset) => (
                          <option key={dataset.datasetId} value={dataset.datasetId}>
                            {"Recommended — " + datasetOptionLabel(dataset)}
                          </option>
                        ))}
                      </optgroup>
                    )}
                    {otherDatasets.length > 0 && (
                      <optgroup label="Other frozen datasets">
                        {otherDatasets.map((dataset) => (
                          <option key={dataset.datasetId} value={dataset.datasetId}>
                            {datasetOptionLabel(dataset)}
                          </option>
                        ))}
                      </optgroup>
                    )}
                  </select>
                  {fieldError("datasetId") && <small role="alert">{fieldError("datasetId")}</small>}
                  {datasetCatalogState === "loading" && <small>Loading frozen datasets…</small>}
                  {datasetCatalogState === "ready" && (
                    <small>
                      {formatNumber(datasets.length)} of {formatNumber(datasetTotalCount)} ready
                      snapshots loaded.
                      {compatibleDatasets.length > 0
                        ? " A compatible snapshot is available for reuse."
                        : " No loaded snapshot exactly matches the current market range."}
                    </small>
                  )}
                  {datasetCatalogState === "error" && (
                    <span className="inline-feedback" role="alert">
                      Frozen datasets could not be loaded.
                      <button
                        type="button"
                        className="button secondary compact"
                        onClick={() => void loadDatasets()}
                      >
                        Retry loading datasets
                      </button>
                    </span>
                  )}
                  {datasetCatalogState === "ready" && datasetHasMore && datasetNextCursor && (
                    <button
                      type="button"
                      className="button secondary compact"
                      disabled={datasetPageLoading}
                      onClick={() => void loadDatasets(datasetNextCursor)}
                    >
                      {datasetPageLoading ? "Loading older datasets…" : "Load older datasets"}
                    </button>
                  )}
                </div>
              )}

              {selectedDataset && (
                <section
                  className="experiment-selected-dataset"
                  aria-label="Selected frozen dataset"
                >
                  <div className="experiment-selected-dataset-heading">
                    <div>
                      <span className="experiment-config-badge">Ready to reuse</span>
                      {compatibleDatasets.includes(selectedDataset) && (
                        <span className="experiment-config-badge subtle">Recommended</span>
                      )}
                    </div>
                    <strong>
                      {selectedDataset.provider} · {selectedDataset.pair} ·{" "}
                      {selectedDataset.timeframe}
                    </strong>
                  </div>
                  <dl className="experiment-dataset-summary">
                    <div>
                      <dt>UTC range</dt>
                      <dd>
                        {formatUtc(selectedDataset.startTime)} →{" "}
                        {formatUtc(selectedDataset.endTime)} UTC
                      </dd>
                    </div>
                    <div>
                      <dt>Candles</dt>
                      <dd>{formatNumber(selectedDataset.membershipCount)}</dd>
                    </div>
                  </dl>
                  <div className="experiment-inline-actions">
                    <button
                      type="button"
                      className="button secondary compact"
                      onClick={() => setShowDatasetSelector(true)}
                    >
                      Choose another
                    </button>
                    <button
                      type="button"
                      className="button secondary compact"
                      onClick={() => {
                        update("datasetId", "");
                        setDatasetState({ status: "idle" });
                        setShowDatasetSelector(true);
                        setShowMarketConfiguration(true);
                        forgetDataset();
                      }}
                    >
                      Create new frozen dataset
                    </button>
                  </div>
                  <TechnicalDetails
                    values={[
                      ["Dataset ID", selectedDataset.datasetId],
                      ["Checksum", selectedDataset.checksum],
                      ["Normalization version", selectedDataset.normalizationVersion],
                      ["Created", selectedDataset.createdAt]
                    ]}
                  />
                </section>
              )}

              {!selectedDataset && showMarketConfiguration && (
                <section className="experiment-market-config" aria-labelledby="market-config-title">
                  <div className="experiment-subsection-heading">
                    <div>
                      <h4 id="market-config-title">New dataset market range</h4>
                      <p>
                        Use a UTC range aligned to the selected timeframe. End time is exclusive.
                      </p>
                    </div>
                    {estimatedCandles !== null && (
                      <span className="experiment-estimate">
                        Estimated · {formatNumber(estimatedCandles)} candles
                      </span>
                    )}
                  </div>
                  <div className="experiment-field-grid">
                    <label className="experiment-config-field">
                      <span>Market pair</span>
                      <input
                        aria-label="Pair"
                        value={draft.pair}
                        onBlur={() => touch("pair")}
                        onChange={(event) => update("pair", event.target.value.toUpperCase())}
                        aria-invalid={!!fieldError("pair")}
                      />
                      {fieldError("pair") && <small role="alert">{fieldError("pair")}</small>}
                    </label>
                    <label className="experiment-config-field">
                      <span>Timeframe</span>
                      <select
                        aria-label="Timeframe"
                        value={draft.timeframe}
                        onBlur={() => touch("timeframe")}
                        onChange={(event) => update("timeframe", event.target.value)}
                      >
                        {["1m", "5m", "15m", "1h", "4h", "1d"].map((value) => (
                          <option key={value} value={value}>
                            {value}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label className="experiment-config-field">
                      <span>
                        Start time <small>UTC</small>
                      </span>
                      <input
                        aria-label="Start UTC"
                        type="datetime-local"
                        value={draft.startUtc}
                        onBlur={() => touch("startUtc")}
                        onChange={(event) => update("startUtc", event.target.value)}
                        aria-invalid={!!fieldError("startUtc")}
                      />
                      {fieldError("startUtc") && (
                        <small role="alert">{fieldError("startUtc")}</small>
                      )}
                    </label>
                    <label className="experiment-config-field">
                      <span>
                        End time <small>UTC · exclusive</small>
                      </span>
                      <input
                        aria-label="End UTC"
                        type="datetime-local"
                        value={draft.endUtc}
                        onBlur={() => touch("endUtc")}
                        onChange={(event) => update("endUtc", event.target.value)}
                        aria-invalid={!!fieldError("endUtc")}
                      />
                      {fieldError("endUtc") && <small role="alert">{fieldError("endUtc")}</small>}
                    </label>
                  </div>
                  <div className="experiment-create-dataset">
                    <button
                      type="button"
                      className="button secondary"
                      disabled={datasetState.status === "creating" || compatibleDatasets.length > 0}
                      onClick={() => void createDataset()}
                    >
                      {compatibleDatasets.length > 0
                        ? "Reuse the compatible dataset above"
                        : datasetState.status === "creating"
                          ? "Creating frozen dataset…"
                          : "Create new frozen dataset"}
                    </button>
                    {datasetState.status === "ready" && (
                      <small role="status">
                        Frozen dataset ready with {formatNumber(datasetState.membershipCount)}{" "}
                        candles.
                      </small>
                    )}
                    {datasetState.status === "error" && (
                      <small role="alert">
                        {datasetState.message}
                        {datasetState.correlationId
                          ? " Reference: " + datasetState.correlationId
                          : ""}
                      </small>
                    )}
                  </div>
                </section>
              )}
            </ExperimentConfigSection>

            <ExperimentConfigSection
              id="experiment-strategies"
              number={2}
              title="Strategy search space"
              description="Select strategies and define the parameter values Random Search may explore."
              status={
                draft.strategyPool.length === 0
                  ? "None selected"
                  : draft.strategyPool.length + " selected"
              }
            >
              <div
                className="experiment-strategy-grid"
                aria-label="Strategy catalog"
                aria-busy={catalogState === "loading"}
              >
                {systemStrategies.map((strategy) => {
                  const key = "system:" + strategy.strategyId + ":" + strategy.version;
                  const selected = draft.strategyPool.some((entry) => entry.key === key);
                  return (
                    <StrategySelectionCard
                      key={strategy.strategyVersionId}
                      name={strategy.displayName}
                      description={strategy.description}
                      selected={selected}
                      disabled={!supportedForSearch(strategy)}
                      onChange={() => toggleSystemStrategy(strategy)}
                    >
                      <TechnicalDetails
                        summaryLabel="Version details"
                        values={[
                          ["Version", strategy.version],
                          ["Descriptor fingerprint", strategy.descriptorFingerprint]
                        ]}
                      />
                    </StrategySelectionCard>
                  );
                })}
                {publishedStrategies.map((strategy) => {
                  const key = "user:" + strategy.version.userStrategyVersionId;
                  const selected = draft.strategyPool.some((entry) => entry.key === key);
                  return (
                    <StrategySelectionCard
                      key={strategy.version.userStrategyVersionId}
                      name={`${strategy.name} · version ${strategy.version.versionNo}`}
                      description={strategy.description}
                      selected={selected}
                      badge="Published"
                      onChange={() => toggleUserStrategy(strategy)}
                    >
                      <TechnicalDetails
                        summaryLabel="Version details"
                        values={[
                          ["Version", String(strategy.version.versionNo)],
                          ["Version ID", strategy.version.userStrategyVersionId],
                          ["Fingerprint", strategy.version.fingerprint]
                        ]}
                      />
                    </StrategySelectionCard>
                  );
                })}
              </div>
              {catalogState === "loading" && <p className="muted">Loading strategy catalog…</p>}
              {catalogState === "error" && (
                <small className="experiment-field-error" role="alert">
                  Strategy catalog is unavailable.
                </small>
              )}
              {fieldError("strategyPool") && (
                <small className="experiment-field-error" role="alert">
                  {fieldError("strategyPool")}
                </small>
              )}

              <div className="experiment-parameter-groups">
                {draft.strategyPool.map((entry) => {
                  const visibleParameters = Object.entries(entry.parameters).filter(
                    ([, domain]) => domain.kind !== "OPTIONS" || domain.options.length > 1
                  );
                  if (visibleParameters.length === 0) return null;
                  const hasError = visibleParameters.some(([name]) =>
                    Boolean(errors["parameter-" + entry.key + "-" + name])
                  );
                  const open =
                    draft.strategyPool.length === 1 ||
                    hasError ||
                    expandedStrategies.has(entry.key);
                  const descriptor = systemStrategies.find(
                    (strategy) =>
                      strategy.strategyId === entry.strategyId &&
                      strategy.version === entry.strategyVersion
                  );
                  const recommendedDomains = descriptor ? searchDomains(descriptor) : {};
                  return (
                    <details
                      key={entry.key}
                      className="experiment-strategy-parameters"
                      open={open}
                      onToggle={(event) => {
                        if (draft.strategyPool.length === 1) return;
                        const isOpen = event.currentTarget.open;
                        setExpandedStrategies((current) => {
                          const next = new Set(current);
                          if (isOpen) next.add(entry.key);
                          else next.delete(entry.key);
                          return next;
                        });
                      }}
                    >
                      <summary>
                        <span>
                          <strong>{entry.displayName}</strong>
                          <small>{visibleParameters.length} configurable parameter(s)</small>
                        </span>
                        {hasError && (
                          <span className="experiment-config-badge error">Needs attention</span>
                        )}
                      </summary>
                      <div className="experiment-parameter-list">
                        {visibleParameters.map(([name, domain]) => {
                          const errorKey = "parameter-" + entry.key + "-" + name;
                          const recommended = recommendedDomains[name];
                          return (
                            <SearchParameterEditor
                              key={errorKey}
                              strategyLabel={entry.displayName}
                              parameterName={name}
                              label={humanize(name)}
                              description={entry.parameterInfo?.[name]?.description}
                              domain={domain}
                              recommended={recommended}
                              error={fieldError(errorKey)}
                              onBlur={() => touch(errorKey)}
                              onChange={(value) => updatePoolParameter(entry.key, name, value)}
                              onReset={
                                recommended
                                  ? () => updatePoolParameter(entry.key, name, recommended)
                                  : undefined
                              }
                            />
                          );
                        })}
                        {(entry.constraints ?? []).map((constraint) => (
                          <p
                            className="experiment-constraint"
                            key={
                              entry.key +
                              ":" +
                              constraint.lowerParameter +
                              ":" +
                              constraint.upperParameter
                            }
                          >
                            <strong>Constraint:</strong> {humanize(constraint.lowerParameter)} must
                            include at least one value lower than{" "}
                            {humanize(constraint.upperParameter)}.
                          </p>
                        ))}
                      </div>
                    </details>
                  );
                })}
              </div>
            </ExperimentConfigSection>

            <ExperimentConfigSection
              id="experiment-assumptions"
              number={3}
              title="Composition & backtest assumptions"
              description="Control how strategies are combined and simulated for every candidate."
              status={
                draft.strategyPool.length > 1
                  ? draft.minimumComponents + "–" + draft.maximumComponents + " components"
                  : "Single strategy"
              }
            >
              {draft.strategyPool.length > 1 && (
                <section
                  className="experiment-config-subsection"
                  aria-labelledby="combination-title"
                >
                  <div className="experiment-subsection-heading">
                    <div>
                      <h4 id="combination-title">Strategy combination</h4>
                      <p>
                        Choose how many selected strategies each generated candidate may contain.
                      </p>
                    </div>
                    <span className="experiment-readonly-value">Majority Vote · v1.0.0</span>
                  </div>
                  <div className="experiment-field-grid">
                    <label className="experiment-config-field">
                      <span>Minimum components</span>
                      <input
                        aria-label="Minimum components"
                        type="number"
                        min="1"
                        max={draft.strategyPool.length}
                        value={draft.minimumComponents}
                        onBlur={() => touch("componentBounds")}
                        onChange={(event) =>
                          update("minimumComponents", Number(event.target.value))
                        }
                        aria-invalid={!!fieldError("componentBounds")}
                      />
                    </label>
                    <label className="experiment-config-field">
                      <span>Maximum components</span>
                      <input
                        aria-label="Maximum components"
                        type="number"
                        min="1"
                        max={draft.strategyPool.length}
                        value={draft.maximumComponents}
                        onBlur={() => touch("componentBounds")}
                        onChange={(event) =>
                          update("maximumComponents", Number(event.target.value))
                        }
                        aria-invalid={!!fieldError("componentBounds")}
                      />
                    </label>
                  </div>
                  {fieldError("componentBounds") && (
                    <small className="experiment-field-error" role="alert">
                      {fieldError("componentBounds")}
                    </small>
                  )}
                </section>
              )}

              <section className="experiment-config-subsection" aria-labelledby="assumptions-title">
                <div className="experiment-subsection-heading">
                  <div>
                    <h4 id="assumptions-title">Backtest assumptions</h4>
                    <p>
                      Simulated values shared by every candidate, not an exchange wallet balance.
                    </p>
                  </div>
                  <span className="experiment-readonly-value">
                    {formatNumber(Number(draft.initialCapital) || 0)} {quoteAsset} ·{" "}
                    {draft.feePercent || "0"}% fee · {draft.slippagePercent || "0"}% slippage
                  </span>
                </div>
                <div className="experiment-assumption-grid">
                  <label className="experiment-config-field">
                    <span>Initial simulated capital</span>
                    <span className="experiment-input-affix">
                      <input
                        aria-label="Initial simulated capital"
                        inputMode="decimal"
                        value={draft.initialCapital}
                        onBlur={() => touch("initialCapital")}
                        onChange={(event) => update("initialCapital", event.target.value)}
                        aria-invalid={!!fieldError("initialCapital")}
                      />
                      <span>{quoteAsset}</span>
                    </span>
                    {fieldError("initialCapital") && (
                      <small role="alert">{fieldError("initialCapital")}</small>
                    )}
                  </label>
                  <label className="experiment-config-field">
                    <span>Transaction fee</span>
                    <span className="experiment-input-affix">
                      <input
                        aria-label="Transaction fee (%)"
                        inputMode="decimal"
                        value={draft.feePercent}
                        onBlur={() => touch("feePercent")}
                        onChange={(event) => update("feePercent", event.target.value)}
                        aria-invalid={!!fieldError("feePercent")}
                      />
                      <span>%</span>
                    </span>
                    {fieldError("feePercent") && (
                      <small role="alert">{fieldError("feePercent")}</small>
                    )}
                  </label>
                  <label className="experiment-config-field">
                    <span>Slippage</span>
                    <span className="experiment-input-affix">
                      <input
                        aria-label="Slippage (%)"
                        inputMode="decimal"
                        value={draft.slippagePercent}
                        onBlur={() => touch("slippagePercent")}
                        onChange={(event) => update("slippagePercent", event.target.value)}
                        aria-invalid={!!fieldError("slippagePercent")}
                      />
                      <span>%</span>
                    </span>
                    {fieldError("slippagePercent") && (
                      <small role="alert">{fieldError("slippagePercent")}</small>
                    )}
                  </label>
                </div>
              </section>
            </ExperimentConfigSection>

            <ExperimentConfigSection
              id="experiment-search-limits"
              number={4}
              title="Search limits"
              description="Set how much of the search space is evaluated and retained."
              status={`Up to ${formatNumber(effectiveCandidateCount)} ${effectiveCandidateCount === 1n ? "candidate" : "candidates"}`}
            >
              <div className="experiment-search-method">
                <span>Search method</span>
                {generators.length <= 1 ? (
                  <strong>{generatorLabel}</strong>
                ) : (
                  <select
                    aria-label="Search method"
                    value={draft.generatorId}
                    onChange={(event) => {
                      const selected = generators.find(
                        (generator) => generator.generatorId === event.target.value
                      );
                      update("generatorId", event.target.value);
                      if (selected) update("generatorVersion", selected.version);
                    }}
                  >
                    {generators.map((generator) => (
                      <option
                        key={generator.generatorId + ":" + generator.version}
                        value={generator.generatorId}
                      >
                        {generator.displayName + " · v" + generator.version}
                      </option>
                    ))}
                  </select>
                )}
              </div>

              <div className="experiment-search-limit-grid">
                <label className="experiment-config-field">
                  <span>Candidate limit</span>
                  <input
                    aria-label="Candidate limit"
                    type="number"
                    min="1"
                    step="1"
                    value={draft.maximumCandidates}
                    onBlur={() => touch("maximumCandidates")}
                    onChange={(event) => update("maximumCandidates", event.target.value)}
                    aria-invalid={!!fieldError("maximumCandidates")}
                  />
                  {fieldError("maximumCandidates") && (
                    <small role="alert">{fieldError("maximumCandidates")}</small>
                  )}
                </label>
                <label className="experiment-config-field">
                  <span>
                    Time limit <small>minutes</small>
                  </span>
                  <input
                    aria-label="Time limit (minutes)"
                    type="number"
                    min="0.1"
                    step="0.1"
                    value={timeLimitMinutes}
                    onBlur={() => touch("maximumDurationSeconds")}
                    onChange={(event) => {
                      setTimeLimitMinutes(event.target.value);
                      update("maximumDurationSeconds", minutesToSeconds(event.target.value));
                    }}
                    aria-invalid={!!fieldError("maximumDurationSeconds")}
                  />
                  {fieldError("maximumDurationSeconds") && (
                    <small role="alert">{fieldError("maximumDurationSeconds")}</small>
                  )}
                </label>
                <label className="experiment-config-field">
                  <span>Leaderboard size</span>
                  <input
                    aria-label="Leaderboard size"
                    type="number"
                    min="1"
                    max={maximumLeaderboardSize || 100}
                    step="1"
                    value={draft.topK}
                    readOnly={maximumLeaderboardSize === 1}
                    onBlur={() => touch("topK")}
                    onChange={(event) => update("topK", Number(event.target.value))}
                    aria-invalid={!!fieldError("topK")}
                  />
                  {fieldError("topK") && <small role="alert">{fieldError("topK")}</small>}
                  {!fieldError("topK") && maximumLeaderboardSize > 0 && (
                    <small>
                      {maximumLeaderboardSize === 1
                        ? "The selected search space has one fixed configuration, so the leaderboard size is 1."
                        : `Choose from 1 to ${maximumLeaderboardSize}, based on the configurations that can be evaluated.`}
                    </small>
                  )}
                </label>
              </div>

              <section className="experiment-cardinality-insight" aria-live="polite">
                <strong>{formatNumber(searchSpaceCardinality)} possible configurations</strong>
                <span>
                  Up to {candidateLimitValid ? formatNumber(effectiveCandidateCount) : "—"}{" "}
                  {effectiveCandidateCount === 1n ? "configuration" : "configurations"} will be
                  evaluated.
                </span>
              </section>

              <label className="experiment-toggle">
                <input
                  type="checkbox"
                  checked={stopWithoutImprovementEnabled}
                  onChange={(event) => {
                    setStopWithoutImprovementEnabled(event.target.checked);
                    update(
                      "maximumWithoutImprovement",
                      event.target.checked ? draft.maximumWithoutImprovement || "10" : ""
                    );
                  }}
                />
                <span>
                  <strong>Stop when results stop improving</strong>
                  <small>
                    End the search after a chosen number of candidates without a better score.
                  </small>
                </span>
              </label>
              {stopWithoutImprovementEnabled && (
                <label className="experiment-config-field experiment-stop-threshold">
                  <span>Candidates without improvement</span>
                  <input
                    aria-label="Candidates without improvement"
                    type="number"
                    min="1"
                    step="1"
                    value={draft.maximumWithoutImprovement}
                    onBlur={() => touch("maximumWithoutImprovement")}
                    onChange={(event) => update("maximumWithoutImprovement", event.target.value)}
                    aria-invalid={!!fieldError("maximumWithoutImprovement")}
                  />
                  {fieldError("maximumWithoutImprovement") && (
                    <small role="alert">{fieldError("maximumWithoutImprovement")}</small>
                  )}
                </label>
              )}

              <details className="advanced-settings experiment-advanced-settings">
                <summary>Advanced settings</summary>
                <div className="experiment-field-grid">
                  <label className="experiment-config-field">
                    <span>Seed</span>
                    <input
                      aria-label="Seed"
                      inputMode="numeric"
                      value={draft.seed}
                      onBlur={() => touch("seed")}
                      onChange={(event) => update("seed", event.target.value)}
                      aria-invalid={!!fieldError("seed")}
                    />
                    <small>Reuse this seed to reproduce candidate generation.</small>
                    {fieldError("seed") && <small role="alert">{fieldError("seed")}</small>}
                  </label>
                  <label className="experiment-config-field">
                    <span>Parallel backtests</span>
                    <input
                      aria-label="Parallel backtests"
                      type="number"
                      min="1"
                      max="64"
                      value={draft.requestedConcurrency}
                      onBlur={() => touch("requestedConcurrency")}
                      onChange={(event) =>
                        update("requestedConcurrency", Number(event.target.value))
                      }
                      aria-invalid={!!fieldError("requestedConcurrency")}
                    />
                    <small>Higher values use more execution and database capacity.</small>
                    {fieldError("requestedConcurrency") && (
                      <small role="alert">{fieldError("requestedConcurrency")}</small>
                    )}
                  </label>
                </div>
              </details>
              {errors.stop && (
                <p className="experiment-field-error" role="alert">
                  {errors.stop}
                </p>
              )}
            </ExperimentConfigSection>
          </main>

          <div className="experiment-config-summary-desktop">
            <ExperimentConfigurationSummary
              dataset={selectedDatasetSummary}
              strategyCount={draft.strategyPool.length}
              cardinality={formatNumber(searchSpaceCardinality)}
              candidateLimit={draft.maximumCandidates}
              timeLimit={summaryTimeLimit}
              concurrency={draft.requestedConcurrency}
              leaderboardSize={draft.topK}
            />
          </div>
        </div>

        <details className="experiment-config-summary-mobile">
          <summary>Experiment summary</summary>
          <ExperimentConfigurationSummary
            headingId="experiment-summary-mobile-title"
            dataset={selectedDatasetSummary}
            strategyCount={draft.strategyPool.length}
            cardinality={formatNumber(searchSpaceCardinality)}
            candidateLimit={draft.maximumCandidates}
            timeLimit={summaryTimeLimit}
            concurrency={draft.requestedConcurrency}
            leaderboardSize={draft.topK}
          />
        </details>

        <div className="experiment-config-action-bar">
          <div
            className={
              formInvalid ? "experiment-readiness is-blocked" : "experiment-readiness is-ready"
            }
          >
            <span aria-hidden="true">{formInvalid || operationallyBusy ? "●" : "✓"}</span>
            <div>
              <strong>{readinessMessage}</strong>
              <small>
                {selectedDataset
                  ? selectedDatasetSummary
                  : "Your draft will be preserved if the API rejects the request."}
              </small>
            </div>
          </div>
          <button className="button primary" disabled={startDisabled}>
            {commands.start.status === "submitting"
              ? "Starting experiment…"
              : commands.start.status === "uncertain" ||
                  commands.start.status === "retryable-failure" ||
                  commands.start.status === "dependency-unavailable"
                ? "Retry search"
                : "Start experiment"}
          </button>
        </div>

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
          <p role="alert">
            The experiment could not be started. Your configuration has been preserved.
          </p>
        )}
      </form>
    </section>
  );
}
