import { NextResponse } from "next/server";
import { createSupabaseServerClient } from "@/src/foundation/auth/supabase-server";
import { safeRedirect } from "@/src/foundation/auth/safe-redirect";
export async function GET(request: Request) {
  const url = new URL(request.url),
    code = url.searchParams.get("code"),
    next = safeRedirect(url.searchParams.get("next"));
  if (!code) return NextResponse.redirect(new URL(`/auth-status?state=invalid`, url.origin));
  const { error } = await (await createSupabaseServerClient()).auth.exchangeCodeForSession(code);
  return NextResponse.redirect(new URL(error ? `/auth-status?state=invalid` : next, url.origin));
}
