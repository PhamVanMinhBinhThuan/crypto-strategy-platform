export type SearchParameterDomain =
  { kind: "RANGE"; minimum: string; maximum: string } | { kind: "OPTIONS"; options: string[] };

export type ExperimentDraft = {
  name: string;
  datasetId: string;
  generatorId: string;
  generatorVersion: string;
  seed: string;
  strategyId: string;
  strategyVersion: string;
  userStrategyVersionId?: string;
  parameters: Record<string, SearchParameterDomain>;
  maximumCandidates: string;
  maximumDurationSeconds: string;
  topK: number;
};
export const initialExperimentDraft: ExperimentDraft = {
  name: "",
  datasetId: "",
  generatorId: "random-search",
  generatorVersion: "1.0.0",
  seed: "20260903",
  strategyId: "",
  strategyVersion: "",
  parameters: {},
  maximumCandidates: "100",
  maximumDurationSeconds: "300",
  topK: 10
};
export function validateExperimentDraft(d: ExperimentDraft) {
  const errors: Record<string, string> = {};
  if (!d.name.trim()) errors.name = "Name is required.";
  if (!d.datasetId.trim()) errors.datasetId = "Dataset ID is required.";
  if (!d.generatorId || !d.generatorVersion)
    errors.generatorId = "Generator identity and version are required.";
  if (!/^-?\d+$/.test(d.seed)) errors.seed = "Seed must be an integer.";
  if (!d.userStrategyVersionId && (!d.strategyId || !d.strategyVersion))
    errors.strategyId = "Strategy and version are required.";
  const positive = (v: string) => Number.isFinite(Number(v)) && Number(v) > 0;
  if (!positive(d.maximumCandidates) && !positive(d.maximumDurationSeconds))
    errors.stop = "At least one positive finite stop bound is required.";
  if (!Number.isInteger(d.topK) || d.topK < 1 || d.topK > 100)
    errors.topK = "Top-K must be an integer from 1 to 100.";
  for (const [name, domain] of Object.entries(d.parameters)) {
    if (domain.kind === "RANGE") {
      if (
        !/^-?\d+$/.test(domain.minimum) ||
        !/^-?\d+$/.test(domain.maximum) ||
        Number(domain.minimum) > Number(domain.maximum)
      )
        errors[`parameter-${name}`] = "Use an ordered integer range.";
    } else if (domain.options.length === 0) {
      errors[`parameter-${name}`] = "Select at least one option.";
    }
  }
  return errors;
}
export const draftPayload = (d: ExperimentDraft) => ({
  name: d.name.trim(),
  datasetId: d.datasetId.trim(),
  generator: { generatorId: d.generatorId, version: d.generatorVersion, seed: Number(d.seed) },
  ...(d.userStrategyVersionId
    ? { userStrategyVersionId: d.userStrategyVersionId }
    : {
        searchSpace: {
          strategyId: d.strategyId,
          strategyVersion: d.strategyVersion,
          parameters: Object.fromEntries(
            Object.entries(d.parameters).map(([name, domain]) => [
              name,
              domain.kind === "RANGE"
                ? { minimum: Number(domain.minimum), maximum: Number(domain.maximum) }
                : { options: domain.options }
            ])
          )
        }
      }),
  stopCondition: {
    ...(d.maximumCandidates ? { maximumCandidates: Number(d.maximumCandidates) } : {}),
    ...(d.maximumDurationSeconds
      ? { maximumDurationSeconds: Number(d.maximumDurationSeconds) }
      : {})
  },
  topK: d.topK
});
