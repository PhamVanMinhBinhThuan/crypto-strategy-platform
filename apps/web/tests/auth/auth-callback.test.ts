import { describe, expect, it, vi, beforeEach } from "vitest";
import { GET } from "@/app/auth/callback/route";

const mockExchangeCodeForSession = vi.fn();

vi.mock("@/src/foundation/auth/supabase-server", () => ({
  createSupabaseServerClient: async () => ({
    auth: {
      exchangeCodeForSession: mockExchangeCodeForSession
    }
  })
}));

describe("Auth Callback Route", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("redirects to invalid state if no code is provided", async () => {
    const request = new Request("http://localhost/auth/callback");
    const response = await GET(request);

    expect(response.status).toBe(307);
    expect(response.headers.get("Location")).toBe("http://localhost/auth-status?state=invalid");
  });

  it("exchanges code and redirects to the safe next URL on success", async () => {
    mockExchangeCodeForSession.mockResolvedValueOnce({ error: null });
    const request = new Request("http://localhost/auth/callback?code=validcode&next=/settings");
    const response = await GET(request);

    expect(mockExchangeCodeForSession).toHaveBeenCalledWith("validcode");
    expect(response.status).toBe(307);
    expect(response.headers.get("Location")).toBe("http://localhost/settings");
  });

  it("defaults to /market if next is not provided", async () => {
    mockExchangeCodeForSession.mockResolvedValueOnce({ error: null });
    const request = new Request("http://localhost/auth/callback?code=validcode");
    const response = await GET(request);

    expect(response.status).toBe(307);
    expect(response.headers.get("Location")).toBe("http://localhost/market");
  });

  it("redirects to invalid state if code exchange fails (e.g., expired or tampered)", async () => {
    mockExchangeCodeForSession.mockResolvedValueOnce({ error: { message: "Invalid code" } });
    const request = new Request("http://localhost/auth/callback?code=expiredcode&next=/settings");
    const response = await GET(request);

    expect(response.status).toBe(307);
    expect(response.headers.get("Location")).toBe("http://localhost/auth-status?state=invalid");
  });
});
