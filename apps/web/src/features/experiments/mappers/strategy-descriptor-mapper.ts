import { z } from "zod";
const decimal = z.string().regex(/^-?\d+(?:\.\d+)?$/);
const parameter = z
  .object({
    name: z.string().min(1),
    type: z.enum(["INTEGER", "DECIMAL", "BOOLEAN", "TEXT", "ENUM"]),
    required: z.boolean(),
    defaultValue: z.string().nullable(),
    minimum: decimal.nullable(),
    maximum: decimal.nullable(),
    allowedValues: z.array(z.string()),
    description: z.string()
  })
  .strict();
const descriptor = z
  .object({
    strategyId: z.string().min(1),
    strategyVersionId: z.string().min(1),
    version: z.string().min(1),
    contractVersion: z.string().min(1),
    displayName: z.string().min(1),
    description: z.string(),
    category: z.string().min(1),
    supportedSignals: z.array(z.string()).min(1),
    requiredLookback: z.number().int().positive(),
    parameters: z.array(parameter),
    constraints: z.array(
      z.object({ lowerParameter: z.string().min(1), upperParameter: z.string().min(1) }).strict()
    ),
    descriptorFingerprint: z.string().min(1)
  })
  .strict();
const page = z.object({
  items: z.array(descriptor),
  nextCursor: z.string().nullable(),
  hasMore: z.boolean()
});
export type StrategyDescriptor = z.infer<typeof descriptor>;
export const mapStrategyDescriptors = (value: unknown) => page.parse(value);
export function validateDescriptorParameters(
  value: Record<string, string>,
  strategy: StrategyDescriptor
) {
  const errors: Record<string, string> = {};
  for (const rule of strategy.parameters) {
    const supplied = value[rule.name];
    if (rule.required && !supplied) {
      errors[rule.name] = "Required.";
      continue;
    }
    if (!supplied) continue;
    if (["INTEGER", "DECIMAL"].includes(rule.type)) {
      if (!/^-?\d+(?:\.\d+)?$/.test(supplied)) {
        errors[rule.name] = "Must be numeric.";
        continue;
      }
      if (rule.minimum !== null && Number(supplied) < Number(rule.minimum))
        errors[rule.name] = `Minimum ${rule.minimum}.`;
      if (rule.maximum !== null && Number(supplied) > Number(rule.maximum))
        errors[rule.name] = `Maximum ${rule.maximum}.`;
    }
    if (rule.allowedValues.length && !rule.allowedValues.includes(supplied))
      errors[rule.name] = "Choose an allowed value.";
  }
  for (const constraint of strategy.constraints) {
    const lower = value[constraint.lowerParameter],
      upper = value[constraint.upperParameter];
    if (lower && upper && Number(lower) >= Number(upper))
      errors[constraint.lowerParameter] = `Must be less than ${constraint.upperParameter}.`;
  }
  return errors;
}
