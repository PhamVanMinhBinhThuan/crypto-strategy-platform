# Các quyết định thiết kế database

**Trạng thái**: Proposed — Database baseline 0.1  
**Cập nhật**: 2026-08-21

File này ghi lại các lựa chọn ban đầu để thiết kế ERD và migration. Sau khi cả
nhóm review, từng mục có thể được giữ nguyên, sửa hoặc chuyển sang `Accepted`.
Migration đã chạy trên môi trường dùng chung không được sửa lại; thay đổi sau đó
phải dùng migration mới.

## Tổng hợp

| ID | Vấn đề | Lựa chọn đề xuất |
| --- | --- | --- |
| DB-01 | Tổ chức schema | Năm schema theo bounded context |
| DB-02 | Kiểu ID | ULID lưu bằng `varchar(26)` |
| DB-03 | Candle và Dataset | Lưu closed Candle một lần, Dataset freeze membership |
| DB-04 | Strategy parameters | Column ổn định kết hợp `jsonb` |
| DB-05 | Backtest execution | Tách Candidate, Attempt và Result |
| DB-06 | Leaderboard | Lưu các revision khi Top-K thay đổi |
| DB-07 | News mapping | News liên kết với Asset |
| DB-08 | Sentiment | Lưu `confidence` và `polarity_score` riêng |
| DB-09 | Retention | Không xóa dữ liệu tái lập; cleanup dữ liệu kỹ thuật |
| DB-10 | Database access | Chỉ Backend/Worker truy cập business data |
| DB-11 | Migration | Chỉ dùng Supabase CLI |
| DB-12 | Kiểu dữ liệu chung | `timestamptz`, `numeric`, `text + check` |

## DB-01 — Tổ chức PostgreSQL schema

**Chọn gì?**

```text
market      Asset, Trading Pair, Candle, Dataset
strategy    Strategy Version, Composite Strategy
experiment  Experiment, Search, Backtest, Evaluation, Leaderboard
news        News Item, Sentiment Result
platform    Outbox, processed message, idempotency
```

**Vì sao?** Phù hợp các bounded context trong docs, thể hiện ownership rõ và
không để business table nằm trong `public` mặc định của Supabase.

**Lưu ý**: Không tạo một schema cho từng Java module. Foreign key chéo schema
không đồng nghĩa module được phép ghi dữ liệu của nhau.

## DB-02 — Public ID

**Chọn gì?** Application tạo ULID viết hoa; PostgreSQL lưu bằng `varchar(26)` và
có `CHECK` đúng định dạng Crockford Base32.

**Vì sao?** Khớp API convention, dễ đọc khi debug và không cần PostgreSQL
extension. `text` quá lỏng, còn `char(26)` có hành vi padding.

**Lưu ý**: Database vẫn phải có primary key hoặc unique constraint để ngăn trùng.

## DB-03 — Candle và Dataset

**Chọn gì?**

- Chỉ persist closed Candle, không lưu mọi tick hoặc open Candle làm lịch sử.
- Một Candle chuẩn hóa chỉ lưu một lần.
- Dataset Version bất biến và có membership tới chính xác các Candle đã dùng.
- Dataset lưu thêm range, candle count và checksum.

**Vì sao?** Tránh nhân bản OHLCV nhưng vẫn tái lập chính xác Backtest khi dữ
liệu provider được backfill hoặc có gap.

**Lưu ý**: Cần bulk insert và index membership theo Dataset + sequence.

## DB-04 — Strategy và Composite Strategy

**Chọn gì?** ID, version, type và fingerprint dùng column; parameter schema,
default và exact values dùng `jsonb`. Composite Strategy có bảng component theo
thứ tự, tham chiếu Strategy Version và lưu weight/parameter override.

**Vì sao?** Mỗi plugin có parameters khác nhau, nhưng các field cần query,
unique hoặc foreign key vẫn phải có column rõ ràng.

**Lưu ý**: Java Strategy validator xử lý rule như `fastPeriod < slowPeriod`.

## DB-05 — Candidate, Attempt và Result

**Chọn gì?**

```text
Experiment      1 ── N Candidate
Candidate       1 ── N Execution Attempt
Candidate       1 ── 0..1 accepted Backtest Result
Backtest Result 1 ── N Trade
Backtest Result 1 ── N Evaluation Result theo metric version
```

**Vì sao?** Retry hoặc duplicate delivery không được tạo Candidate/Result mới
ngoài ý muốn và không được overwrite kết quả thành công.

