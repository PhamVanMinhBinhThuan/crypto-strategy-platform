# Database Data Dictionary

**Trạng thái**: Baseline 0.1 đã áp dụng; DB setup v2 đang chờ review/apply
**Cập nhật**: 2026-08-28

Đây là contract vật lý của baseline và forward migration DB setup v2.
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
| `user_strategy` | User Strategy ID PK, `owner_user_id` FK → `auth.users`, kind, name/description, status, archive/created/updated time | CK kind/status/archive state; partial UQ owner+active name không phân biệt hoa thường; owner listing index |
| `user_strategy_version` | User Strategy version ID PK, parent FK, version number, frozen kind/plugin/parameters hoặc combination policy, lifecycle, fingerprint, publish/created time | UQ parent+version/fingerprint; CK source theo kind và publish state; published row được trigger bảo vệ bất biến |
| `user_strategy_component` | User Strategy version FK, position, shared plugin version FK, exact parameters, optional weight | PK version+position; UQ version+plugin; chỉ composite draft được sửa, published components bất biến |

Composite có ít nhất hai component được kiểm tra trong cùng application
transaction vì CHECK constraint đơn bảng không thể đếm row.

`strategy_version` vẫn là catalog plugin dùng chung. Ba bảng `user_strategy*` là
cấu hình riêng của user; chúng không chứa Java class, password hoặc token.

## Schema `experiment`

| Table | Columns chính | Constraints và indexes |
| --- | --- | --- |
| `experiment` | `experiment_id` PK, `owner_user_id uuid` FK → `auth.users`, optional `derived_from_experiment_id` self-FK, `name`, `status`, started/completed time, failure code/message | CK lifecycle status; indexes owner+created_at và status+created_at |
| `experiment_manifest` | `experiment_id` PK/FK, `manifest_version`, `dataset_version_id` FK, `dataset_provenance jsonb`, typed `strategy_provenance jsonb`, optional source User Strategy version FK, backtest/search/evaluation config, optional sentiment config, software version, git commit, fingerprint | Hai provenance snapshot là bắt buộc và bất biến khi queue; Strategy snapshot chứa typed ID/version, policy version và `strategy-v1` fingerprint |
| `candidate_definition` | `candidate_id` PK, `experiment_id` FK, `generation_index`, `definition jsonb`, `generator_state jsonb?`, fingerprint | UQ experiment+index và experiment+fingerprint |
| `job` | Job ID PK, Experiment FK, optional Candidate, type/status, correlation ID, progress, best score, lifecycle/failure timestamps | FK Candidate+Experiment; CK Search/Backtest shape và progress; UQ Search per Experiment/Backtest per Candidate; recovery/listing indexes |
| `execution_attempt` | `attempt_id` PK, `job_id` + `candidate_id` composite FK, `attempt_no`, status, worker/times, failure fields, retryable | UQ job+attempt; CK attempt > 0/status; indexes candidate và status+next_retry_at |
| `backtest_result` | `backtest_result_id` PK, `experiment_id`, `candidate_id`, `successful_attempt_id`, initial/final capital, result fingerprint, completed time, `reproduces_result_id?` self-FK | UQ candidate; composite FK buộc Candidate và successful Attempt thuộc cùng lineage |
| `trade` | `trade_id` PK, result FK, sequence, side, entry/exit time, price, quantity, fee, profit_loss | UQ result+sequence; CK side/time/nonnegative execution values; index result+entry_time |
| `evaluation_result` | `evaluation_result_id` PK, `experiment_id`, result FK, metric/ranking version, total return, win rate, maximum drawdown, number of trades, overall score, evaluated time | Composite FK buộc Evaluation và Backtest Result cùng Experiment; UQ result+metric_version |
| `leaderboard_revision` | revision ID PK, experiment FK, `revision_no bigint`, `top_k` | UQ experiment+revision; CK values > 0; descending latest-revision index |
| `leaderboard_entry` | `experiment_id`, revision FK, `rank`, evaluation FK, score snapshot | Composite FK buộc Revision và Evaluation cùng Experiment; PK revision+rank; UQ revision+evaluation |

Lifecycle values:

- Experiment: `CREATED, QUEUED, RUNNING, COMPLETED, FAILED, STOP_REQUESTED, STOPPED`.
- Job: `QUEUED, RUNNING, RETRY_SCHEDULED, SUCCEEDED, FAILED, CANCEL_REQUESTED, CANCELLED`.
- Attempt: `QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED`; `RETRY_SCHEDULED` chỉ thuộc Job.

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

`user_profile` không phải bảng đăng nhập. Password hash, access token, refresh token
và session do Supabase Auth quản lý trong schema `auth`; business schema chỉ lưu UUID
để xác định owner. Browser không được cấp quyền đọc/ghi trực tiếp các bảng này.

## Invariant do Backend kiểm tra trong transaction

- Successful Attempt phải thuộc cùng Candidate với Backtest Result.
- Evaluation đưa vào Leaderboard phải thuộc đúng Experiment của revision.
- Candle thêm vào Dataset phải đúng provider, pair, timeframe và range.
- Composite Version phải có tối thiểu hai component.
- User Strategy version phải cùng kind với parent; chỉ version `PUBLISHED` mới được
  Experiment tham chiếu và phải có cùng owner với Experiment.
- Repository/API phải lọc User Strategy theo `owner_user_id` và Job theo
  `job → experiment → owner_user_id`; ID client gửi không tự cấp quyền.
- State transition của Job được application service kiểm tra; database giới hạn
  tập trạng thái và quan hệ Candidate/Experiment hợp lệ.
- Bảng bất biến không được update/delete sau khi đã được tham chiếu.

Các invariant này liên quan nhiều row hoặc quan hệ đa hình nên không ép bằng
CHECK constraint đơn giản trong baseline migration.

## Trạng thái migration

Các lựa chọn ảnh hưởng schema baseline đã được chốt trong
[Database Decisions](decisions.md). Baseline `20260827000100` đã áp dụng và không
được sửa. DB setup v2 dùng forward migration `20260828000100`; chỉ cập nhật trạng
thái `Verified` sau khi dry-run/apply/lint/test thật hoàn tất.
