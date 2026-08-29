# Contract: F-003 Database Verification

## Authoritative schema

F-003 uses the applied migration:

```text
supabase/migrations/20260827000100_create_database_baseline.sql
```

The migration remains unchanged. F-003 creates no new migration.

## Static verification

Review must confirm:

- adapters address only existing `market.*` tables;
- SQL is schema-qualified;
- no Flyway/Liquibase/second migration source is added;
- no mutation operation exists for accepted Candle or Dataset evidence;
- Gradle dependencies follow `persistence -> domain + market-data public ports`;
- no JDBC/row mapping appears in `domain` or Market Data application packages.

## Default deterministic verification

The root `clean check` remains independent of Docker, live Supabase, Redis, and Binance. It covers domain/application/provider contracts with fakes and architecture rules.

## Local database integration verification

Use the repository's existing Supabase CLI/local configuration so the authoritative migrations and SQL tests create an isolated PostgreSQL database. Do not point this suite at shared development.

Planned sequence:

```text
supabase start
supabase db reset
./gradlew marketDataIntegrationTest
supabase test db
```

Stop the local stack after verification when it is not otherwise needed.

The dedicated Java integration source set reads local database configuration from environment/runtime arguments. It must fail clearly when local prerequisites are missing and must never print credential values.

## Required adapter scenarios

1. Persist and reload Asset/Trading Pair with canonical pair reconstructed through base/quote join.
2. Persist exact `numeric(30,12)` values and UTC timestamps without precision/timezone loss.
3. Insert one closed Candle and reprocess the exact duplicate idempotently.
4. Reject same natural key with different canonical content and preserve the original row.
5. Merge overlapping/concurrent batches without duplicate logical Candles.
6. Finalize Dataset Version plus contiguous membership atomically.
7. Force a membership insert failure and prove all records from that finalization roll back.
8. Finalize equivalent Dataset concurrently and return one logical winner.
9. Present the same checksum with different provenance/membership and receive integrity conflict.
10. Load Dataset membership ordered by sequence and verify scope, continuity, count, and checksum.
11. Attempt controlled test tampering, then prove integrity verification refuses to return valid evidence.
12. Confirm no adapter/API method updates or deletes accepted Candle/Dataset evidence.

## Existing baseline assertions retained

The existing SQL suite remains responsible for physical constraints including:

- ULID format;
- canonical Timeframe checks;
- Candle natural uniqueness and OHLCV/time checks;
- Dataset checksum format/uniqueness and valid range/count;
- membership sequence/member uniqueness;
- critical indexes and foreign keys.

F-003 integration tests prove application transaction behavior and mapping on top of those constraints; they do not duplicate or replace the SQL baseline suite.

## Remote environments

Applying a future migration or mutating shared development requires separate explicit approval. No such action is part of this plan or verification contract.