**Lưu ý**: Reproduce tạo run/result mới có reference tới bản gốc.

## DB-06 — Leaderboard revision

**Chọn gì?** Khi Top-K thực sự thay đổi, tạo một revision bất biến và các entry
của revision đó. Redis sau này chỉ cache revision mới nhất.

**Vì sao?** Hỗ trợ audit, realtime ordering và rebuild cache với chi phí nhỏ vì
chỉ lưu Top-K.

**Lưu ý**: Không tạo revision nếu chỉ có progress thay đổi mà thứ hạng giữ nguyên.

## DB-07 — News và Asset

**Chọn gì?** Có `market.asset`, `market.trading_pair` và bảng nhiều-nhiều
`news.news_item_asset`. News liên kết với BTC/ETH thay vì liên kết trực tiếp với
`BTC/USDT` hoặc `ETH/USDT`.

**Vì sao?** Một bài News có thể liên quan nhiều asset và không chỉ thuộc một cặp
giao dịch cụ thể.

**Lưu ý**: Filter News theo pair được suy ra từ base/quote asset của pair.

## DB-08 — Sentiment Result

**Chọn gì?** Lưu riêng:

```text
label          POSITIVE | NEUTRAL | NEGATIVE
confidence     0..1
polarity_score -1..1
model_version
analyzed_at
```

**Vì sao?** `confidence` là độ tự tin vào label; `polarity_score` là hướng và
cường độ sentiment. Một field `score` không thể hiện rõ hai ý nghĩa này.

**Lưu ý**: OpenAPI và API examples đã dùng cùng hai field này; khi implement
News API phải giữ nguyên ý nghĩa và khoảng giá trị đã quy định.

## DB-09 — Retention

**Chọn gì?**

| Dữ liệu | Thời gian giữ đề xuất |
| --- | --- |
| Dataset, Candle, Strategy và Result được tham chiếu | Không tự động xóa |
| Experiment, Trade, Evaluation và Leaderboard | Không tự động xóa trong MVP |
| Outbox đã publish | 30 ngày |
| Outbox chưa publish | Không tự động xóa |
| Processed Message | 30 ngày |
| HTTP Idempotency Record | 7 ngày |
| News metadata và Sentiment Result | Không tự động xóa trong MVP |
| Full News content | Tối đa 30 ngày nếu license không cho giữ lâu hơn |

**Vì sao?** Ưu tiên reproducibility nhưng không giữ vô hạn dữ liệu kỹ thuật có
thể cleanup. News content còn phụ thuộc điều khoản của provider.

**Lưu ý**: Các con số retention cần được feature spec hoặc vận hành xác nhận.

## DB-10 — Quyền truy cập database

**Chọn gì?** Chỉ Backend và Worker truy cập business table. Không cấp quyền cho
Supabase `anon`/`authenticated`; không đưa `service_role` key vào browser. RLS
chỉ là lớp bảo vệ bổ sung.

**Vì sao?** Giữ business validation trong Java API và tránh hai đường ghi dữ liệu.

**Lưu ý**: Supabase Auth/Storage/Realtime chỉ được thêm khi có quyết định riêng.

## DB-11 — Quản lý migration

**Chọn gì?** Chỉ dùng `supabase/migrations/` và Supabase CLI; không duy trì thêm
Flyway migration trong `modules/persistence`.

**Vì sao?** Một lịch sử migration giúp local, CI và remote không bị lệch nhau.

**Lưu ý**: Migration phải chạy trước khi deploy API/Worker cần schema mới.

## DB-12 — Quy ước kiểu dữ liệu

**Chọn gì?**

- Timestamp: `timestamptz`, trao đổi dưới dạng ISO-8601 UTC.
- Giá, tiền, fee và metric: `numeric` với precision/scale phù hợp.
- Status: `text + check constraint`, không dùng PostgreSQL enum.
- Không dùng soft delete mặc định.
- Chỉ có `updated_at` trên table thật sự cho phép update.

**Vì sao?** Tránh sai số float, nhầm timezone, migration enum khó và query
soft-delete phức tạp không cần thiết cho MVP.

## Cách thay đổi quyết định

Khi feature spec hoặc kiểm thử cho thấy một lựa chọn không phù hợp:

1. Cập nhật mục tương ứng trong file này và ghi lý do.
2. Cập nhật ERD và data dictionary.
3. Tạo forward migration mới; không sửa migration đã áp dụng.
4. Chuyển trạng thái thành `Accepted` sau khi nhóm review.
