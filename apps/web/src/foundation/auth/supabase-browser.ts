import { createBrowserClient } from "@supabase/ssr";
import { getPublicEnvironment } from "@/src/foundation/config/environment";
export function createSupabaseBrowserClient() {
  const env = getPublicEnvironment();
  return createBrowserClient(env.supabaseUrl, env.supabaseAnonKey);
}
