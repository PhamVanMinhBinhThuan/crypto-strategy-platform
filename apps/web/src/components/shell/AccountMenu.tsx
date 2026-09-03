"use client";
import { useRouter } from "next/navigation";
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
      <button onClick={logout} aria-label="Sign out">
        {session?.email || "Account"} · Sign out
      </button>
    </div>
  );
}
