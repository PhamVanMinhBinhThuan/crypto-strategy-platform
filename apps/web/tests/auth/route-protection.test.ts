import { describe, expect, it, vi, beforeEach } from "vitest";
import { NextRequest } from "next/server";

vi.mock("next/server", async () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const actual = await vi.importActual<any>("next/server");
  return {
    ...actual,
    NextResponse: {
      next: vi.fn(() => ({ status: 200, cookies: { set: vi.fn() } })),
      redirect: vi.fn((url: string | URL) => {
        const res = new actual.NextResponse(null, { status: 302 });
        res.headers.set("Location", url.toString());
        return res;
      })
    }
  };
});

import { proxy } from "@/proxy";

vi.mock("@supabase/ssr", () => ({
  createServerClient: vi.fn(() => ({
    auth: {
      getUser: vi.fn().mockImplementation(async () => {
        // Mock implementation will be overridden in tests
        return { data: { user: null } };
      })
    }
  }))
}));

import { createServerClient } from "@supabase/ssr";

describe("Route Protection (proxy)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    process.env.NEXT_PUBLIC_SUPABASE_URL = "http://localhost";
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY = "anon-key";
  });

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const mockGetUser = (user: any) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (createServerClient as any).mockImplementation(() => ({
      auth: { getUser: async () => ({ data: { user } }) }
    }));
  };

  it("redirects unauthenticated users from protected routes to login", async () => {
    mockGetUser(null);
    const req = new NextRequest("http://localhost/market", { headers: new Headers() });
    const res = await proxy(req);

    expect(res.status).toBe(302);
    expect(res.headers.get("Location")).toBe("http://localhost/login?next=%2Fmarket");
  });

  it("allows unauthenticated users to access public routes", async () => {
    mockGetUser(null);
    const req = new NextRequest("http://localhost/login", { headers: new Headers() });
    const res = await proxy(req);

    expect(res.status).toBe(200); // NextResponse.next()
  });

  it("allows authenticated users to access protected routes", async () => {
    mockGetUser({ id: "user-123" });
    const req = new NextRequest("http://localhost/market", { headers: new Headers() });
    const res = await proxy(req);

    expect(res.status).toBe(200);
  });

  it("redirects authenticated users away from auth pages", async () => {
    mockGetUser({ id: "user-123" });
    const req = new NextRequest("http://localhost/login", { headers: new Headers() });
    const res = await proxy(req);

    expect(res.status).toBe(302);
    expect(res.headers.get("Location")).toBe("http://localhost/market");
  });

  it("redirects to login if env vars are missing and trying to access protected route", async () => {
    delete process.env.NEXT_PUBLIC_SUPABASE_URL;
    const req = new NextRequest("http://localhost/market", { headers: new Headers() });
    const res = await proxy(req);

    expect(res.status).toBe(302);
    expect(res.headers.get("Location")).toBe("http://localhost/login");
  });
});
