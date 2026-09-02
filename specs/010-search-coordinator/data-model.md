# Data Model: Search Coordinator

## Ownership summary

| Entity | Owner | Persistence/source of truth |
| --- | --- | --- |
| Experiment, Manifest, Candidate, Job, Attempt, Outbox | Experiment (F-005) | Existing `experiment`/`platform` schema |
| Search Run, Generator State, Coordination Decision | Search (F-010) | New forward-only Search-owned tables |
| Backtest Result, Evaluation, Leaderboard | F-006 owners | Existing immutable tables |
| Redis request/completion messages | Contract/delivery | Transient, rebuildable from durable state |

## 1. Search Run

Một durable coordination state cho đúng một SEARCH Job.

| Field | Type | Rules |
| --- | --- | --- |
| `searchRunId` | typed ULID | Primary identity, immutable |
| `experimentId` | typed ULID | Unique; reference identity, Experiment owner vẫn authoritative |
| `searchJobId` | typed ULID | Unique; phải là Job type SEARCH |
| `generatorId` | typed slug | Frozen, non-empty |
| `generatorVersion` | semantic version | Frozen, exact match registry |
| `seed` | signed 64-bit integer | Frozen |
| `searchSpaceFingerprint` | SHA-256 hex | Frozen canonical input |
| `generatorState` | canonical object | State sau Candidate cuối đã commit |
| `nextGenerationIndex` | non-negative integer | Monotonic; bằng số allocation decisions đã commit |
| `maximumCandidates` | positive integer | Frozen stop condition |
| `maximumDuration` | positive duration | Frozen stop condition |
| `maxInFlight` | positive integer | Bounded scheduling window |
| `status` | enum | `PENDING`, `RUNNING`, `STOPPING`, `COMPLETED`, `STOPPED`, `FAILED` |
| `version` | non-negative integer | Optimistic fencing/CAS |
| `startedAt` | UTC instant, nullable | Set khi claim lần đầu |
| `deadlineAt` | UTC instant, nullable | Derived once from start + duration |
| `finishedAt` | UTC instant, nullable | Set đúng một lần ở terminal transition |
| `failureCode/message` | nullable safe strings | Chỉ terminal failure; không chứa secret/internal detail |
| `createdAt`, `updatedAt` | UTC instant | Audit |

### Invariants

- Một Experiment có tối đa một Search Run và một SEARCH Job.
- `nextGenerationIndex` không giảm; state/index/fingerprint đổi cùng transaction allocation.
- Terminal Search Run bất biến ngoài idempotent replay.
- `active = allocated - terminal`; quyết định fill dùng authoritative count, không dùng event count.
- `deadlineAt` được tính đúng một lần từ `startedAt + maximumDuration` bằng injected UTC clock khi
  first start; restart/retry chỉ reload giá trị đã lưu, tuyệt đối không kéo dài deadline.
- Completion/deadline race dùng cùng lock/fence và authoritative reload; terminal transition đầu
  tiên commit thắng, mọi replay sau đó trả idempotent existing outcome.

### State transitions

```text
PENDING -> RUNNING -> COMPLETED
                   -> STOPPING -> STOPPED
                   -> FAILED
PENDING ----------> STOPPING -> STOPPED
PENDING/RUNNING ---> FAILED
```

## 2. Generator Definition

Không nhất thiết là database row; là published immutable descriptor.

| Field | Rules |
| --- | --- |
| `generatorId` | Stable typed slug, ví dụ `random-search` |
| `version` | Exact supported version |
| `stateContractVersion` | Version format state |
| `supportedParameterKinds` | Khả năng được khai báo rõ |
| `descriptorFingerprint` | Canonical fingerprint của descriptor |

Registry từ chối duplicate `(generatorId, version)` và lookup không fallback silent.

## 3. Generator State

Canonical object do Search sở hữu, lưu trong Search Run.

Baseline Random Search state:

| Field | Rules |
| --- | --- |
| `contractVersion` | `random-state-v1` |
| `seed` | Khớp frozen seed |
| `drawIndex` | Non-negative, monotonic |
| `acceptedCount` | Bằng `nextGenerationIndex` |
| `exhausted` | Chỉ true khi không còn unique Candidate hợp lệ |

State phải round-trip canonical và không chứa runtime clock, Worker ID hay unordered values.

## 4. Coordination Decision

Append-only audit cho durable decision quan trọng; có thể lưu cùng Search Run transition nhưng
logical model phải rõ.

