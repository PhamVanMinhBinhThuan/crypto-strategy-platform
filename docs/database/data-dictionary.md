# Database Data Dictionary

**Trạng thái**: Draft — Database baseline 0.1  
**Cập nhật**: 2026-08-22

Đây là contract vật lý dự kiến để viết migration, chưa phải schema đã deploy.
`PK`, `FK`, `UQ` và `CK` lần lượt là primary key, foreign key, unique và
check constraint.

## Quy tắc chung

- ID: ULID `varchar(26)`; timestamp: `timestamptz`.
- Price, quantity, money, P&L và fee: `numeric(30,12)`.
- Rate, metric và score: `numeric(20,10)`.
- Status dùng `text + CHECK`, không dùng PostgreSQL enum.
- Config bất biến dùng `jsonb`, được application validate theo version.
- Bảng có ID dùng `created_at timestamptz NOT NULL DEFAULT now()`; chỉ thêm
  `updated_at` cho bảng thực sự cho phép update.

## Schema `market`

| Table | Columns chính | Constraints và indexes |
| --- | --- | --- |
| `asset` | `asset_id` PK, `symbol`, `name?`, `active boolean` | UQ symbol; CK symbol viết hoa |
| `trading_pair` | `trading_pair_id` PK, `base_asset_id` FK, `quote_asset_id` FK, `symbol`, `active` | UQ base+quote và symbol; CK base khác quote |
| `candle` | `candle_id` PK, `provider`, `trading_pair_id` FK, `timeframe`, `open_time`, `close_time`, OHLC và `volume numeric(30,12)` | UQ provider+pair+timeframe+open_time; CK timeframe theo ADR-0003, OHLC/volume không âm, high/low hợp lệ, close_time > open_time; index pair+timeframe+open_time |
| `dataset_version` | `dataset_version_id` PK, `version`, provider/pair/timeframe, `normalization_version`, `range_start`, `range_end`, `candle_count`, `checksum` | UQ checksum; CK timeframe theo ADR-0003, range hợp lệ và count > 0 |
| `dataset_candle` | `dataset_version_id` FK, `sequence_no integer`, `candle_id` FK | PK dataset+sequence; UQ dataset+candle; CK sequence >= 0; index candle |

Chỉ closed Candle được persist. Dataset freeze đúng thứ tự Candle và không sửa
membership sau khi tạo.

## Schema `strategy`

| Table | Columns chính | Constraints và indexes |
| --- | --- | --- |
| `strategy_version` | `strategy_version_id` PK, `plugin_id`, `version`, `display_name`, `parameter_schema jsonb`, `default_parameters jsonb`, `supported_signals jsonb`, `fingerprint` | UQ plugin+version và fingerprint |
| `composite_version` | `composite_version_id` PK, `composite_id`, `version`, `display_name`, policy ID/version/parameters, `fingerprint` | UQ composite+version và fingerprint |
| `composite_component` | `composite_version_id` FK, `position`, `strategy_version_id` FK, `parameter_overrides jsonb`, `weight numeric?` | PK composite+position; UQ composite+strategy; CK position >= 0 và weight > 0 khi có |

Composite có ít nhất hai component được kiểm tra trong cùng application
transaction vì CHECK constraint đơn bảng không thể đếm row.

## Schema `experiment`

