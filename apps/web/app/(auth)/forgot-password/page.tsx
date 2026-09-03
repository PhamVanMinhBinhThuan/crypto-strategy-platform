import { ForgotPasswordForm } from "@/src/components/auth/ForgotPasswordForm";
import { authCopy } from "@/src/components/auth/auth-copy";
import { AuthShell } from "@/src/components/auth/AuthShell";
export default function Page() {
  return (
    <AuthShell title={authCopy.forgot[0]} subtitle={authCopy.forgot[1]}>
      <ForgotPasswordForm />
    </AuthShell>
  );
}
