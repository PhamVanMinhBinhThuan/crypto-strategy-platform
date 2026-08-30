# Quickstart Validation: Strategy Registry and User Strategy Library

This guide describes validation expected after F-004 implementation. It does not authorize remote database mutation, public endpoint work, or changes to ADR status without team review.

## 1. Prerequisites

- JDK 21
- Repository Gradle Wrapper
- F-002 foundation and F-003 Market Data/Dataset code present
- For PostgreSQL integration only: Docker-compatible runtime and local Supabase CLI
- No Binance key, Redis, browser, Search, Job, Backtest engine, or remote Supabase access is required

Confirm branch and feature pointer:

```powershell
git branch --show-current
Get-Content .specify/feature.json
git status --short
```

Expected feature directory: `specs/004-user-strategy-library`.

## 2. Confirm governance gate

Before merging dependent implementation, review:

```powershell
Select-String -Path docs/adr/0001-modular-monolith.md,docs/adr/0002-module-boundaries.md,docs/adr/0005-strategy-plugin-registry.md,docs/adr/0009-reproducible-experiments.md,docs/adr/0011-supabase-auth-user-ownership.md,docs/adr/0012-user-strategy-job-ownership.md -Pattern '^\*\*Status\*\*'
```

ADR-0011 is already `Accepted`. The other applicable ADRs must be accepted or superseded through team review before implementation merge. Planning and local implementation verification do not authorize changing those statuses automatically.

## 3. Run default offline verification

PowerShell:

```powershell
.\gradlew.bat clean check
```

Expected evidence:

- all existing F-002/F-003 tests remain green;
- Strategy, registry, parameter, MA, Composite, User Strategy, and architecture tests pass;
- no live Binance, database, Redis, remote Supabase, or browser dependency is contacted;
- no Strategy logic imports Spring, JDBC, provider, persistence implementation, clock, Job, Search, Backtest, Evaluation, Leaderboard, or UI code.

## 4. Run focused deterministic suites

```powershell
.\gradlew.bat :modules:strategy-core:test
.\gradlew.bat :modules:strategies:test
.\gradlew.bat :modules:combination:test
.\gradlew.bat :modules:persistence:test
.\gradlew.bat :apps:api:test
.\gradlew.bat :architecture-tests:test
```

Expected evidence:

- 100 identical evaluations produce identical decisions/evidence;
- defaults become complete canonical parameters;
- invalid and cross-field parameters are rejected;
- duplicate registry keys fail deterministically;
- short lookback returns `INSUFFICIENT_DATA`, never `HOLD`;
- majority vote returns the unique maximum and HOLD for ties;
- all component permutations produce the same majority decision/fingerprint;
- two-user tests deny every cross-owner private operation;
- published snapshots remain immutable after rename/version/archive;
- test-only F-003 batches produce bounded rolling contexts without a production Strategy-to-Market-Data dependency.

## 5. Verify QA-01 extension boundary

Run the test-only MACD extension scenario:

```powershell
.\gradlew.bat :modules:strategy-core:test :modules:strategies:test :architecture-tests:test --tests "*StrategyExtension*"
```

Then inspect the change surface relative to the branch base selected by the team:

```powershell
git diff --name-only origin/main...HEAD
```

Expected: the MACD proof uses test fixture implementation, descriptor/schema, registry input, and tests. It does not require production edits to Backtester, Evaluator, Leaderboard, Search, UI, or F-003 public contracts.

## 6. Optional local PostgreSQL verification

This section is environment-dependent and must use local Supabase only.

Check local status:

```powershell
npx supabase status
```

If Docker/local Supabase is intentionally available, reset only the local development database so repository migrations are applied:

```powershell
npx supabase db reset --local
```

Configure the future `strategyIntegrationTest` task with the local database URL/credentials, then run:

```powershell
.\gradlew.bat :modules:persistence:strategyIntegrationTest
```

Expected evidence:

- catalog register-or-verify is idempotent and detects conflicting content;
- first draft creation is atomic;
- owner predicates hide cross-user rows;
- published versions/components reject update/delete;
- archive preserves published resolution;
- concurrent next-version/publication permits exactly one success and returns conflict to stale requests;
- rollback leaves no partial root/version/component.

Do not run `supabase db push` from this guide. No F-004 migration or remote apply is planned.

## 7. Static scope and migration checks

Confirm existing migrations were not edited:

```powershell
git diff --exit-code origin/main -- supabase/migrations/20260827000100_create_database_baseline.sql supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql
```

Check excluded scope did not enter planned F-004 source areas:

```powershell
rg -n "ServiceLoader|ClassLoader|ScriptEngine|prompt|ExecutionAttempt|SearchGenerator|BacktestEngine|@RestController|WebSocket" modules/strategy-core modules/strategies modules/combination
```

Any match must be reviewed; package documentation or a negative architecture-test fixture may be intentional, but production implementation of excluded behavior is not.

## 8. Final repository checks

```powershell
git diff --check
git diff --stat
git status --short
```

Evidence must remain `Planned` until the corresponding command runs successfully on a recorded commit/environment. Do not create benchmark, log, SQL, or demo results that were not actually collected.

## 9. Reviewer and QA-01 gates

On 2026-08-30, the repository owner reviewed the F-004 remediation decisions, approved the aligned specification/plan/contracts/tasks, and explicitly authorized implementation on `feature/004-strategy-registry`. This satisfies the pre-implementation artifact review gate; it does not accept any ADR and does not authorize merge or remote database mutation.

For QA-01, add MACD only under the test fixture and extension-test paths, then inspect the change surface:

```powershell
git diff --name-only origin/main...HEAD
```

Allowed MACD proof paths are `architecture-tests/.../strategyextension/`, `StrategyExtensionTest.java`, the Strategy extension contract, and verification evidence. Production Backtester, Evaluator, Leaderboard, Search, UI, and F-003 public contracts must have no MACD-specific change.
