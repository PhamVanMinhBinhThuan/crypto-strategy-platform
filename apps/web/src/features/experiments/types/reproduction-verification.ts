export type ReproductionVerification = Readonly<{
  verificationId: string;
  sourceExperimentId: string;
  reproductionExperimentId: string;
  status: "PENDING" | "RUNNING" | "MATCHED" | "MISMATCHED" | "FAILED";
  tradesMatched: boolean | null;
  metricsMatched: boolean | null;
  fingerprintsMatched: boolean | null;
  sourceEvidenceFingerprint: string | null;
  reproductionEvidenceFingerprint: string | null;
  differences: Readonly<Record<string, unknown>>;
  failure: Readonly<{ code: string; message: string }> | null;
  startedAt: string | null;
  finishedAt: string | null;
  updatedAt: string;
}>;

export type ReproductionVerificationState =
  | { status: "idle" | "loading" }
  | { status: "success"; snapshot: ReproductionVerification }
  | { status: "error"; message: string };
