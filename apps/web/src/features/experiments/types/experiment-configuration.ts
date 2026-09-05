export type SearchParameterDomain =
  | {
      kind: "RANGE";
      valueType?: "INTEGER" | "DECIMAL";
      minimum: string;
      maximum: string;
      step?: string;
    }
  | { kind: "OPTIONS"; options: string[] };

export type StrategyPoolEntryDraft = {
  key: string;
  displayName: string;
  strategyId?: string;
  strategyVersion?: string;
  userStrategyVersionId?: string;
  parameters: Record<string, SearchParameterDomain>;
  constraints?: ReadonlyArray<{ lowerParameter: string; upperParameter: string }>;
};

export type ExperimentDraft = {
  name: string;
  datasetId: string;
  pair: string;
  timeframe: string;
  startUtc: string;
  endUtc: string;
  initialCapital: string;
  feePercent: string;
  slippagePercent: string;
  generatorId: string;
  generatorVersion: string;
  seed: string;
  strategyId: string;
  strategyVersion: string;
  userStrategyVersionId?: string;
  parameters: Record<string, SearchParameterDomain>;
  strategyPool: StrategyPoolEntryDraft[];
  minimumComponents: number;
  maximumComponents: number;
  combinationPolicyId: "majority-vote";
  combinationPolicyVersion: "1.0.0";
  requestedConcurrency: number;
  maximumCandidates: string;
  maximumDurationSeconds: string;
  maximumWithoutImprovement: string;
  topK: number;
};
const defaultRange = () => {
  const end = new Date();
  end.setUTCMinutes(0, 0, 0);
  const start = new Date(end.getTime() - 30 * 24 * 60 * 60 * 1000);
  const value = (date: Date) => date.toISOString().slice(0, 16);
  return { startUtc: value(start), endUtc: value(end) };
};
export const initialExperimentDraft: ExperimentDraft = {
  name: "",
  datasetId: "",
  pair: "BTC/USDT",
  timeframe: "1h",
  ...defaultRange(),
  initialCapital: "10000",
  feePercent: "0.1",
  slippagePercent: "0",
  generatorId: "random-search",
  generatorVersion: "1.0.0",
  seed: "20260903",
  strategyId: "",
  strategyVersion: "",
  parameters: {},
  strategyPool: [],
  minimumComponents: 1,
  maximumComponents: 1,
  combinationPolicyId: "majority-vote",
  combinationPolicyVersion: "1.0.0",
  requestedConcurrency: 4,
  maximumCandidates: "100",
  maximumDurationSeconds: "300",
  maximumWithoutImprovement: "",
  topK: 10
};
export function validateExperimentDraft(d: ExperimentDraft) {
  const errors: Record<string, string> = {};
  const decimal = /^(?:\d+(?:\.\d*)?|\.\d+)$/;
  if (!d.name.trim()) errors.name = "Name is required.";
  if (!d.datasetId.trim()) errors.datasetId = "Dataset must be created or selected.";
  if (!d.pair.trim()) errors.pair = "Pair is required.";
  if (!d.timeframe) errors.timeframe = "Timeframe is required.";
  const start = Date.parse(`${d.startUtc}Z`);
  const end = Date.parse(`${d.endUtc}Z`);
  if (!Number.isFinite(start)) errors.startUtc = "Enter a valid UTC start.";
  if (!Number.isFinite(end) || (Number.isFinite(start) && end <= start))
    errors.endUtc = "End UTC must be later than start UTC.";
  if (
    !decimal.test(d.initialCapital.trim()) ||
    !Number.isFinite(Number(d.initialCapital)) ||
    Number(d.initialCapital) <= 0
  )
    errors.initialCapital = "Initial simulated capital must be a positive decimal.";
  const validPercent = (value: string) =>
    decimal.test(value.trim()) &&
    Number.isFinite(Number(value)) &&
    Number(value) >= 0 &&
    Number(value) < 100;
  if (!validPercent(d.feePercent))
    errors.feePercent = "Transaction fee must be between 0% (inclusive) and 100% (exclusive).";
  if (!validPercent(d.slippagePercent))
    errors.slippagePercent = "Slippage must be between 0% (inclusive) and 100% (exclusive).";
  if (!d.generatorId || !d.generatorVersion)
    errors.generatorId = "Generator identity and version are required.";
  if (!/^-?\d+$/.test(d.seed)) errors.seed = "Seed must be an integer.";
  if (d.strategyPool.length === 0) errors.strategyPool = "Select at least one Strategy.";
  if (
    !Number.isInteger(d.minimumComponents) ||
    !Number.isInteger(d.maximumComponents) ||
    d.minimumComponents < 1 ||
    d.maximumComponents < d.minimumComponents ||
    d.maximumComponents > d.strategyPool.length
  )
    errors.componentBounds = "Component bounds must fit inside the selected Strategy pool.";
  if (
    !Number.isInteger(d.requestedConcurrency) ||
    d.requestedConcurrency < 1 ||
    d.requestedConcurrency > 64
  )
    errors.requestedConcurrency = "Worker concurrency must be an integer from 1 to 64.";
  const positive = (v: string) => Number.isFinite(Number(v)) && Number(v) > 0;
  if (!positive(d.maximumCandidates) && !positive(d.maximumDurationSeconds))
    errors.stop = "At least one positive finite stop bound is required.";
  if (d.maximumWithoutImprovement && !positive(d.maximumWithoutImprovement))
    errors.maximumWithoutImprovement = "No-improvement threshold must be positive when set.";
  if (!Number.isInteger(d.topK) || d.topK < 1 || d.topK > 100)
    errors.topK = "Top-K must be an integer from 1 to 100.";
  for (const entry of d.strategyPool)
    for (const [name, domain] of Object.entries(entry.parameters)) {
      if (domain.kind === "RANGE") {
        const numeric = /^-?(?:\d+(?:\.\d*)?|\.\d+)$/;
        const integer = /^-?\d+$/;
        const pattern = domain.valueType === "DECIMAL" ? numeric : integer;
        if (
          !pattern.test(domain.minimum) ||
          !pattern.test(domain.maximum) ||
          Number(domain.minimum) > Number(domain.maximum) ||
          (domain.step !== undefined && (!numeric.test(domain.step) || Number(domain.step) <= 0))
        )
          errors[`parameter-${entry.key}-${name}`] =
            "Use an ordered numeric range and positive step.";
      } else if (domain.options.length === 0) {
        errors[`parameter-${entry.key}-${name}`] = "Select at least one option.";
      }
    }
  return errors;
}

