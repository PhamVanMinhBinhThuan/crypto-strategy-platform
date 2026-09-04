export type StrategySelectionDraft = Readonly<{
  strategyId: string;
  version: string;
  parameters: Readonly<Record<string, string | number | boolean>>;
}>;
export type StrategySourceDraft =
  | Readonly<{ type: "SINGLE"; strategy: StrategySelectionDraft }>
  | Readonly<{
      type: "COMPOSITE";
      policyId: string;
      policyVersion: string;
      policyParameters: Readonly<Record<string, string | number | boolean>>;
      components: readonly StrategySelectionDraft[];
    }>;
export type StrategyDraft = Readonly<{
  name: string;
  description: string;
  kind: "SINGLE" | "COMPOSITE";
  source: StrategySourceDraft;
  expectedLatestVersionNo?: number;
}>;
