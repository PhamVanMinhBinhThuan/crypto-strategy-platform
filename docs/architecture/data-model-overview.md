# Conceptual Data Model and Ownership

**Status**: Draft — Target MVP Architecture

**Last Updated**: 2026-08-14

**Owner**: Văn Minh

Tài liệu này mô tả entity, identity, ownership và lifecycle cấp hệ thống. Column/index/schema vật lý được chốt trong feature plan và migration, không nằm trong view này.

## Conceptual Diagram

```mermaid
erDiagram
    DATASET_VERSION ||--o{ CANDLE : contains
    EXPERIMENT ||--|| EXPERIMENT_MANIFEST : freezes
    EXPERIMENT_MANIFEST }o--|| DATASET_VERSION : uses
    EXPERIMENT_MANIFEST }o--|| STRATEGY_DEFINITION : uses
    EXPERIMENT ||--o{ CANDIDATE_DEFINITION : generates
    CANDIDATE_DEFINITION ||--o{ EXECUTION_ATTEMPT : runs_as
    CANDIDATE_DEFINITION ||--o| BACKTEST_RESULT : succeeds_as
    BACKTEST_RESULT ||--o{ TRADE : contains
    BACKTEST_RESULT ||--|| EVALUATION_RESULT : evaluated_as
    EVALUATION_RESULT ||--o| LEADERBOARD_ENTRY : projected_to
    LEADERBOARD_REVISION ||--o{ LEADERBOARD_ENTRY : contains
    NEWS_ITEM ||--o{ SENTIMENT_RESULT : analyzed_as
    OUTBOX_EVENT }o--|| EXPERIMENT : publishes_for
```

## Entity Catalog

| Entity/Value | Ý nghĩa | Owner | Identity/version |
| --- | --- | --- | --- |
| Pair/Timeframe | Canonical market scope | `market-data` | Stable normalized value |
| Candle | OHLCV trong một khoảng | `market-data` | provider + pair + timeframe + openTime |
| Dataset Version | Frozen ordered Candle membership | `market-data` | datasetId + version + checksum |
| Strategy Definition | Plugin descriptor và exact parameters | `strategy-core` | pluginId + version + parameters |
| Composite Definition | Policy và Strategy con | `combination` | compositeId + version/fingerprint |
| Experiment | Identity và runtime lifecycle của một lần chạy | `experiment` | experimentId |
| Experiment Manifest | Immutable input/configuration | `experiment` | manifestVersion + fingerprint |
| Candidate Definition | Strategy/Composite cụ thể được sinh | `search` | candidateId + generationIndex/fingerprint |
| Job/Execution Attempt | Một lần Worker thử chạy Candidate | `backtesting` | jobId + attempt |
| Backtest Result | Kết quả mô phỏng thành công | `backtesting` | resultId; unique theo Candidate/Job policy |
| Trade | Entry/exit/P&L mô phỏng | `backtesting` | tradeId + stable sequence index |
| Evaluation Result | Metrics/version từ Result | `evaluation` | evaluationId + metricVersion |
| Leaderboard Revision/Entry | Top-K projection và stable rank | `leaderboard` | experimentId + revision + evaluationId |
| News Item | Tin đã normalize/deduplicate | `news` | newsId + contentHash/source identity |
| Sentiment Result | Kết quả model immutable | `news` | newsId + contentHash + modelVersion |
| Outbox Event | Durable message chờ publish | Platform persistence | outboxId/messageId |
| Processed Message | Dấu vết idempotency consumer | Platform persistence | consumer + messageId |

## Immutable Experiment Relationships

Một Experiment Manifest đã xác nhận phải tham chiếu chính xác:

- Dataset ID/version/checksum, provider, pair, timeframe, range và Candle count.
- Strategy/Composite ID, version, exact parameters và Combination Policy.
- Backtest assumptions: initial capital, fee, execution price, position/slippage/end-position policy.
- Search algorithm/generator version, seed, Search Space, Stop Conditions, Top-K và Candidate thực tế.
- Evaluation/Ranking version và tie-break rule.
- Sentiment dataset/model/preprocessing version khi có dùng.
- Application version, Git commit và business-relevant configuration.

Thay input tạo Experiment mới; chạy kiểm chứng tạo Reproduction Run mới. Result gốc không bị overwrite.

## Lifecycle and State Machines

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> QUEUED
    QUEUED --> RUNNING
    RUNNING --> COMPLETED
    RUNNING --> FAILED
    RUNNING --> STOP_REQUESTED
    STOP_REQUESTED --> STOPPED
```

```mermaid
stateDiagram-v2
    [*] --> QUEUED
    QUEUED --> RUNNING
    RUNNING --> SUCCEEDED
    RUNNING --> RETRY_SCHEDULED
    RETRY_SCHEDULED --> QUEUED
    RUNNING --> FAILED
    QUEUED --> CANCELLED
```

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> ANALYZING
    ANALYZING --> ANALYZED
    ANALYZING --> PENDING: retryable
    ANALYZING --> FAILED
```

Transition được kiểm tra ở Backend; Frontend không tự đặt status.

## Source of Truth and Storage Roles

PostgreSQL/Supabase là source of truth cho business state, immutable result, Outbox và durable Leaderboard revision. Redis chỉ giữ:

- Redis Streams, consumer group, pending/dead-letter state.
- Cache historical range/Strategy descriptor/Top-K.
- Latest/open Candle, subscriber count, connection/progress snapshot.
- Rate-limit counter hoặc short-lived coordination lock.

Xóa Redis không được làm mất Experiment/Result; cache và pending work phải rebuild/recover được từ PostgreSQL/provider/Outbox.

## CQRS and Event Sourcing Decision

MVP **không dùng full CQRS**: command và query vẫn đi qua cùng Java application/domain model và PostgreSQL. Chỉ Leaderboard có projection/read model riêng vì Top-K cần format đọc nhanh, revision và realtime update. Projection có thể lưu PostgreSQL và cache Redis, đồng thời rebuild từ Evaluation Result.

MVP **không dùng Event Sourcing**: business state không được dựng độc quyền bằng replay toàn bộ event. Immutable Manifest/Result, status/attempt records và Outbox cung cấp audit/provenance cần thiết với độ phức tạp thấp hơn. Outbox là cơ chế reliable publish, không phải event store.

Nếu sau này read/write shape hoặc audit/replay trở thành driver mạnh, nhóm phải tạo ADR mới trước khi mở rộng thành CQRS/Event Sourcing đầy đủ.

## Cross-cutting Rules

- Timestamp dùng ISO-8601 UTC; `evaluationTime` thay cho system clock trong Strategy.
- Price, fee và metric quan trọng dùng decimal/BigDecimal cùng rounding policy được version hóa.
- Canonical serialization sắp field ổn định trước khi tính checksum/fingerprint.
- Foreign key vật lý không cấp quyền ghi chéo module; owner output port vẫn là boundary.
- Migration trong Git là nguồn schema; không sửa demo database chỉ bằng Dashboard.
- Retention không được xóa Dataset/Strategy/Result đang được Experiment tham chiếu.
