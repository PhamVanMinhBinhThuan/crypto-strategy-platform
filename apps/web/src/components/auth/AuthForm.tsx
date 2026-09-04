"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useRef, useState } from "react";
import { Button } from "@/src/components/ui/Button";
import { Field } from "@/src/components/ui/Field";
import { StatusPanel } from "@/src/components/ui/StatusPanel";
import { safeRedirect } from "@/src/foundation/auth/safe-redirect";
import { createSupabaseAuthClient } from "@/src/foundation/auth/supabase-auth-adapter";
import { authCopy } from "./auth-copy";
type Mode = "login" | "register" | "forgot" | "reset";
export function AuthForm({ mode, next }: { mode: Mode; next?: string }) {
  const router = useRouter(),
    client = createSupabaseAuthClient();
  const [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [confirm, setConfirm] = useState(""),
    [busy, setBusy] = useState(false),
    [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const formRef = useRef<HTMLFormElement>(null);
  useEffect(() => formRef.current?.setAttribute("data-ready", "true"), []);
  const needsEmail = mode !== "reset",
    needsPassword = mode !== "forgot",
    needsConfirm = mode === "register" || mode === "reset";

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    if (needsConfirm && password !== confirm) {
      setMessage({ kind: "error", text: "Passwords do not match." });
      return;
    }
    setBusy(true);
    setMessage(null);
    let result;
    const origin = window.location.origin;
    if (mode === "login") result = await client.signIn(email, password);
    else if (mode === "register")
      result = await client.signUp(email, password, `${origin}/auth/callback?next=/login`);
    else if (mode === "forgot")
      result = await client.requestPasswordReset(
        email,
        `${origin}/auth/callback?next=/reset-password`
      );
    else result = await client.updatePassword(password);
    setBusy(false);
    if (!result.ok) {
      setMessage({ kind: "error", text: result.message });
      return;
    }
    if (mode === "login") router.replace(safeRedirect(next));
    else if (mode === "register")
      setMessage({
        kind: "success",
        text: "Check your email to confirm your account before signing in."
      });
    else if (mode === "forgot")
      setMessage({
        kind: "success",
        text: "If an account can receive email, a recovery link has been sent."
      });
    else router.replace("/login?reset=success");
  }
  return (
    <form
      className="auth-form"
      aria-label="Authentication form"
      data-ready="false"
      ref={formRef}
      onSubmit={submit}
    >
      {needsEmail && (
        <Field
          label="Email address"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      )}{" "}
      {needsPassword && (
        <Field
          label={mode === "reset" ? "New password" : "Password"}
          name="password"
          type="password"
          minLength={8}
          autoComplete={mode === "login" ? "current-password" : "new-password"}
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      )}{" "}
      {needsConfirm && (
        <Field
          label="Confirm password"
          name="confirm"
          type="password"
          minLength={8}
          autoComplete="new-password"
          required
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
        />
      )}{" "}
      {message && (
        <StatusPanel
          title={message.kind === "error" ? "Request failed" : "Request received"}
          message={message.text}
          kind={message.kind}
        />
      )}
      <Button type="submit" disabled={busy}>
        {busy ? "Please wait…" : authCopy[mode][0]}
      </Button>
      <div className="auth-links">
        {mode === "login" && (
          <>
            <Link href="/forgot-password">Forgot password?</Link>
            <Link href="/register">Create account</Link>
          </>
        )}
        {mode === "register" && <Link href="/login">Already have an account?</Link>}
        {(mode === "forgot" || mode === "reset") && <Link href="/login">Back to sign in</Link>}
      </div>
    </form>
  );
}
