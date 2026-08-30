# Verification Matrix: Strategy Registry and User Strategy Library

All evidence starts as **Planned — not yet implemented**. A task may change a row to Verified only after a reproducible test run records the commit and environment.

| Requirement/story | Planned evidence | Expected result | Initial status |
|---|---|---|---|
| US1, FR-001–FR-004 | Strategy contract/unit tests | Same input produces one identical decision/evidence; no clock/network/database/random dependency. | Planned — not yet implemented |
| FR-005–FR-006, SC-009 | F-003 batch interoperability test harness | Batches feed a bounded rolling context; Strategy has no Dataset/reader dependency or unbounded Candle field. | Planned — not yet implemented |
| FR-007–FR-008, SC-002 | Parameter-schema contract tests | Unknown/type/range/cross-field errors fail; defaults materialize into sorted complete values. | Planned — not yet implemented |
| FR-009–FR-011, SC-007 | Registry unit/contract tests | Stable ordering and lookup; every duplicate key/version fails composition. | Planned — not yet implemented |
| FR-012 | Moving Average golden fixtures | Deterministic BUY, SELL, HOLD, invalid parameters, and insufficient lookback. | Planned — not yet implemented |
| FR-013–FR-018, SC-003 | Two-user application and repository tests | Shared catalog visible; private rows owner-scoped; cross-owner access is non-disclosing. | Planned — not yet implemented |
| FR-019–FR-023, SC-004/SC-010 | Lifecycle/fingerprint/unit and PostgreSQL integration tests | Archive preserves published snapshots; edits create versions; resolved provenance remains unchanged. | Planned — not yet implemented |
| FR-021, SC-011 | Concurrent create/publish integration tests | Exactly one succeeds; stale operation conflicts; no overwrite/duplicate/partial rows. | Planned — not yet implemented |
| US3, FR-024–FR-028, SC-005 | Majority-vote property/permutation tests | Flat components only; majority/tie correct; input order changes neither decision nor fingerprint. | Planned — not yet implemented |
| FR-029–FR-031 | JDBC contract tests and ArchUnit | Persistence implements public ports; owner predicates present; typed ULID/UUID rules enforced. | Planned — not yet implemented |
| FR-032–FR-033 | Scope and public-signature architecture tests | No script/JAR/prompt/URL loader, transport endpoint, Job, Search, Backtest engine, or UI addition. | Planned — not yet implemented |
| FR-034, SC-006 | Test-only MACD extension and change-surface check | MACD works by plugin/registration/schema/test additions; downstream production modules unchanged. | Planned — not yet implemented |
| FR-035, Constitution V | Full Gradle check and evidence review | All deterministic suites pass; no fabricated benchmark/log/demo result. | Planned — not yet implemented |
| FR-036, SC-012 | Insufficient-lookback contract tests | Every short window returns `INSUFFICIENT_DATA`, never HOLD or a data fetch. | Planned — not yet implemented |
| Database safety | SQL static scan, local Supabase integration when available | Existing migrations unchanged; no remote apply; transactional assertions pass locally. | Planned — environment dependent |
| ADR governance | Review checklist | ADR-0001/0002/0005/0009/0012 accepted or superseded before implementation merge. | Planned — team approval required |

## Execution notes

- 2026-08-30: artifact remediation and implementation were explicitly approved by the repository owner on branch `feature/004-strategy-registry`. ADR status was not changed; the ADR merge gate remains open.
- Local PostgreSQL/Supabase verification has not run because no local database environment was established in this session. No remote migration was applied.
- 2026-08-30 final source check: independent Java 21 compilation succeeded for all 95 current Domain/Strategy/Strategies/Combination production sources, including the public `StrategyModuleFactory` boundary introduced for `apps/api`.
- The full Gradle `check` was initially blocked by a `build-logic` generated accessor cache inconsistency and two test failures (one in Worker `databaseReadiness` and one architecture boundary violation for slug IDs). After clearing the Gradle cache completely and fixing the test issues, `.\gradlew.bat clean check --no-daemon --no-build-cache --no-configuration-cache` completed successfully for all modules.
- Final static checks (`git diff --check`, scope scans with `Select-String`, migration diffs, and `git diff --stat`) passed with no trailing whitespace, no disallowed exclusions, no changes to existing migrations, and only expected new feature changes present.
- Final verification evidence has been recorded; F-004 implementation validation is successfully verified.
