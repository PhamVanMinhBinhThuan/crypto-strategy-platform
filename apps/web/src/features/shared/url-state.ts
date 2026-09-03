export function canonicalEnum<T extends string>(
  value: string | null,
  allowed: readonly T[],
  fallback: T
): T {
  return value && allowed.some((item) => item === value) ? (value as T) : fallback;
}

export function canonicalEnumList<T extends string>(
  values: readonly string[],
  allowed: readonly T[],
  fallback: readonly T[],
  limit: number
): T[] {
  const unique = values.filter((value, index) => values.indexOf(value) === index);
  const valid = unique.filter((value): value is T => allowed.some((item) => item === value));
  return (valid.length ? valid : [...fallback]).slice(0, limit);
}
