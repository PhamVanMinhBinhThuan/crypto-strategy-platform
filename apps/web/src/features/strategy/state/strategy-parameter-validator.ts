import type { StrategyDescriptor } from "../model/strategy";
export type ValidationIssues = Readonly<Record<string, string>>;
const decimal = /^-?(0|[1-9][0-9]*)(\.[0-9]+)?$/;
function decimalParts(value: string, places: number) {
  const negative = value.startsWith("-");
  const [whole, fraction = ""] = value.replace("-", "").split(".");
  const result =
    BigInt(whole) * 10n ** BigInt(places) + BigInt(fraction.padEnd(places, "0") || "0");
  return negative ? -result : result;
}
function compareDecimal(a: string, b: string) {
  const places = Math.max(a.split(".")[1]?.length ?? 0, b.split(".")[1]?.length ?? 0);
  const left = decimalParts(a, places),
    right = decimalParts(b, places);
  return left < right ? -1 : left > right ? 1 : 0;
}
export function validateStrategyParameters(
  descriptor: StrategyDescriptor,
  values: Readonly<Record<string, string>>
): ValidationIssues {
  const issues: Record<string, string> = {};
  for (const field of descriptor.parameters) {
    const value = values[field.name] ?? "";
    if (field.required && !value) {
      issues[field.name] = "Bắt buộc.";
      continue;
    }
    if (!value) continue;
    if (["INTEGER", "DECIMAL"].includes(field.type) && !decimal.test(value))
      issues[field.name] = "Giá trị số không hợp lệ.";
    else if (field.type === "INTEGER" && !/^-?(0|[1-9][0-9]*)$/.test(value))
      issues[field.name] = "Phải là số nguyên.";
    else if (field.type === "BOOLEAN" && !["true", "false"].includes(value))
      issues[field.name] = "Phải là true hoặc false.";
    else if (field.type === "ENUM" && !field.allowedValues.includes(value))
      issues[field.name] = "Giá trị không được hỗ trợ.";
    else if (field.minimum && compareDecimal(value, field.minimum) < 0)
      issues[field.name] = `Tối thiểu ${field.minimum}.`;
    else if (field.maximum && compareDecimal(value, field.maximum) > 0)
      issues[field.name] = `Tối đa ${field.maximum}.`;
  }
  for (const rule of descriptor.constraints) {
    const lower = values[rule.lowerParameter],
      upper = values[rule.upperParameter];
    if (
      lower &&
      upper &&
      decimal.test(lower) &&
      decimal.test(upper) &&
      compareDecimal(lower, upper) >= 0
    )
      issues[rule.upperParameter] = `Phải lớn hơn ${rule.lowerParameter}.`;
  }
  return issues;
}
