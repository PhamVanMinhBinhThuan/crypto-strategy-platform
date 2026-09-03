export type ExperimentDraft = {
  name: string;
  datasetId: string;
  generatorId: string;
  generatorVersion: string;
  seed: string;
  strategyId: string;
  strategyVersion: string;
  parameters: Record<string, { minimum: string; maximum: string }>;
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
  strategyId: "ma-crossover",
  strategyVersion: "1.0.0",
  parameters: {
    fastPeriod: { minimum: "5", maximum: "30" },
    slowPeriod: { minimum: "40", maximum: "120" }
  },
  maximumCandidates: "100",
  maximumDurationSeconds: "300",
  topK: 10
};
export function validateExperimentDraft(d: ExperimentDraft) {
  const errors: Record<string, string> = {};
  if (!d.name.trim()) errors.name = "Name is required.";
  if (!d.datasetId.trim()) errors.datasetId = "Dataset ID is required.";
  if (!d.generatorId || !d.generatorVersion)
    errors.generatorId = "Fixture generator identity and version are required.";
  if (!/^-?\d+$/.test(d.seed)) errors.seed = "Seed must be an integer.";
  if (!d.strategyId || !d.strategyVersion) errors.strategyId = "Strategy and version are required.";
  const positive = (v: string) => Number.isFinite(Number(v)) && Number(v) > 0;
  if (!positive(d.maximumCandidates) && !positive(d.maximumDurationSeconds))
    errors.stop = "At least one positive finite stop bound is required.";
  if (!Number.isInteger(d.topK) || d.topK < 1 || d.topK > 100)
    errors.topK = "Top-K must be an integer from 1 to 100.";
  for (const [name, r] of Object.entries(d.parameters))
    if (
      !Number.isFinite(Number(r.minimum)) ||
      !Number.isFinite(Number(r.maximum)) ||
      Number(r.minimum) > Number(r.maximum)
    )
      errors[`parameter-${name}`] = "Minimum cannot exceed maximum.";
  return errors;
}
export const draftPayload = (d: ExperimentDraft) => ({
  name: d.name.trim(),
  datasetId: d.datasetId.trim(),
  generator: { generatorId: d.generatorId, version: d.generatorVersion, seed: Number(d.seed) },
  searchSpace: {
    strategyId: d.strategyId,
    strategyVersion: d.strategyVersion,
    parameters: Object.fromEntries(
      Object.entries(d.parameters).map(([k, v]) => [
        k,
        { minimum: Number(v.minimum), maximum: Number(v.maximum) }
      ])
    )
  },
  stopCondition: {
    ...(d.maximumCandidates ? { maximumCandidates: Number(d.maximumCandidates) } : {}),
    ...(d.maximumDurationSeconds
      ? { maximumDurationSeconds: Number(d.maximumDurationSeconds) }
      : {})
  },
  topK: d.topK
});
