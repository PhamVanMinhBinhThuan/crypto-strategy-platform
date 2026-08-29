# Quickstart: Verify User Strategies and Jobs

## 1. Confirm branch and files

```powershell
git branch --show-current
git diff --check
git diff -- supabase/migrations/20260827000100_create_database_baseline.sql
```

Expected: branch is `db-setup-v2`, whitespace check passes, and the applied baseline migration has no diff.

## 2. Review the forward migration

Review `supabase/migrations/20260828000100_add_user_strategies_and_jobs.sql` against [data-model.md](data-model.md) and [database-verification.md](contracts/database-verification.md).

Confirm the migration creates User Strategy/Job data before adding new foreign keys and preserves existing Attempt rows through backfill.

## 3. Independent local verification order

Run the two suites against separate disposable PostgreSQL databases:

```text
Database A: baseline migration → 001_database_baseline_test.sql
Database B: baseline migration → v2 migration → 002_user_strategy_jobs_test.sql
```

The baseline suite intentionally does not reference `experiment.job` or any User
Strategy table. The v2 suite owns every assertion for the new schema. Do not run the
baseline suite after applying v2 and then weaken its exact baseline table/FK counts.

## 4. Non-mutating linked validation

```powershell
supabase db push --dry-run
```

Expected: only the new forward migration is pending. Do not run `supabase db push` without explicit approval.

## 5. Apply only after approval

```powershell
supabase db push
supabase migration list
supabase db lint --linked --fail-on error
```

This step is intentionally not executed as part of documentation/schema preparation unless the user separately authorizes remote mutation.

## 6. Run transactional assertions after migration exists in the target database

```powershell
supabase db query --linked --file supabase/tests/database/002_user_strategy_jobs_test.sql
```

Expected: every assertion passes and the final rollback leaves no fixture data.

## 7. Evidence state

Until the commands run against a reviewed non-production target, linked dry-run, apply, lint and SQL suite remain `Planned — not yet verified`. Record project, migration version, commit, environment and timestamp when real evidence is collected; never record credentials.

### Current branch evidence — 2026-08-29

- Local PostgreSQL 15 Database A — baseline migration → baseline suite: `Verified — PASS`.
- Local PostgreSQL 15 Database B — baseline migration → v2 migration → v2 suite: `Verified — PASS`.
- `git diff --check`: rerun in final validation after documentation cleanup.
- Baseline migration diff: no content changes detected.
- Supabase dry-run/linked lint: `Blocked` — Supabase CLI, local project link and access-token context are unavailable; an `npx` CLI acquisition attempt did not complete.
- Linked SQL suite: `Planned` — run only after the reviewed v2 migration exists in shared development.
- Remote apply: not attempted; it requires explicit approval.
