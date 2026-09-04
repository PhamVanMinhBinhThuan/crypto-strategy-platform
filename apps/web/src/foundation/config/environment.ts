import { z } from "zod";

const publicEnvironmentSchema = z.object({
  NEXT_PUBLIC_SUPABASE_URL: z.string().url(),
  NEXT_PUBLIC_SUPABASE_ANON_KEY: z.string().min(16),
  NEXT_PUBLIC_API_BASE_URL: z.string().url(),
  NEXT_PUBLIC_WS_URL: z
    .string()
    .url()
    .refine((value) => /^wss?:/.test(value), "Must be ws/wss"),
  NEXT_PUBLIC_ENABLE_FIXTURES: z.enum(["true", "false"]).default("false")
});

const forbidden = ["SUPABASE_SERVICE_ROLE_KEY", "DATABASE_URL", "PGPASSWORD"] as const;

export type PublicEnvironment = {
  supabaseUrl: string;
  supabaseAnonKey: string;
  apiBaseUrl: string;
  websocketUrl: string;
  fixturesEnabled: boolean;
};

export function parsePublicEnvironment(
  source: Record<string, string | undefined>,
  production = process.env.NODE_ENV === "production"
): PublicEnvironment {
  for (const name of forbidden) {
    if (source[name])
      throw new Error(`Privileged environment variable is forbidden in apps/web: ${name}`);
  }
  const value = publicEnvironmentSchema.parse(source);
  const fixturesEnabled = value.NEXT_PUBLIC_ENABLE_FIXTURES === "true";
  if (production && fixturesEnabled)
    throw new Error("Fixture mode cannot be enabled in production");
  return {
    supabaseUrl: value.NEXT_PUBLIC_SUPABASE_URL,
    supabaseAnonKey: value.NEXT_PUBLIC_SUPABASE_ANON_KEY,
    apiBaseUrl: value.NEXT_PUBLIC_API_BASE_URL.replace(/\/$/, "").replace(/\/api\/v1$/, ""),
    websocketUrl: value.NEXT_PUBLIC_WS_URL,
    fixturesEnabled
  };
}

export function getPublicEnvironment(): PublicEnvironment {
  return parsePublicEnvironment({
    NEXT_PUBLIC_SUPABASE_URL: process.env.NEXT_PUBLIC_SUPABASE_URL,
    NEXT_PUBLIC_SUPABASE_ANON_KEY: process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY,
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
    NEXT_PUBLIC_WS_URL: process.env.NEXT_PUBLIC_WS_URL,
    NEXT_PUBLIC_ENABLE_FIXTURES: process.env.NEXT_PUBLIC_ENABLE_FIXTURES
  });
}
