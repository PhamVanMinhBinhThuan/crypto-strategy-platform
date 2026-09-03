# Crypto Strategy Lab Web

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
