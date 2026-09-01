# Quickstart: Validate F-006 Design and Implementation

This guide defines evidence to collect after implementation. A command is not evidence until its real output, commit and environment are recorded. Remote Supabase migration apply is outside F-006.

## 1. Prerequisites

- Branch: `feature/006-backtest-evaluation-leaderboard`.
- Java 21 selected for Gradle.
- ADR-0013 reviewed and `Accepted` before dependent implementation is merged.
- F-003, F-004 and F-005 focused tests passing.
- For database tests, use an isolated local PostgreSQL/Supabase environment. Do not target shared development or production.

Confirm:

```powershell
git branch --show-current
java -version
.\gradlew.bat --version
```

## 2. Review gates

Verify there is no implementation outside scope:

```powershell
rg -n "RedisStream|WebSocket|Controller|SearchGenerator|dead.?letter" modules/backtesting modules/evaluation modules/leaderboard
```

Review these artifacts before code verification:

- [Specification](spec.md)
- [Plan](plan.md)
- [Data model](data-model.md)
- [ADR-0013](../../docs/adr/0013-backtest-execution-integration.md)
- [Contracts](contracts/)

## 3. Focused unit and contract tests

```powershell
.\gradlew.bat :modules:backtesting:test :modules:evaluation:test :modules:leaderboard:test --no-daemon --max-workers=1 --no-configuration-cache
```

Expected outcomes:

- next-open execution, two-sided fee/slippage and forced close pass;
- batch progression and parameter-aware lookback pass;
- all four metrics, normalization and five-Trade eligibility pass;
- Top-10 and deterministic tie-break pass;
- invalid Dataset/lineage paths create no successful outcome.

## 4. Reproduction verification

Run the dedicated reproduction suite defined during tasks:

```powershell
.\gradlew.bat :modules:backtesting:test :modules:evaluation:test :modules:leaderboard:test --tests "*Reproduction*" --no-daemon --max-workers=1 --no-configuration-cache
```

The fixture must run the same frozen input with at least two different batch sizes and prove identical:

- ordered Trade sequence and decimal values;
- Total Return, Win Rate, Maximum Drawdown and Number of Trades;
- `backtest-v1`, `evaluation-v1` and applicable `leaderboard-v1` fingerprints.

Changing one frozen assumption/version must change the appropriate fingerprint.

## 5. Architecture tests

Run only after ADR-0013 is accepted and the dependency matrix is updated consistently:

```powershell
.\gradlew.bat :architecture-tests:test --no-daemon --max-workers=1 --no-configuration-cache
```

Expected outcomes:

- allowed Gradle dependencies match ADR-0013;
- no capability imports another capability's `internal` package;
- Backtest/Evaluation/Leaderboard policy imports no Spring, JDBC, Redis, Binance, transport or UI classes;
- persistence accesses only public owner contracts;
- no dependency cycle exists.

## 6. Database verification

Inspect the proposed migration without applying it remotely:

```powershell
npx supabase db push --dry-run
```

The command must list only the expected new forward migration after all existing migrations. Stop if it targets a shared project or lists an unexpected file. Do not run `npx supabase db push` as part of F-006.

When an isolated local database is available:

```powershell
npx supabase db reset --local
npx supabase test db
```

If Docker/local Supabase is unavailable, keep database evidence `Planned`; do not substitute a shared database or claim verification.

For JDBC integration tests, set credentials only in the current PowerShell session, confirm the project/database is isolated, then run the source set created by tasks:

```powershell
.\gradlew.bat :modules:persistence:backtestEvaluationLeaderboardIntegrationTest --no-daemon --max-workers=1 --no-configuration-cache
```

Expected database rejections include wrong/non-successful Attempt, cross-Experiment links, immutable artifact update/delete and duplicate outcome/version/revision.

## 7. Full regression

```powershell
.\gradlew.bat clean check --no-daemon --max-workers=1 --no-configuration-cache
git diff --check
git status --short
```

Review that changes remain limited to F-006 modules, necessary F-004 public extensions, persistence, architecture tests, forward migration, SQL tests, ADR/module documentation and Spec Kit artifacts. Do not include `docs/thesis.pdf` or `docs/thesis_text.txt` unless separately intended.

## 8. Evidence record

For each successful verification record:

- commit SHA;
- Java/Gradle version;
- database environment/project identity when applicable;
- exact command;
- real pass/fail output and timestamp.

Keep status `Planned` for every command not actually executed.

