const DEFAULT_ROUTE = "/market";
export function safeRedirect(value: string | null | undefined): string {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.startsWith("/login"))
    return DEFAULT_ROUTE;
  try {
    const parsed = new URL(value, "https://local.invalid");
    return parsed.origin === "https://local.invalid"
      ? `${parsed.pathname}${parsed.search}${parsed.hash}`
      : DEFAULT_ROUTE;
  } catch {
    return DEFAULT_ROUTE;
  }
}
