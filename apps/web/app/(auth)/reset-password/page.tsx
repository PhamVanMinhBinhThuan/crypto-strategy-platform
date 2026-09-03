import { ResetPasswordForm } from "@/src/components/auth/ResetPasswordForm";
import { authCopy } from "@/src/components/auth/auth-copy";
import { AuthShell } from "@/src/components/auth/AuthShell";
export default function Page() {
  return (
    <AuthShell title={authCopy.reset[0]} subtitle={authCopy.reset[1]}>
      <ResetPasswordForm />
    </AuthShell>
  );
}
