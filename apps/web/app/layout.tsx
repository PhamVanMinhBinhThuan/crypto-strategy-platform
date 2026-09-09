import type { Metadata } from "next";
import { JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { SessionProvider } from "@/src/foundation/auth/SessionProvider";

const jetBrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-jetbrains-mono",
  display: "swap"
});

export const metadata: Metadata = {
  title: "Crypto Strategy Lab",
  description: "Reproducible crypto strategy research"
};
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className={jetBrainsMono.variable}>
        <SessionProvider>{children}</SessionProvider>
      </body>
    </html>
  );
}
