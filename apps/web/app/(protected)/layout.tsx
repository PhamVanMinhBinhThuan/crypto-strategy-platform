import { ApplicationShell } from "@/src/components/shell/ApplicationShell";
import { ClientProvider } from "@/src/foundation/composition/client-provider";
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <ClientProvider>
      <ApplicationShell>{children}</ApplicationShell>
    </ClientProvider>
  );
}
