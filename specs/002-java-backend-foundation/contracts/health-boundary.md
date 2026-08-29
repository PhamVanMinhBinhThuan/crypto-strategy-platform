# Health Boundary Contract

## API and Worker endpoints

| Endpoint | Purpose | External dependency |
|---|---|---|
| `/actuator/health/liveness` | Process/application can continue running | None |
| `/actuator/health/readiness` | Runtime can safely receive work/traffic | Database connection health |

Only health endpoints are exposed by default. Environment/config/beans/loggers and other
management endpoints are not public.

## Behavior

- Missing mandatory configuration: fail startup with configuration key name, never value.
- Configured but database unreachable: liveness remains `UP`; readiness is not `UP`.
- Database available: readiness is `UP` after a connection-level read-only health check.
- Health operation does not access business schema/table or create migration history.
- Response details do not contain JDBC URL, username, exception stack or credentials.
- Worker is `IDLE` in F-002 and has no Redis/queue health indicator.

## Remote verification

`supabaseIntegrationTest` uses environment-provided server-side JDBC settings and invokes
both runtimes' readiness behavior through connection-level operations only. Verification
captures reviewable evidence that no business schema/table statement was executed and
records commit, environment/configuration identity, status and timing without credentials.
It does not print the connection string and is not part of default offline `check`.
