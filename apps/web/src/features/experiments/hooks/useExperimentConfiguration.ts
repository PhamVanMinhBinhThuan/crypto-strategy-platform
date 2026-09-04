"use client";
import { useCallback, useState } from "react";
import {
  initialExperimentDraft,
  validateExperimentDraft,
  type ExperimentDraft,
  type SearchParameterDomain
} from "../types/experiment-configuration";
export function useExperimentConfiguration() {
  const [draft, setDraft] = useState<ExperimentDraft>(initialExperimentDraft);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const update = useCallback(
    <K extends keyof ExperimentDraft>(key: K, value: ExperimentDraft[K]) =>
      setDraft((d) => ({ ...d, [key]: value })),
    []
  );
  const updateParameter = useCallback(
    (name: string, domain: SearchParameterDomain) =>
      setDraft((d) => ({
        ...d,
        parameters: { ...d.parameters, [name]: domain }
      })),
    []
  );
  const selectStrategy = useCallback(
    (selection: {
      strategyId: string;
      strategyVersion: string;
      userStrategyVersionId?: string;
      parameters: Record<string, SearchParameterDomain>;
    }) => setDraft((d) => ({ ...d, ...selection })),
    []
  );
  const validate = () => {
    const value = validateExperimentDraft(draft);
    setErrors(value);
    return Object.keys(value).length === 0;
  };
  return { draft, errors, update, updateParameter, selectStrategy, validate };
}
