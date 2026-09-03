import Link from "next/link";
import { AuthShell } from "@/src/components/auth/AuthShell";
import { StatusPanel } from "@/src/components/ui/StatusPanel";
export default async function Page({
  searchParams
}: {
  searchParams: Promise<{ state?: string }>;
}) {
  const { state } = await searchParams;
  return (
    <AuthShell
      title="Email link unavailable"
      subtitle="This link may be invalid, expired, or already used."
    >
      <StatusPanel
        title="Try again"
        message={
          state === "invalid"
            ? "Request a new confirmation or recovery email."
            : "Return to sign in."
        }
        kind="error"
      />
      <Link className="button link-button" href="/login">
        Back to sign in
      </Link>
    </AuthShell>
  );
}
