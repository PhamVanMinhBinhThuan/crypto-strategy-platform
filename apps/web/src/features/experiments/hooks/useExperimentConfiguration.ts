"use client";
import { useState } from "react";
import {
  initialExperimentDraft,
  validateExperimentDraft,
  type ExperimentDraft
} from "../types/experiment-configuration";
export function useExperimentConfiguration() {
  const [draft, setDraft] = useState<ExperimentDraft>(initialExperimentDraft);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const update = <K extends keyof ExperimentDraft>(key: K, value: ExperimentDraft[K]) =>
    setDraft((d) => ({ ...d, [key]: value }));
  const updateParameter = (name: string, bound: "minimum" | "maximum", value: string) =>
    setDraft((d) => ({
      ...d,
      parameters: { ...d.parameters, [name]: { ...d.parameters[name]!, [bound]: value } }
    }));
  const validate = () => {
    const value = validateExperimentDraft(draft);
    setErrors(value);
    return Object.keys(value).length === 0;
  };
  return { draft, errors, update, updateParameter, validate };
}
