# Data Model: F-014 Demo and Evidence

F014 không tạo business database mới. Các model này là artifact cấu hình/kiểm chứng; dữ liệu nghiệp vụ vẫn thuộc capability owner hiện có.

## DemoProfile

- `id`, `mode` (`LIVE`/`FIXTURE`), `baseUrls`, `marketProvider`, `symbols`, `timeframes`
- `strategyIds`, `searchConfiguration`, `stopCondition`, `topK`, `createdForCommit`
- `nonSecretEnvironment`; tuyệt đối không chứa token, password hoặc service-role key.

## DemoScenario

- `id`, `title`, `preconditions`, `steps`, `expectedResults`
- `profileId`, `owner`, `cleanup`, `fallbackAction`
- `rubricCriteria[]`, `acceptanceScenarios[]`

## EvidenceRecord

- `id`, `criterionId`, `status`, `commitSha`, `capturedAt`
- `environment`, `nonSecretConfiguration`, `commandOrAction`
- `artifactPaths[]`, `observedResult`, `limitations[]`
- `VERIFIED` chỉ hợp lệ khi đủ commit, môi trường, thao tác, kết quả và artifact xem lại được.

## ReadinessFinding

- `id`, `severity`, `capabilityOwner`, `description`, `remediation`, `status`
- `affectedRequirementIds[]`, `evidenceRecordIds[]`; chỉ đóng khi evidence chứng minh remediation.

## FallbackProfile

- `id`, `fixtureVersion`, `checksum`, `activationSteps`, `visibleLabel`
- `limitations`, `allowedEnvironments`; không dùng làm evidence cho live-provider criterion.

## Relationships and lifecycle

```text
DemoProfile 1 ── * DemoScenario * ── * RubricCriterion
DemoScenario 1 ── * EvidenceRecord * ── * ReadinessFinding
DemoProfile 0..1 ── 1 FallbackProfile

PLANNED → PARTIAL → VERIFIED
    └────→ BLOCKED ─→ PLANNED/PARTIAL
```

Evidence được append/revise, không sửa kết quả cũ để biến fail thành pass. Reproduction nghiệp vụ tạo run mới, không ghi đè nguồn.
