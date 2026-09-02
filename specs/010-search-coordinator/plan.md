# Implementation Plan: Search Coordinator

**Branch**: `010-search-coordinator` | **Date**: 2026-09-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/010-search-coordinator/spec.md`

## Summary

Thêm capability Search thuần để sinh Candidate deterministic qua generator registry và một
Coordinator runtime trong Worker để điều phối Search Job theo durable state. Start/Reproduce
Experiment được đóng gói thành application command atomic, phát `search.requests.v1` qua Outbox,
consume `candidate.evaluated.v1` bằng consumer group riêng, giới hạn in-flight, phục hồi bằng
reconciliation và chỉ sau evidence đầy đủ mới gỡ readiness gate trong F-009.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3, Spring Data Redis, Spring JDBC, Jackson; module domain,
strategy, experiment, contracts, persistence và worker hiện có  
**Storage**: PostgreSQL/Supabase là source of truth; Redis Streams là delivery layer  
**Testing**: JUnit 5, AssertJ, Mockito, ArchUnit; PostgreSQL integration source set và Redis smoke  
**Target Platform**: Linux container/server; local macOS/Linux development  
**Project Type**: Modular monolith với API app và Worker app  
**Performance Goals**: Start acceptance p95 dưới 2 giây; bounded active Backtests; progress
notification trong 5 giây ở môi trường acceptance  
**Constraints**: At-least-once delivery, no cross-module internal import/direct table write,
typed ULID/UUID, exact decimal, UTC, forward-only migration, deterministic fingerprints  
**Scale/Scope**: Một Coordinator logical owner mỗi Search Job; nhiều Search Jobs song song;
Random Search deterministic MVP; không Bayesian/adaptive search hoặc multi-leader consensus

## Constitution Check

### Pre-design gates

| Gate | Kết quả | Bằng chứng/quyết định |
| --- | --- | --- |
| Spec-first và acceptance measurable | Pass | `spec.md`, checklist quality pass |
| Một owner cho business concept | Pass | Search sở hữu generator/search state; Experiment sở hữu Experiment/Candidate/Job/Outbox |
| Không cross-module internal/table write | Pass | Worker orchestration chỉ gọi published ports; persistence adapter triển khai từng owner port |
| Reproducibility và immutable evidence | Pass | Frozen manifest + generator version/seed/state + Candidate fingerprint |
| Versioned contracts/provider isolation | Pass | Generator contract và Redis envelope có version |
| Durable truth, duplicate-safe delivery | Pass | PostgreSQL state + Outbox; Redis có thể replay/rebuild |
| Security/ownership/redaction | Pass | Public command giữ UUID owner check và safe F-009 error mapping |
| Evidence tương xứng | Pass | Unit, contract, architecture, restart/replay, DB và Redis integration planned |
| ADR cho quyết định xuyên module | Required | Tạo ADR-0016, chuyển Accepted trước implementation phụ thuộc |

Không có Constitution violation cần miễn trừ.

## Architecture and Ownership

### Capability split

- `modules/search`: pure generator contract, registry, Random generator, canonical search-space
  validation, deterministic state transition và Search-owned state ports. Chỉ phụ thuộc Domain và
  Strategy theo dependency matrix hiện hành.
- `modules/experiment`: atomic command/use cases cho Start/Reproduce, Candidate + Backtest Job
  allocation, Search Job progress/terminal transition và Experiment-owned Outbox. Không chứa
  generator algorithm.
- `modules/persistence`: JDBC adapters cho Search state và atomic Experiment graph; forward
  migration, locking/fencing và recovery queries.
- `apps/worker`: Search request/completion consumers, orchestration, bounded in-flight scheduling,
  retry/reclaim/reconciliation và event publication. Không sở hữu durable business rules.
- `apps/api`: map validated request sang published Start/Reproduce port; bỏ gate 503 sau khi
  readiness tests pass. Không gọi Worker hay Redis trực tiếp.

### Command and event flow

1. API canonicalize + claim idempotency, validate/freeze inputs, atomically create Experiment và
   Search Job cùng Outbox `SearchRequested`.
2. Existing Outbox publisher maps versioned event tới `search.requests.v1`.
3. Search consumer claims Search Job attempt, loads frozen manifest + durable Search state, then
   fills available in-flight slots bằng deterministic generator transitions.
4. Mỗi allocation atomically persists Candidate, next generator state, Backtest Job và its Outbox;
   duplicate decision returns existing identities.
5. Ranking Handler vẫn sở hữu Evaluation/Leaderboard update; Search consumer group riêng nhận
   `candidate.evaluated.v1`, reconciles authoritative counts, checks stop/fill decision.
6. Stop prevents new allocation; existing F-007 cancellation/reconciler drains active Jobs.
7. Scheduled Search reconciler scans non-terminal Search Jobs and resumes missing decisions from DB.
8. Reproduction creates linked run, copies frozen manifest/Candidate sequence, dispatches exactly
   those Candidates, then existing reproduction verification compares evidence.

## Project Structure

### Documentation for this feature

```text
specs/010-search-coordinator/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── generator-contract.md
│   ├── coordination-contract.md
│   ├── search-events.md
│   └── public-readiness-contract.md
└── checklists/requirements.md
```

### Source code impact

```text
modules/search/src/main/java/com/cryptostrategy/platform/search/
├── api/model/                 # typed generator/search values
├── api/port/in/               # deterministic generation capability
├── api/port/out/              # Search-owned state store
└── internal/                  # registry, validation, Random generator

