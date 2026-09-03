import { z } from "zod";
import { decimalStringSchema, paginationSchema } from "../../shared/public-contract";
export const parameterDescriptorSchema = z
  .object({
    name: z.string().min(1),
    type: z.enum(["INTEGER", "DECIMAL", "BOOLEAN", "TEXT", "ENUM"]),
    required: z.boolean(),
    defaultValue: z.string().nullable(),
    minimum: decimalStringSchema.nullable(),
    maximum: decimalStringSchema.nullable(),
    allowedValues: z.array(z.string()),
    description: z.string()
  })
  .strict();
export const strategyDescriptorSchema = z
  .object({
    strategyId: z.string().min(1),
    strategyVersionId: z.string().min(1),
    version: z.string().min(1),
    contractVersion: z.string().min(1),
    displayName: z.string().min(1),
    description: z.string(),
    category: z.string().min(1),
    supportedSignals: z.array(z.enum(["BUY", "SELL", "HOLD"])).min(1),
    requiredLookback: z.number().int().min(1),
    parameters: z.array(parameterDescriptorSchema),
    constraints: z.array(
      z.object({ lowerParameter: z.string().min(1), upperParameter: z.string().min(1) }).strict()
    ),
    descriptorFingerprint: z.string().min(1)
  })
  .strict();
export const strategyPageSchema = paginationSchema.extend({
  items: z.array(strategyDescriptorSchema)
});
const canonicalParameters = z.record(z.string(), z.string());
const selectionResponse = z
  .object({
    strategyId: z.string().min(1),
    strategyVersionId: z.string().min(1),
    version: z.string().min(1),
    parameters: canonicalParameters
  })
  .strict();
export const strategySourceSchema = z.discriminatedUnion("type", [
  z.object({ type: z.literal("SINGLE"), strategy: selectionResponse }).strict(),
  z
    .object({
      type: z.literal("COMPOSITE"),
      policyId: z.string().min(1),
      policyVersion: z.string().min(1),
      policyParameters: canonicalParameters,
      components: z.array(selectionResponse).min(2)
    })
    .strict()
]);
export const userStrategyVersionSchema = z
  .object({
    userStrategyVersionId: z.string().min(1),
    userStrategyId: z.string().min(1),
    versionNo: z.number().int().min(1),
    kind: z.enum(["SINGLE", "COMPOSITE"]),
    source: strategySourceSchema,
    status: z.enum(["DRAFT", "PUBLISHED"]),
    fingerprint: z.string().min(1),
    publishedAt: z.iso.datetime({ offset: true }).nullable(),
    createdAt: z.iso.datetime({ offset: true })
  })
  .strict();
export const userStrategySchema = z
  .object({
    userStrategyId: z.string().min(1),
    kind: z.enum(["SINGLE", "COMPOSITE"]),
    name: z.string().min(1),
    description: z.string(),
    status: z.enum(["ACTIVE", "ARCHIVED"]),
    archivedAt: z.iso.datetime({ offset: true }).nullable(),
    createdAt: z.iso.datetime({ offset: true }),
    updatedAt: z.iso.datetime({ offset: true }),
    latestVersion: userStrategyVersionSchema
  })
  .strict();
export const userStrategySummarySchema = z
  .object({
    userStrategyId: z.string().min(1),
    kind: z.enum(["SINGLE", "COMPOSITE"]),
    name: z.string().min(1),
    description: z.string(),
    createdAt: z.iso.datetime({ offset: true })
  })
  .strict();
export const userStrategyPageSchema = paginationSchema.extend({
  items: z.array(userStrategySummarySchema)
});