| Field | Rules |
| --- | --- |
| `decisionId` | typed ULID |
| `searchRunId` | Parent Search Run |
| `sequence` | Monotonic, unique trong run |
| `type` | `ALLOCATED`, `DUPLICATE_SKIPPED`, `STOP_REACHED`, `FAILED` |
| `candidateId` / `backtestJobId` | Bắt buộc với `ALLOCATED` |
| `candidateFingerprint` | Bắt buộc với draw có Candidate |
| `stateBeforeFingerprint`, `stateAfterFingerprint` | Reproducibility/audit |
| `reasonCode` | Stable safe code |
| `decidedAt` | UTC instant |

## 5. Existing Candidate/Job extensions

- Candidate giữ unique `(experiment_id, generation_index)` và
  `(experiment_id, candidate_fingerprint)`.
- Backtest Job giữ unique logical relation với `candidate_id`.
- SEARCH Job progress:
  - `totalWork = maximumCandidates`;
  - `completedWork = authoritative succeeded terminal Candidate jobs`;
  - `failedWork = authoritative failed terminal Candidate jobs`;
  - `bestScore = max eligible evaluated score`.
- Atomic allocation phải ghi Candidate + Backtest Job + JobQueued Outbox + Search next state/decision.

## 6. Reproduction initialization và verification

Reproduction Run dùng Search Run mode `REPRODUCTION` (hoặc frozen mode trong config), gồm:

- `sourceExperimentId` bắt buộc và immutable;
- ordered source Candidate snapshot/fingerprints;
- `nextGenerationIndex` chỉ tiến theo source sequence;
- không gọi generator để thay đổi Candidate definition;
- original entities không bị update.

Source phải đúng owner, terminal và đầy đủ Manifest/Candidate/Result provenance trước atomic create.

Mỗi reproduction tạo một durable **Reproduction Verification** cùng transaction initialization:

| Field | Rules |
| --- | --- |
| `verificationId` | typed ULID, immutable |
| `sourceExperimentId`, `reproductionExperimentId` | Unique lineage pair, đúng owner |
| `status` | `PENDING`, `RUNNING`, `MATCHED`, `MISMATCHED`, `FAILED` |
| `expectedVersion` | Optimistic fencing/idempotent terminal trigger |
| `trade/metrics/fingerprint outcome` | Chỉ set ở terminal verification |
| `safeDifferences` | Bounded/redacted, không chứa secret/provider payload |
| `startedAt`, `finishedAt` | UTC instants, set đúng một lần |

API chỉ tạo durable graph và `PENDING` verification rồi trả `202`. Khi reproduction Experiment
terminal, handler/reconciler claim verification, gọi published execution verification port và lưu
terminal outcome. Restart ở bất kỳ bước nào tiếp tục từ durable state, không chạy verifier trong
request thread.

## 7. Constraints and indexes

- Unique: Search Run `experiment_id`, `search_job_id`.
- Unique: decision `(search_run_id, sequence)` và allocated `candidate_id`.
- Index recovery: `(status, updated_at, search_run_id)` cho non-terminal scan.
- Index owner traversal vẫn đi qua Experiment owner boundary, không duplicate owner làm authority.
- Foreign keys có thể bảo vệ integrity nhưng không cấp quyền cross-owner hoặc quyền ghi chéo.
- Mọi schema change dùng migration mới; không sửa migration F-005–F-009 đã áp dụng.

## 8. Transaction boundaries

1. **Start**: idempotency claim + Experiment/Manifest + SEARCH Job + Search Run + Outbox hoặc không gì.
2. **Allocate**: lock/fence Search Run; validate state; Candidate + Backtest Job + next state + decision
   + Outbox hoặc idempotent existing outcome.
3. **Completion reconcile**: authoritative child state → progress + stop/fill/terminal decision.
4. **Stop**: Experiment `STOP_REQUESTED` chặn allocation; no long-running lock while cancelling work.
5. **Reproduce initialization**: new linked Experiment + copied frozen evidence + Search Run/Job/Outbox
   + `PENDING` Reproduction Verification hoặc không gì.
6. **Reproduction verification**: terminal trigger claim/fence verification, compare ngoài DB lock,
   rồi revalidate và commit đúng một terminal verification outcome.

Không transaction nào được giữ mở trong thời gian Backtest/Evaluation chạy hoặc lúc chờ Redis.
