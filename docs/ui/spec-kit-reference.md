# Spec Kit — Shared UI Reference Policy

This file is the single shared entry point that frontend Spec Kit workflows should read.

## Mandatory rule

Any feature that creates or modifies browser UI under `apps/web` MUST inspect:

1. `docs/ui/README.md`
2. `docs/ui/screen-map.md`
3. `docs/ui/design-system.md`
4. `docs/ui/interaction-states.md`
5. `docs/ui/features/<FEATURE-ID>.md` when present
6. relevant screenshots under `docs/ui/screens/`
7. relevant source under `docs/ui/prototype/`
8. F-011 Frontend Foundation contract
9. relevant released F-009/F-010/public capability contracts

## Authority

```text
Constitution / accepted ADRs
        >
released public contracts
        >
F-011 frontend foundation
        >
current feature spec
        >
shared UI reference
```

If a prototype field/action is absent from the released public contract, Spec Kit MUST NOT create browser business logic to synthesize it. The correct outcomes are to omit it, map it to an existing public read, or record an upstream contract/dependency gap.

## Forbidden production copying

Do not copy or recreate from the prototype:

- Vite application wiring;
- an alternate React root;
- alternate Sidebar/Header/ApplicationShell ownership;
- prototype `AppContext` business simulation;
- `setTimeout` Search/Backtest simulation;
- mock Worker orchestration;
- frontend Candidate generation;
- frontend Backtest execution;
- frontend Evaluation or Ranking calculations;
- a second authentication/session implementation;
- a second HTTP client singleton;
- a second WebSocket singleton;
- direct Supabase business-table access;
- direct Redis, Worker, Binance or internal-service access.

## Required production boundary

```text
Feature UI
   -> F-011 published HTTP/realtime/auth/shared-state interfaces
   -> F-009 public REST/WebSocket boundary
   -> capability owners / F-010 Search Coordinator
```

REST/durable snapshots are authoritative where the released contract says so. Realtime notifications improve freshness and must follow the released reconnect, deduplication and snapshot-recovery rules.

## Prompt block

For every frontend `/speckit-specify`, `/speckit-clarify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-analyze`, `/speckit-implement` and `/speckit-converge`, include or enforce this instruction:

```text
SHARED UI REFERENCE

This feature modifies browser UI.
Read docs/ui/spec-kit-reference.md first and follow it as a mandatory project rule.
Read docs/ui/features/<FEATURE-ID>.md and only the relevant shared screens/prototype source.
Do not create a feature-local copy of the shared UI reference.
Public contracts override prototype behavior whenever they conflict.
```
