import { createServerClient } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";
const publicPaths = ["/login", "/register", "/forgot-password", "/reset-password", "/auth/"];
export async function proxy(request: NextRequest) {
  let response = NextResponse.next({ request });
  const bypassToken = process.env.PLAYWRIGHT_AUTH_BYPASS_TOKEN;
  const playwrightBypass =
    process.env.NODE_ENV !== "production" &&
    Boolean(bypassToken) &&
    request.headers.get("x-playwright-auth-bypass") === bypassToken;
  if (playwrightBypass) return response;
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL,
    key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  if (!url || !key)
    return publicPaths.some((p) => request.nextUrl.pathname.startsWith(p))
      ? response
      : NextResponse.redirect(new URL("/login", request.url));
  const supabase = createServerClient(url, key, {
    cookies: {
      getAll: () => request.cookies.getAll(),
      setAll(values) {
        values.forEach(({ name, value }) => request.cookies.set(name, value));
        response = NextResponse.next({ request });
        values.forEach(({ name, value, options }) => response.cookies.set(name, value, options));
      }
    }
  });
  const {
    data: { user }
  } = await supabase.auth.getUser();
  const isPublic = publicPaths.some((p) => request.nextUrl.pathname.startsWith(p));
  if (!user && !isPublic) {
    const login = new URL("/login", request.url);
    login.searchParams.set("next", `${request.nextUrl.pathname}${request.nextUrl.search}`);
    return NextResponse.redirect(login);
  }
  if (user && ["/login", "/register", "/forgot-password"].includes(request.nextUrl.pathname))
    return NextResponse.redirect(new URL("/market", request.url));
  return response;
}
export const config = { matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"] };
