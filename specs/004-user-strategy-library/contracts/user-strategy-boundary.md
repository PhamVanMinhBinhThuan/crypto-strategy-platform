# Contract: Owner-Scoped User Strategy Library

## Purpose

Define internal application commands and queries for private saved Strategy configurations. Public HTTP endpoints and UI behavior are outside F-004.

## Identity source

- The application composition boundary obtains the authenticated Supabase user UUID.
- The UUID is supplied separately from editable request data.
- A client-supplied `ownerUserId` field is never trusted or persisted as authorization evidence.
- A User Strategy ULID alone never grants access.

## Input use cases

### List usable Strategies

```text
listUsableStrategies(authenticatedUserId, usable page request)
    -> UsableStrategyCatalog(system page, owner's active private page)
```

This is one application use case rather than a requirement for callers to merge two unrelated queries. The system and private sections have independent opaque cursors. Each section defaults to 20 entries, accepts 1–100 entries, and rejects any other page size. System descriptors are ordered by plugin key/version; private summaries are ordered by `createdAt DESC` with ULID as the stable tie-breaker. The result reveals no other user's root, version, name, or count.

### Create User Strategy

```text
createUserStrategy(authenticatedUserId, name, description, kind, complete draft source)
    -> UserStrategy + draft UserStrategyVersion
```

The service validates the referenced trusted descriptor/policy, resolves all defaults, creates the canonical fingerprint, and persists a complete draft. Owner comes only from `authenticatedUserId`.

### Create next version

```text
createNextVersion(authenticatedUserId, userStrategyId, expectedLatestVersionNo, complete draft source)
    -> draft UserStrategyVersion | STRATEGY_CONFLICT
```

Root kind cannot change. The expected latest version prevents a stale client from silently creating a version from outdated state.

### Publish version

```text
publishVersion(authenticatedUserId, userStrategyId, versionId, expectedLifecycle=DRAFT)
    -> immutable StrategySnapshot | STRATEGY_CONFLICT
```

Publication is atomic, owner-scoped, one-way, and revalidates the complete canonical source. Exactly one concurrent request can transition the same draft.

### Get/resolve

```text
getUserStrategy(authenticatedUserId, userStrategyId)
resolvePublishedSnapshot(authenticatedUserId, userStrategyVersionId)
```

Cross-owner and missing targets return the same `USER_STRATEGY_NOT_FOUND` response. Archived roots are omitted from normal listing, but an authorized published version remains resolvable for provenance.

### Archive root

```text
archiveUserStrategy(authenticatedUserId, userStrategyId, expectedStatus=ACTIVE)
    -> archived summary | STRATEGY_CONFLICT
```

Archive is one-way. It does not delete versions/components and prevents new drafts/publications.

## Draft source shapes

### Single

- exact registered system `StrategyVersionId` and semantic key/version;
- supplied parameters, which are converted to a complete canonical set before persistence.

### Composite

- `majority-vote@1.0.0`;
- at least two distinct exact registered system Strategy versions;
- complete canonical parameters for every component;
- no weight in F-004;
- no User Strategy or nested Composite reference.

## Output views

### Summary

Contains root ID, owner-authorized name/description, kind, active/archive status, latest version number/status, and timestamps. It never contains executable implementation objects.

### Published snapshot

Contains the immutable information defined in [data-model.md](../data-model.md): exact source version, complete resolved parameters, policy/components where applicable, owner path, and `strategy-v1` fingerprint. Mutable root display metadata is excluded from fingerprint meaning.

## Stable application errors

| Code | Meaning |
|---|---|
| `USER_STRATEGY_NOT_FOUND` | Missing or not owned; deliberately non-disclosing. |
| `USER_STRATEGY_NAME_CONFLICT` | Owner already has the same active normalized name. |
| `STRATEGY_CONFLICT` | Expected version/lifecycle/root state is stale. |
| `USER_STRATEGY_ARCHIVED` | Archived root cannot create or publish a version. |
| `INVALID_USER_STRATEGY` | Kind/source/component invariants failed. |
| Runtime validation codes | Descriptor, parameters, context, or Composite validation failed. |

## Authorization acceptance contract

For two distinct authenticated UUIDs:

- both can list the shared system catalog;
- each lists only their active private roots;
- the same active name is allowed across owners;
- every cross-owner get/create-next/publish/archive/resolve attempt returns the non-disclosing not-found result;
- no repository method used by an application service offers an unscoped private lookup.
