import type { z } from "zod";
import type {
  strategyDescriptorSchema,
  userStrategySchema,
  userStrategySummarySchema,
  userStrategyVersionSchema
} from "../api/schemas";
export type StrategyDescriptor = z.infer<typeof strategyDescriptorSchema>;
export type UserStrategy = z.infer<typeof userStrategySchema>;
export type UserStrategySummary = z.infer<typeof userStrategySummarySchema>;
export type UserStrategyVersion = z.infer<typeof userStrategyVersionSchema>;
