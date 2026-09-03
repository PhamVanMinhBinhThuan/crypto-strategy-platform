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
