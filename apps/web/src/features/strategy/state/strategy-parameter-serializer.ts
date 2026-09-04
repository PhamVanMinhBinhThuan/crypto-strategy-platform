import type { StrategyDescriptor } from "../model/strategy";

export function serializeStrategyParameters(
  descriptor: StrategyDescriptor,
  values: Readonly<Record<string, string>>
): Readonly<Record<string, string | number | boolean>> {
  return Object.fromEntries(
    descriptor.parameters.map((field) => {
      const value = values[field.name] ?? field.defaultValue ?? "";
      if (field.type === "INTEGER") return [field.name, Number(value)];
      if (field.type === "BOOLEAN") return [field.name, value === "true"];
      return [field.name, value];
    })
  );
}
