import type { Metadata } from "next";
import "./globals.css";
import { SessionProvider } from "@/src/foundation/auth/SessionProvider";
export const metadata: Metadata = {
  title: "Crypto Strategy Lab",
  description: "Reproducible crypto strategy research"
};
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <SessionProvider>{children}</SessionProvider>
      </body>
    </html>
  );
}
