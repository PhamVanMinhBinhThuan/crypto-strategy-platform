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
    <K extends keyof ExperimentDraft>(key: K, value: ExperimentDraft[K]) => {
      setDraft((d) => ({ ...d, [key]: value }));
      setErrors((current) => {
        const related = new Set<string>([String(key)]);
        if (key === "maximumCandidates" || key === "maximumDurationSeconds") related.add("stop");
        if (
          key === "maximumCandidates" ||
          key === "minimumComponents" ||
          key === "maximumComponents" ||
          key === "strategyPool"
        )
          related.add("topK");
        if (key === "strategyPool") related.add("componentBounds");
        if (![...related].some((errorKey) => errorKey in current)) return current;
        const next = { ...current };
        for (const errorKey of related) delete next[errorKey];
        return next;
      });
    },
    []
  );
  const updateParameter = useCallback(
    (name: string, domain: SearchParameterDomain) =>
      setDraft((d) => ({
        ...d,
        parameters: { ...d.parameters, [name]: domain },
        strategyPool: d.strategyPool.map((entry, index) =>
          index === 0 ? { ...entry, parameters: { ...entry.parameters, [name]: domain } } : entry
        )
      })),
    []
  );
  const selectStrategy = useCallback(
    (selection: {
      strategyId: string;
      strategyVersion: string;
      displayName?: string;
      userStrategyVersionId?: string;
      parameters: Record<string, SearchParameterDomain>;
      parameterInfo?: Record<string, { description: string }>;
      constraints?: ReadonlyArray<{ lowerParameter: string; upperParameter: string }>;
    }) =>
      setDraft((d) => ({
        ...d,
        ...selection,
        strategyPool: [
          {
            key: selection.userStrategyVersionId
              ? `user:${selection.userStrategyVersionId}`
              : `system:${selection.strategyId}:${selection.strategyVersion}`,
            displayName: selection.displayName ?? selection.strategyId,
            strategyId: selection.strategyId || undefined,
            strategyVersion: selection.strategyVersion || undefined,
            userStrategyVersionId: selection.userStrategyVersionId,
            parameters: selection.parameters,
            parameterInfo: selection.parameterInfo,
            constraints: selection.constraints
          }
        ],
        minimumComponents: 1,
        maximumComponents: 1
      })),
    []
  );
  const updatePoolParameter = useCallback(
    (key: string, name: string, domain: SearchParameterDomain) => {
      setDraft((d) => ({
        ...d,
        strategyPool: d.strategyPool.map((entry) =>
          entry.key === key
            ? { ...entry, parameters: { ...entry.parameters, [name]: domain } }
            : entry
        )
      }));
      setErrors((current) => {
        const errorKey = `parameter-${key}-${name}`;
        const related = Object.keys(current).filter(
          (keyName) =>
            keyName === "strategyPool" ||
            keyName === "topK" ||
            keyName.startsWith(`parameter-${key}-`)
        );
        if (!(errorKey in current) && related.length === 0) return current;
        const next = { ...current };
        for (const keyName of related) delete next[keyName];
        delete next[errorKey];
        return next;
      });
    },
    []
  );
  const validate = () => {
    const value = validateExperimentDraft(draft);
    setErrors(value);
    return Object.keys(value).length === 0;
  };
  const applyServerErrors = useCallback(
    (serverErrors: Readonly<Record<string, string>>) =>
      setErrors((current) => ({ ...current, ...serverErrors })),
    []
  );
  return {
    draft,
    errors,
    update,
    updateParameter,
    updatePoolParameter,
    selectStrategy,
    validate,
    applyServerErrors
  };
}
