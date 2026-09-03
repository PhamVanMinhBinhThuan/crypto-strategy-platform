# Crypto Strategy Lab Web

## F-013 fixture safety

Set `NEXT_PUBLIC_ENABLE_FIXTURES=true` only for local development or tests. Protected browser routes still require a real development Supabase Auth session; fixture mode is never an authentication bypass. Public API and WebSocket environment values must remain valid placeholders even in fixture mode. Production must keep `NEXT_PUBLIC_ENABLE_FIXTURES=false`, and the environment guard deliberately rejects a fixture-enabled production build.

Never place service-role, database, Redis, Worker, or provider credentials in `apps/web` or any `NEXT_PUBLIC_*` variable. Commit placeholders only; keep `.env.local` untracked.

Next.js foundation for authentication and the F-012/F-013 UI streams.

```powershell
Copy-Item .env.example .env.local
npm install
npm run check
npm run dev
```

Only `NEXT_PUBLIC_SUPABASE_URL` and the public anonymous/publishable key belong in the browser.
Never add a service-role key, database password, provider secret, access token, or refresh token.
Fixture mode is development/test-only and is disabled by default in production.
