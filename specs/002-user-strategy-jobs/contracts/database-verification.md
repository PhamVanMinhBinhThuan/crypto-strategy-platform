# Database Verification Contract: User Strategies and Jobs

## Purpose

Define executable assertions for the forward migration. The SQL suite must fail on the first unexpected result and roll back all fixture data.

## Required assertions

| ID | Assertion | Maps to |
| --- | --- | --- |
| V2-01 | Three User Strategy tables and one Job table exist; baseline migration content remains untouched. | FR-002, FR-010, FR-016 |
| V2-02 | Business schema contains no password, password hash, session token or refresh token column. | FR-001, SC-008 |
| V2-03 | User Strategy requires an Auth owner; active name uniqueness is scoped per owner and case-insensitive. | FR-002, FR-009, SC-001, SC-002 |
| V2-04 | Strategy version enforces positive version number, kind-specific source fields, fingerprint uniqueness and valid plugin FKs. | FR-005–FR-007 |
| V2-05 | Composite component enforces ordered identity, valid plugin references and positive optional weight. | FR-006 |
| V2-06 | Manifest may reference a valid User Strategy version while preserving existing snapshot columns. | FR-008 |
| V2-07 | Job requires an Experiment, valid type/status/progress and a type-compatible Candidate from the same Experiment. | FR-010, FR-011 |
| V2-08 | Backtest Job is unique per Candidate; Search Job has no Candidate. | FR-011, FR-013 |
| V2-09 | Attempt requires a real Job, matches that Job's Candidate and keeps unique attempt numbers. | FR-012, FR-018, SC-004 |
| V2-10 | Job ownership resolves through Experiment and User Strategy ownership resolves directly to Auth user. | FR-009, FR-014, SC-005 |
| V2-11 | Recovery indexes exist for owner Strategy listing and pending/retry Jobs. | FR-015, SC-006 |
| V2-12 | `anon` and `authenticated` have no direct privilege on new tables or business schemas. | FR-017, SC-008 |

## Migration upgrade assertions

- Before adding the Attempt foreign key, every distinct legacy `execution_attempt.job_id` must produce exactly one Backtest Job.
- Migration must abort if one legacy Job ID points to multiple Candidates; it must not guess or discard evidence.
- Migration must abort if one legacy Candidate points to multiple Job IDs because MVP permits one logical Backtest Job per Candidate.
- After migration, no Attempt may have an orphan Job or mismatched Candidate.
- Existing Experiment, Candidate, Attempt and Result identifiers must be preserved.

## Authorization boundary

SQL verifies ownership paths and lack of direct browser grants. It does not impersonate the Java application or claim endpoint authorization is implemented. Future API integration tests must prove user A cannot read, update, archive or execute user B's Strategy and cannot read/stop user B's Job.

## Execution rules

1. Review and run `supabase db push --dry-run` before any remote mutation.
2. Apply to a shared project only after explicit approval.
3. Run fixtures inside `BEGIN`/`ROLLBACK`.
4. Record real command output as evidence; do not mark planned checks verified without results.
