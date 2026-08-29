# Quickstart: Validate Java Backend Foundation

## 1. Prerequisites

- JDK 21 available through `JAVA_HOME` or toolchain discovery.
- Network access for the first Gradle Wrapper dependency download.
- No Docker, Redis, database or provider is required for the default build.

Check context:

```bash
git branch --show-current
java -version
```

## 2. Build and test offline foundation

```bash
./gradlew clean check
./gradlew projects
```

Expected: both apps, all 13 capability modules and `architecture-tests` participate;
unit/security/architecture tests pass without external services.

## 3. Run API foundation

Copy variable names from `.env.example` into a local untracked environment. Use real
values only in the shell/secret store, never in committed files.

```bash
./gradlew :apps:api:bootRun
```

From another terminal:

```bash
curl --fail http://localhost:8080/actuator/health/liveness
curl --fail http://localhost:8080/actuator/health/readiness
```

Liveness must not depend on Supabase. Readiness is `UP` only when the configured database
is reachable.

## 4. Run idle Worker foundation

```bash
./gradlew :apps:worker:bootRun
curl --fail http://localhost:8081/actuator/health/liveness
curl --fail http://localhost:8081/actuator/health/readiness
```

Worker must start without Redis and must not consume any job in F-002.

## 5. Verify authentication offline

```bash
./gradlew :apps:api:test --tests '*AuthenticationIntegrationTest'
```

The test-only controller covers missing, malformed, expired, wrong signature,
issuer/audience and valid token cases. No public endpoint is added.

## 6. Verify shared-development database connectivity

After loading server-side JDBC variables into the local environment:

```bash
./gradlew supabaseIntegrationTest
```

Expected: API and Worker connection/readiness verification passes using connection-level
operations only. Evidence confirms no business-schema/table statement was executed and
output contains no connection URL/password.

## 7. Completion evidence

Record non-secret results for:

- exact Git commit, environment name and non-secret configuration profile;
- JDK, wrapper and dependency versions;
- root `clean check` duration/result;
- project list and architecture positive/negative fixtures;
- API/Worker startup and health status;
- authentication matrix result;
- shared-development readiness result;
- repository/log secret scan.

Keep each item `Planned` until the real command/review has produced inspectable output;
only then mark it `Verified`. Never invent benchmark, log or demo evidence.