| Table | Columns chính | Constraints và indexes |
| --- | --- | --- |
| `experiment` | `experiment_id` PK, `owner_user_id uuid` FK → `auth.users`, optional `derived_from_experiment_id` self-FK, `name`, `status`, started/completed time, failure code/message | CK lifecycle status; indexes owner+created_at và status+created_at |
| `experiment_manifest` | `experiment_id` PK/FK, `manifest_version`, `dataset_version_id` FK, strategy kind/ref/version, strategy parameters, backtest/search/evaluation config, optional sentiment config, software version, git commit, fingerprint | Index fingerprint; CK kind SINGLE/COMPOSITE; fingerprint được phép lặp khi reproduce |
| `candidate_definition` | `candidate_id` PK, `experiment_id` FK, `generation_index`, `definition jsonb`, `generator_state jsonb?`, fingerprint | UQ experiment+index và experiment+fingerprint |
| `execution_attempt` | `attempt_id` PK, `job_id`, `candidate_id` FK, `attempt_no`, status, worker/times, failure fields, retryable | UQ job+attempt; CK attempt > 0/status; indexes candidate và status+next_retry_at |
| `backtest_result` | `backtest_result_id` PK, `candidate_id` FK, `successful_attempt_id` FK, initial/final capital, result fingerprint, completed time, `reproduces_result_id?` self-FK | UQ candidate; CK capital >= 0 |
| `trade` | `trade_id` PK, result FK, sequence, side, entry/exit time, price, quantity, fee, profit_loss | UQ result+sequence; CK side/time/nonnegative execution values; index result+entry_time |
| `evaluation_result` | `evaluation_result_id` PK, result FK, metric/ranking version, total return, win rate, maximum drawdown, number of trades, overall score, evaluated time | UQ result+metric_version; CK win rate 0..1, drawdown/trade count >= 0; index score |
| `leaderboard_revision` | revision ID PK, experiment FK, `revision_no bigint`, `top_k` | UQ experiment+revision; CK values > 0; descending latest-revision index |
| `leaderboard_entry` | revision FK, `rank`, evaluation FK, score snapshot | PK revision+rank; UQ revision+evaluation; CK rank > 0 |

Lifecycle values:

- Experiment: `CREATED, QUEUED, RUNNING, COMPLETED, FAILED, STOP_REQUESTED, STOPPED`.
- Attempt: `QUEUED, RUNNING, RETRY_SCHEDULED, SUCCEEDED, FAILED, CANCELLED`.

## Schema `news`

| Table | Columns chính | Constraints và indexes |
| --- | --- | --- |
| `news_item` | `news_item_id` PK, source/source item ID, URL, title, optional summary/content, content hash, published/crawled time, analysis status | UQ URL; partial UQ source+source ID; indexes published_at và status+crawled_at; content tuân theo retention/license |
| `news_item_asset` | news FK, asset FK, optional `relevance_score numeric` | PK news+asset; CK relevance 0..1; index asset |
| `sentiment_result` | result ID PK, news FK, content hash, model version, label, confidence, polarity score, analyzed time | UQ news+hash+model; CK label, confidence 0..1, polarity -1..1; index news+analyzed_at |

Analysis status là `PENDING, ANALYZING, ANALYZED, FAILED`; label là
`POSITIVE, NEUTRAL, NEGATIVE`. Kết quả model mới tạo version mới, không
overwrite kết quả cũ.

## Schema `platform`

| Table | Columns chính | Constraints và indexes |
| --- | --- | --- |
| `user_profile` | `user_id uuid` PK/FK → `auth.users`, optional `display_name`, created/updated time | CK display name không rỗng khi có |
| `outbox_event` | outbox ID PK, `message_id`, aggregate type/ID, event type/version, payload/headers JSONB, occurred/published time, attempts, last error | UQ message ID; CK attempts >= 0; partial index unpublished by occurred_at |
| `processed_message` | `consumer_name`, `message_id`, processed/expires time | PK consumer+message; index expires_at |
| `idempotency_record` | `user_id uuid` FK → `auth.users`, `scope`, `idempotency_key`, request hash, optional resource type/ID, response status/body, created/expires time | PK user+scope+key; CK HTTP status 100..599; indexes user và expires_at |

Outbox aggregate reference là logic reference vì event có thể thuộc nhiều
schema. Không tạo foreign key đa hình.

## Invariant do Backend kiểm tra trong transaction

- Successful Attempt phải thuộc cùng Candidate với Backtest Result.
- Evaluation đưa vào Leaderboard phải thuộc đúng Experiment của revision.
- Candle thêm vào Dataset phải đúng provider, pair, timeframe và range.
- Composite Version phải có tối thiểu hai component.
- Bảng bất biến không được update/delete sau khi đã được tham chiếu.

Các invariant này liên quan nhiều row hoặc quan hệ đa hình nên không ép bằng
CHECK constraint đơn giản trong baseline migration.

## Trạng thái trước migration

Các lựa chọn ảnh hưởng schema baseline đã được chốt trong
[Database Decisions](decisions.md). Thay đổi sau khi migration được áp dụng phải
dùng forward migration mới.