modules/experiment/src/main/java/.../experiment/
├── api/port/in/               # atomic start/reproduce/allocation/progress ports
└── internal/                  # Experiment-owned transaction rules

modules/persistence/src/main/java/.../persistence/internal/
├── search/                    # Search state adapter
└── experiment/                # atomic allocation/start extensions

apps/worker/src/main/java/.../worker/search/
├── consumer/                  # SearchRequested + CandidateEvaluated handlers
├── coordination/              # bounded scheduling decisions
└── reconciliation/            # restart/queue-loss recovery

apps/api/src/main/java/.../api/
├── config/                    # published component wiring
└── experiment/                # remove readiness gate after evidence
```

## Phase 0: Research outcomes

Các quyết định và alternatives nằm trong [research.md](research.md). Không còn
`NEEDS CLARIFICATION`.

## Phase 1: Design outcomes

- Data ownership, fields, invariants và transition: [data-model.md](data-model.md)
- Generator boundary: [contracts/generator-contract.md](contracts/generator-contract.md)
- Atomic coordination/recovery: [contracts/coordination-contract.md](contracts/coordination-contract.md)
- Redis messages/versioning: [contracts/search-events.md](contracts/search-events.md)
- Điều kiện gỡ public gate: [contracts/public-readiness-contract.md](contracts/public-readiness-contract.md)
- Runnable evidence guide: [quickstart.md](quickstart.md)

## Migration and Compatibility

- Tạo migration mới sau `20260902000100_f009_standalone_backtest.sql`; không sửa migration cũ.
- Giữ `search.requests.v1` backward-compatible; các field cũ vẫn bắt buộc, field mới optional.
- Thêm consumer group riêng cho Coordinator; không dùng ranking group và không tranh ACK.
- Candidate/Job/Experiment public representation giữ nguyên F-009.
- Gỡ `DEPENDENCY_UNAVAILABLE` là readiness change, không đổi request/response schema.

## Verification Strategy

1. Pure Search tests: determinism, canonical ordering, exact values, duplicate/out-of-space output,
   registry replaceability và no-progress guard.
2. Experiment transaction tests: start/reproduce replay/conflict, atomic allocation, rollback,
   ownership, stop race và terminal transition.
3. Worker tests: separate consumer group, bounded in-flight, duplicate/stale/out-of-order messages,
   crash boundaries, retry/dead-letter và reconciliation after queue loss.
4. Architecture tests: dependency matrix, no internal imports, no direct Worker SQL, no framework
   in pure Search module.
5. API tests: 202/Location/idempotency/ownership và removal of 503 only after runtime wiring.
6. PostgreSQL/Redis integration: forward migration, concurrent allocation/fencing, outbox delivery,
   restart/reclaim and end-to-end finite Experiment/reproduction.

## Post-design Constitution Check

Pass. Design giữ Search algorithm thuần trong owner module, durable mutations ở owner application
ports, versioned messages qua Outbox, F-009 chỉ làm transport, và có evidence plan cho mọi failure
boundary. ADR-0016 là gate trước implementation; không phát sinh ngoại lệ Constitution.

## Complexity Tracking

Không có Constitution violation. Search state riêng là dữ liệu tối thiểu cần để deterministic
resume; không giới thiệu Kafka, Kubernetes, microservices, CQRS/Event Sourcing hay distributed
leader election.
