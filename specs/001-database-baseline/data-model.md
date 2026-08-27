# Phase 1 Data Model: Database Baseline

## Boundaries

| Schema | Owner | Tables |
|---|---|---|
| `market` | Market Data | `asset`, `trading_pair`, `candle`, `dataset_version`, `dataset_candle` |
| `strategy` | Strategy | `strategy_version`, `composite_version`, `composite_component` |
| `experiment` | Experiment | `experiment`, `experiment_manifest`, `candidate_definition`, `execution_attempt`, `backtest_result`, `trade`, `evaluation_result`, `leaderboard_revision`, `leaderboard_entry` |
| `news` | News/Sentiment | `news_item`, `news_item_asset`, `sentiment_result` |
| `platform` | Platform | `user_profile`, `outbox_event`, `processed_message`, `idempotency_record` |

Foreign key giữa schema chỉ biểu diễn quan hệ dữ liệu; nó không chuyển quyền ghi từ module owner này sang module khác.

## Identity and common types

- Public domain IDs dùng uppercase ULID dạng `varchar(26)`.
- User identity dùng `uuid` và tham chiếu `auth.users(id)`.
- Thời gian dùng `timestamptz`; structured snapshots dùng `jsonb`.
- Price, quantity, fee và money dùng `numeric(30,12)`.
- Rate, confidence, polarity, score và metrics dùng `numeric(20,10)`.
- Timeframe canonical: `1m`, `5m`, `15m`, `30m`, `1h`, `2h`, `4h`, `1d`.

## Entities and relationships

### Market

- **Asset**: symbol, name, active state. Symbol là duy nhất.
- **Trading Pair**: base/quote Asset, symbol, active state. Base khác quote và mỗi cặp asset là duy nhất.
- **Candle**: provider, Trading Pair, timeframe, time range, OHLCV. Identity duy nhất là `(provider, trading_pair_id, timeframe, open_time)`; OHLCV không âm và high/low hợp lệ.
- **Dataset Version**: provider/pair/timeframe, normalization version, range, candle count, SHA-256 checksum. Đại diện snapshot dữ liệu bất biến.
- **Dataset Candle**: membership có thứ tự giữa Dataset Version và Candle. Cả sequence và Candle đều không được lặp trong một dataset.

### Strategy

- **Strategy Version**: snapshot plugin/version, parameter schema/default, supported signals và fingerprint. `(plugin_id, version)` và fingerprint là duy nhất.
- **Composite Version**: snapshot policy/version/parameters và fingerprint.
- **Composite Component**: danh sách Strategy Version có thứ tự, override và optional positive weight. Một Strategy Version không lặp trong cùng composite.

Quy tắc composite phải có ít nhất hai component được persistence service kiểm tra trong transaction vì không thể biểu diễn bằng row-local constraint.

### Platform identity

- **User Profile**: optional profile cho một Supabase Auth user. Xóa Auth user sẽ xóa profile; dữ liệu Experiment không cascade theo owner.

### Experiment and reproducibility

- **Experiment**: owner bắt buộc, optional parent Experiment, name, runtime status và timestamps/failure details.
- **Experiment Manifest**: quan hệ 1:1 với Experiment; cố định Dataset Version, strategy reference/configs, software/git version và fingerprint. Fingerprint không unique để cho phép rerun cùng cấu hình.
- **Candidate Definition**: generation index và fingerprint duy nhất trong một Experiment.
- **Execution Attempt**: một Candidate có nhiều lần chạy; `(job_id, attempt_no)` duy nhất.
- **Backtest Result**: tối đa một result cho Candidate, tham chiếu successful attempt và optional result gốc được tái lập.
- **Trade**: chuỗi BUY/SELL có thứ tự trong Backtest Result; sequence không lặp.
- **Evaluation Result**: nhiều metric version cho một Backtest Result; mỗi version chỉ xuất hiện một lần.
- **Leaderboard Revision**: snapshot leaderboard có revision tăng trong Experiment.
- **Leaderboard Entry**: rank và Evaluation Result không lặp trong revision.

### News and sentiment

- **News Item**: source identity, URL, metadata, optional content, content hash, publication/crawl time và analysis status. URL và available source item identity được deduplicate.
- **News Item Asset**: liên kết News Item–Asset với optional relevance score `[0,1]`.
- **Sentiment Result**: kết quả cho đúng `(news item, content hash, model version)`, gồm label, confidence `[0,1]`, polarity `[-1,1]` và analyzed time.

### Durable delivery and idempotency

- **Outbox Event**: event envelope, payload, publish state/attempts; message ID duy nhất.
- **Processed Message**: identity `(consumer_name, message_id)` và expiry để consumer deduplicate bền vững.
- **Idempotency Record**: identity `(user_id, scope, idempotency_key)`, request hash và response snapshot/expiry.

## State transitions

### Experiment

`CREATED → QUEUED → RUNNING → COMPLETED | FAILED | STOP_REQUESTED → STOPPED`

Database giới hạn tập giá trị; persistence feature sẽ kiểm tra transition hợp lệ và tính nhất quán của timestamps/failure fields trong transaction.

### Execution Attempt

`QUEUED → RUNNING → SUCCEEDED | FAILED | RETRY_SCHEDULED → QUEUED`, hoặc `CANCELLED`.

Database giới hạn tập giá trị. Persistence/worker chịu trách nhiệm retry policy và chỉ gắn Backtest Result với successful attempt của cùng Candidate.

### News analysis

`PENDING → ANALYZING → ANALYZED | FAILED`; retry có thể đưa `FAILED → ANALYZING`.

## Invariant ownership

**Database-enforced**: format ID, foreign keys, required values, enum-like checks, numeric ranges, row-local consistency, duplicate identities và documented indexes.

**Application transaction-enforced later**: snapshot immutability; composite có ít nhất hai component; Dataset Candle khớp provider/pair/timeframe/range của Dataset Version; successful attempt thuộc đúng Candidate và có status `SUCCEEDED`; Leaderboard Entry thuộc đúng Experiment; lifecycle transitions; retention không xóa reproducibility data.
