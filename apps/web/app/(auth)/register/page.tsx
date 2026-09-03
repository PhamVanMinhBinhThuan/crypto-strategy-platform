import { RegisterForm } from "@/src/components/auth/RegisterForm";
import { authCopy } from "@/src/components/auth/auth-copy";
import { AuthShell } from "@/src/components/auth/AuthShell";
export const metadata = { title: "Register - Crypto Strategy Lab" };
export default function Page() {
  return (
    <AuthShell title={authCopy.register[0]} subtitle={authCopy.register[1]}>
      <RegisterForm />
    </AuthShell>
  );
}
