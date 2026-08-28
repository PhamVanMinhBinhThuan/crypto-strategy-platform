# Authentication Boundary Contract

## Runtime inputs

| Configuration | Secret | Rule |
|---|---|---|
| `SUPABASE_JWT_ISSUER` | No | Exact `iss` expected from Supabase Auth |
| `SUPABASE_JWT_JWKS_URI` | No | HTTPS JWKS endpoint for signature verification |
| `SUPABASE_JWT_AUDIENCE` | No | Required audience expected in `aud` |

Database password or service-role key is not part of JWT validation.

## Request processing

1. Only `Authorization: Bearer <token>` is accepted for a protected request.
2. Signature algorithm/key is resolved from configured JWKS.
3. `exp`, `nbf`, exact issuer and required audience are validated.
4. `sub` must exist and parse as UUID.
5. Success creates `AuthenticatedUserContext(userId)`.
6. Failure stops before the protected handler and returns the existing safe error envelope.

## Verification matrix

| Case | Expected |
|---|---|
| Missing header | Unauthorized; handler not invoked |
| Malformed scheme/token | Unauthorized; no token echo |
| Unknown key or invalid signature | Unauthorized |
| Expired/not-yet-valid | Unauthorized |
| Wrong issuer | Unauthorized |
| Missing/wrong audience | Unauthorized |
| Missing/non-UUID subject | Unauthorized |
| Valid token | Handler receives exact UUID context |

Tests use a local signing key and test-only controller. F-002 MUST NOT add a public `/me`
or business endpoint and MUST NOT depend on live Supabase Auth during default `check`.
