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
**Performance Goals**: Start acceptance p95 dưới 2 giây trên ít nhất 100 request hợp lệ sau warm-up,
đo từ API command receipt đến atomic commit và response durable identity, không gồm container startup
hoặc migration; bounded active Backtests; progress notification trong 5 giây ở môi trường acceptance
**Constraints**: At-least-once delivery, no cross-module internal import/direct table write,
typed ULID/UUID, exact decimal, UTC, forward-only migration, deterministic fingerprints  
**Scale/Scope**: Một active Coordinator claimant có database fence mỗi Search Job, không phải
business/data owner; nhiều Search Jobs song song;
Random Search deterministic trên một Strategy version trong MVP; không Composite Search,
Bayesian/adaptive search hoặc multi-leader consensus

## Constitution Check

### Pre-design gates

| Gate | Kết quả | Bằng chứng/quyết định |
| --- | --- | --- |
| Spec-first và acceptance measurable | Pass | `spec.md`, checklist quality pass |
| Một owner cho business concept | Pass | Search sở hữu generator/search state; Experiment sở hữu Experiment/Candidate/Job/Outbox |
| Không cross-module internal/table write | Pass | Execution compose public owner policies; persistence implement ADR-0016 composite gateway; Worker chỉ gọi published ports |
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
- `modules/experiment`: sở hữu Experiment/Manifest/Candidate/Job/Outbox models, validation policies
  và published owner ports. Không chứa generator algorithm hoặc cross-capability transaction.
- `modules/experiment-execution`: application orchestration boundary cho Start/Reproduce,
  allocation, progress/terminal transition và async reproduction verification. Nó compose public
  Search/Experiment ports và định nghĩa composite transaction gateways, không import internal package.
- `modules/persistence`: implement Search owner stores và composite transaction gateways của
  `experiment-execution`; JDBC adapter là nơi duy nhất atomically persist records của hai owner,
  với forward migration, locking/fencing và recovery queries.
- `apps/worker`: Search request/completion consumers, orchestration, bounded in-flight scheduling,
  retry/reclaim/reconciliation và event publication. Không sở hữu durable business rules.
- `apps/api`: map validated request sang published Start/Reproduce port; bỏ gate 503 sau khi
  readiness tests pass. Không gọi Worker hay Redis trực tiếp.

### Command and event flow

1. API gọi public `experiment-execution` Start port; orchestration canonicalize/validate owner input
   rồi composite persistence gateway atomically claim idempotency và tạo Experiment, Search Job,
   Search Run cùng Outbox `SearchRequested`.
2. Existing Outbox publisher maps versioned event tới `search.requests.v1`.
3. Search consumer claims Search Job attempt, loads frozen manifest + durable Search state, then
   fills available in-flight slots bằng deterministic generator transitions.
4. Mỗi allocation atomically persists Candidate, next generator state, Backtest Job và its Outbox;
   duplicate decision returns existing identities.
5. Ranking Handler vẫn sở hữu Evaluation/Leaderboard update; Search consumer group riêng nhận
   `candidate.evaluated.v1`, reconciles authoritative counts, checks stop/fill decision.
6. Stop prevents new allocation; existing F-007 cancellation/reconciler drains active Jobs.
7. Scheduled Search reconciler scans non-terminal Search Jobs and resumes missing decisions from DB.
8. Reproduction initialization tạo linked run, copy frozen manifest/Candidate sequence và durable
   `PENDING` verification rồi trả `202`; khi run terminal, execution handler/reconciler claim/fence,
   compare evidence và persist `MATCHED`, `MISMATCHED` hoặc `FAILED` idempotently.

Completion/deadline race dùng authoritative `completedAt` và frozen `deadlineAt`: Coordinator
reconcile completion có `completedAt <= deadlineAt` trước khi đánh giá deadline, bao gồm equality;
completion sau deadline vẫn được giữ nhưng không đảo quyết định deadline đã chặn allocation và đưa
run sang stopping. Quy tắc này được áp dụng sau durable lock/reload để mọi replay có cùng outcome.

Reproduction comparator so exact canonical values theo frozen metric version cho Total Return,
Win Rate, Maximum Drawdown và Number of Trades, cùng ordered Trade sequence và fingerprints; không
dùng tolerance ngầm.

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
├── api/port/                  # owner validation/state policies
└── internal/                  # Experiment-owned rules only

modules/experiment-execution/src/main/java/.../execution/
├── api/port/in/               # start/reproduce/allocation/progress orchestration
├── api/port/out/              # composite transaction/verification gateways
└── internal/                  # cross-capability application orchestration

modules/persistence/src/main/java/.../persistence/internal/
├── search/                    # Search state adapter
└── execution/                 # composite transaction gateway adapters

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
2. Execution/composite transaction tests: start/reproduce replay/conflict, atomic allocation,
   rollback, ownership, frozen deadline, stop race và terminal transition.
3. Worker tests: separate consumer group, bounded in-flight, duplicate/stale/out-of-order messages,
   crash boundaries, retry/dead-letter và reconciliation after queue loss.
4. Architecture tests: dependency matrix, no internal imports, no direct Worker SQL, no framework
   in pure Search module.
5. API tests: Start 202/Location/idempotency/ownership chỉ activate sau US2 evidence; Reproduce 202
   và gate riêng chỉ activate sau terminal async verification evidence US3; parameterized public
   error/progress/lifecycle failure matrix xác nhận redaction cho mọi mapping của F-010.
6. PostgreSQL/Redis integration: forward migration, concurrent allocation/fencing, outbox delivery,
   restart/reclaim and end-to-end finite Experiment/reproduction.
7. Performance acceptance: chạy ít nhất 100 Start request hợp lệ sau warm-up trong môi trường cô lập,
   đo từ API receipt đến atomic commit/response durable identity, assert p95 dưới 2 giây và ghi commit,
   environment cùng configuration; loại trừ container startup và migration khỏi sample.

## Post-design Constitution Check

Pass. Design giữ Search algorithm thuần trong owner module; cross-capability mutation đi qua public
`experiment-execution` orchestration và composite persistence gateway, không qua owner internals.
Messages dùng Outbox, F-009 chỉ làm transport, và evidence plan bao phủ mọi failure boundary.
ADR-0016 là gate trước implementation; không phát sinh ngoại lệ Constitution.

## Complexity Tracking

Không có Constitution violation. Search state riêng là dữ liệu tối thiểu cần để deterministic
resume; không giới thiệu Kafka, Kubernetes, microservices, CQRS/Event Sourcing hay distributed
leader election.
