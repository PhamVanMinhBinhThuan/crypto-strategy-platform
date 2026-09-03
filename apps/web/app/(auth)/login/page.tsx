import { LoginForm } from "@/src/components/auth/LoginForm";
import { authCopy } from "@/src/components/auth/auth-copy";
import { AuthShell } from "@/src/components/auth/AuthShell";
export const metadata = { title: "Login - Crypto Strategy Lab" };
export default async function Page({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const { next } = await searchParams;
  return (
    <AuthShell title={authCopy.login[0]} subtitle={authCopy.login[1]}>
      <LoginForm next={next} />
    </AuthShell>
  );
}
