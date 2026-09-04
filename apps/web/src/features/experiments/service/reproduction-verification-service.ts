import { z } from "zod";
import type { ApiClient, ApiResult } from "@/src/foundation/http/contracts";
import type { ReproductionVerification } from "../types/reproduction-verification";

const schema = z
  .object({
    verificationId: z.string().min(1),
    sourceExperimentId: z.string().min(1),
    reproductionExperimentId: z.string().min(1),
    status: z.enum(["PENDING", "RUNNING", "MATCHED", "MISMATCHED", "FAILED"]),
    tradesMatched: z.boolean().nullable(),
    metricsMatched: z.boolean().nullable(),
    fingerprintsMatched: z.boolean().nullable(),
    sourceEvidenceFingerprint: z.string().nullable(),
    reproductionEvidenceFingerprint: z.string().nullable(),
    differences: z.record(z.string(), z.unknown()),
    failure: z.object({ code: z.string(), message: z.string() }).strict().nullable(),
    startedAt: z.string().datetime().nullable(),
    finishedAt: z.string().datetime().nullable(),
    updatedAt: z.string().datetime()
  })
  .strict();

export const createReproductionVerificationService = (api: ApiClient) => ({
  async read(experimentId: string): Promise<ApiResult<ReproductionVerification>> {
    const result = await api.request(
      `/api/v1/experiments/${encodeURIComponent(experimentId)}/reproduction-verification`
    );
    if (!result.ok) return result;
    try {
      return { ...result, data: schema.parse(result.data) };
    } catch {
      return {
        ok: false,
        error: {
          code: "INVALID_RESPONSE",
          message: "The service returned an invalid reproduction verification.",
          retryable: false
        }
      };
    }
  }
});
