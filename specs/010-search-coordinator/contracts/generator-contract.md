# Contract: Strategy Generator v1

## Purpose

Cho phép thay thuật toán Search mà không đổi Experiment, Backtest, Evaluation, Leaderboard hoặc
public workflow.

## Descriptor

- `generatorId`: typed stable slug.
- `generatorVersion`: exact version; không dùng range/fallback silent.
- `stateContractVersion`: exact serialization contract.
- `supportedParameterKinds`: danh sách capability.
- `descriptorFingerprint`: canonical SHA-256.

Registry MUST từ chối duplicate identity/version và trả stable unsupported outcome khi lookup fail.

## Generate-next input

- frozen search-space canonical representation/fingerprint;
- signed 64-bit seed;
- prior canonical generator state hoặc explicit initial state;
- next expected generation index;
- set Candidate fingerprints đã chấp nhận hoặc equivalent duplicate guard;
- bounded remaining draw budget.

## Generate-next outcome

Một trong:

- `GENERATED`: exact Candidate parameter map, candidate fingerprint, generation index và next state;
- `EXHAUSTED`: không còn unique Candidate hợp lệ;
- `NO_PROGRESS`: duplicate draws vượt bounded guard;
- stable validation/unsupported failure.

## Invariants

- Same descriptor + frozen input + seed + prior state → byte-equivalent canonical outcome.
- Parameter keys/options được canonical-sort; decimal không đi qua binary floating point.
- Generator không đọc clock/network/database và không tạo Experiment/Candidate/Job identity.
- Output phải nằm trong search space và không được sửa input/prior state.
- State phải versioned, round-trip được và thay đổi khi outcome `GENERATED`.

## Baseline Random Search

`random-search` v1 sample từ canonical discrete domain bằng deterministic seeded state. Duplicate
draw được skip nhưng tổng draw cho một decision bị giới hạn. Search space hữu hạn có thể trả
`EXHAUSTED` trước `maximumCandidates`.
