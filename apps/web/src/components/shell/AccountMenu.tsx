"use client";
import { useRouter } from "next/navigation";
import { LogOut, UserRound } from "lucide-react";
import { clearPrivateClientState } from "@/src/foundation/auth/logout";
import { useSession } from "@/src/foundation/auth/SessionProvider";
export function AccountMenu() {
  const { session, client } = useSession(),
    router = useRouter();
  async function logout() {
    await clearPrivateClientState();
    await client.signOut();
    router.replace("/login");
    router.refresh();
  }
  return (
    <div className="account">
      <button type="button" onClick={logout} aria-label="Sign out" title="Sign out">
        <span className="account-avatar" aria-hidden="true">
          <UserRound size={17} />
        </span>
        <span className="account-copy">
          <small>{session?.email || "Account"}</small>
          <strong>Sign out</strong>
        </span>
        <LogOut className="account-logout-icon" size={17} aria-hidden="true" />
      </button>
    </div>
  );
}
