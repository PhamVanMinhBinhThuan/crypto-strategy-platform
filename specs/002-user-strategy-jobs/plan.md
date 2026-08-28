# Implementation Plan: User-owned Strategies and Durable Jobs

**Branch**: `db-setup-v2` | **Date**: 2026-08-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-user-strategy-jobs/spec.md`

## Summary

Add a forward-only PostgreSQL migration that separates the shared Strategy plugin catalog from private user-saved Strategy definitions and introduces a durable Job aggregate above Execution Attempt. Keep Supabase Auth as the only credential/session owner, derive Job authorization through Experiment ownership, preserve immutable Experiment snapshots, backfill existing Attempt job identifiers, and update database/architecture documentation plus transactional SQL verification.

## Technical Context

**Language/Version**: PostgreSQL SQL supported by the linked Supabase project; Markdown/Mermaid for design artifacts

**Primary Dependencies**: Supabase CLI; existing `auth.users`; baseline migration `20260827000100_create_database_baseline.sql`

**Storage**: Supabase-hosted PostgreSQL is source of truth; Redis remains a recoverable queue/cache and is not modified by this schema feature

**Testing**: Transactional PostgreSQL assertion suite under `supabase/tests/database/`; `supabase db push --dry-run`; `supabase db lint --linked --fail-on error`

**Target Platform**: Shared Supabase development database after explicit approval; local/static review before any remote mutation

**Project Type**: Database schema and architecture documentation increment for a planned web application

**Performance Goals**: Indexed owner Strategy listing, Experiment Job recovery, and pending/retry Job lookup; no production benchmark is claimed in this documentation-only repository

**Constraints**: Do not edit the applied baseline migration; browser roles retain no business-table access; exact owner authorization remains in the Java application boundary; no password/refresh token duplication; no full CQRS/Event Sourcing

**Scale/Scope**: Three new Strategy tables, one Job table, one optional manifest reference, Attempt foreign keys, supporting indexes, documentation, and SQL verification

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Gate | Result | Evidence |
| --- | --- | --- |
| Specification before schema change | PASS | `spec.md` defines business outcomes, acceptance scenarios and measurable criteria. |
| ADR for long-lived ownership/contract decision | PASS WITH MERGE GATE | ADR-0012 is created as `Proposed`; it must be team-reviewed and changed to `Accepted` before dependent implementation is merged. |
| One capability owns durable data | PASS | `strategy-core` owns user Strategy records; `experiment` owns Job; `backtesting` owns Attempts. |
| Reproducibility and immutability | PASS | User Strategy versions are snapshots; Manifest retains exact frozen data and only adds provenance. |
| Auth and owner checks | PASS | Supabase Auth remains identity owner; browser roles receive no table privilege; API must query through owner paths. |
| Durable async state | PASS | PostgreSQL stores Job and Attempts; Outbox/Redis flow remains recoverable. |
| Forward migration only | PASS | New migration follows the applied baseline; baseline content is untouched. |
| Evidence honesty | PASS | Remote dry-run/apply/lint remain `Planned` until actually executed with approval. |

Post-design check: PASS with the same ADR merge gate. No Constitution exception is required.

## Project Structure

### Documentation (this feature)

```text
specs/002-user-strategy-jobs/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── database-verification.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
supabase/
├── migrations/
│   ├── 20260827000100_create_database_baseline.sql
│   └── 20260828000100_add_user_strategies_and_jobs.sql
└── tests/database/
    ├── 001_database_baseline_test.sql
    └── 002_user_strategy_jobs_test.sql

docs/
├── adr/
│   └── 0012-user-strategy-job-ownership.md
├── architecture/
│   └── data-model-overview.md
└── database/
    ├── README.md
    ├── erd.md
    ├── drawio-erd.mmd
    ├── data-dictionary.md
    └── decisions.md
```

**Structure Decision**: This repository currently contains schema and design artifacts but no Java application implementation. This increment therefore delivers the forward migration, executable database assertions, ADR, and synchronized documentation. Java authorization and repository integration tests remain explicit follow-up work rather than fabricated code.

## Complexity Tracking

No Constitution violations require justification.