const percentageToRate = (value: string) => {
  const normalized = value.trim();
  const [whole, fraction = ""] = normalized.split(".");
  const digits = `${whole}${fraction}`.replace(/^0+/, "") || "0";
  const scale = fraction.length + 2;
  const padded = digits.padStart(scale + 1, "0");
  const integerPart = padded.slice(0, -scale);
  const fractionalPart = padded.slice(-scale).replace(/0+$/, "");
  return fractionalPart ? `${integerPart}.${fractionalPart}` : integerPart;
};

export const draftPayload = (d: ExperimentDraft) => ({
  configurationVersion: 2,
  name: d.name.trim(),
  datasetId: d.datasetId.trim(),
  backtestConfiguration: {
    initialCapital: d.initialCapital.trim(),
    feeRate: percentageToRate(d.feePercent),
    slippageRate: percentageToRate(d.slippagePercent)
  },
  generator: { generatorId: d.generatorId, version: d.generatorVersion, seed: Number(d.seed) },
  searchSpace: {
    schemaVersion: 2,
    strategyPool: d.strategyPool.map((entry) => ({
      ...(entry.userStrategyVersionId
        ? { artifactType: "PUBLISHED", userStrategyVersionId: entry.userStrategyVersionId }
        : {
            artifactType: "BUILT_IN",
            strategyId: entry.strategyId,
            version: entry.strategyVersion
          }),
      parameterDomains: Object.fromEntries(
        Object.entries(entry.parameters).map(([name, domain]) => [
          name,
          domain.kind === "RANGE"
            ? {
                kind: domain.valueType === "DECIMAL" ? "DECIMAL_RANGE" : "INTEGER_RANGE",
                min: Number(domain.minimum),
                max: Number(domain.maximum),
                step: Number(domain.step ?? (domain.valueType === "DECIMAL" ? "0.1" : "1"))
              }
            : { kind: "CHOICES", values: domain.options }
        ])
      )
    })),
    minComponents: d.minimumComponents,
    maxComponents: d.maximumComponents,
    combinationPolicy: {
      policyId: d.combinationPolicyId,
      version: d.combinationPolicyVersion,
      configuration: {}
    },
    constraints: d.strategyPool.flatMap((entry) =>
      (entry.constraints ?? []).map((constraint) => ({
        kind: "PARAMETER_LT",
        left: `${entry.strategyId}.${constraint.lowerParameter}`,
        right: `${entry.strategyId}.${constraint.upperParameter}`
      }))
    )
  },
  stopConditions: {
    ...(d.maximumCandidates ? { maximumCandidates: Number(d.maximumCandidates) } : {}),
    ...(d.maximumDurationSeconds
      ? { maximumDurationSeconds: Number(d.maximumDurationSeconds) }
      : {}),
    ...(d.maximumWithoutImprovement
      ? { maximumWithoutImprovement: Number(d.maximumWithoutImprovement) }
      : {})
  },
  topK: d.topK,
  requestedConcurrency: d.requestedConcurrency
});
