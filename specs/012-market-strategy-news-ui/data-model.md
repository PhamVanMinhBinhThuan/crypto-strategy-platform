# Data Model: Market, Strategy and News UI

Các model dưới đây là frontend view/state model, không phải durable business authority.

## 1. Market Selection

| Field | Rules |
| --- | --- |
| `pair` | Canonical supported `BASE/QUOTE`; URL-safe encode |
| `panels` | 1–4 panel có stable ID và timeframe trong versioned Market catalog |
| `startTime`, `endTime` | UTC interval, start inclusive/end exclusive |
| `generation` | Monotonic local request identity; late generation không commit |

Invalid URL value được canonicalize về default đã công bố và replace URL, không loop navigation.

## 2. Candle View

| Field | Rules |
| --- | --- |
| `identity` | `(pair, timeframe, openTime)` |
| `openTime`, `closeTime` | Valid UTC instants; ordered ascending |
| `open`, `high`, `low`, `close`, `volume` | Canonical exact strings; validate finite/nonnegative rules từ contract |
| `closed` | Closed Candle không bị open update cũ ghi đè |

Reducer dedupe theo identity, kiểm tra selection, giữ newest canonical value và bounded window. Chart
projection có thể dùng number tạm thời nhưng source string không đổi.

## 3. Transport và Provider State

```text
Transport: DISCONNECTED -> CONNECTING -> CONNECTED -> RECONNECTING
Provider:  trạng thái public từ MARKET_CONNECTION_STATUS_CHANGED, độc lập transport
View:      live/degraded/unavailable được dẫn xuất từ hai state trên
```

State giữ `lastSuccessfulEventAt`, subscription confirmation và safe recovery action. Connection
state không xóa Candle snapshot.

## 4. Strategy Views

### System Strategy Descriptor

Identity/version, display metadata, category, signals, lookback, parameter descriptors,
cross-parameter rules và descriptor fingerprint. Descriptor immutable trong một version.

### Private Strategy Summary/Detail

Owner-safe opaque identity, kind `SINGLE|COMPOSITE`, status `ACTIVE|ARCHIVED`, latest immutable
version và timestamps. Missing/foreign dùng chung inaccessible presentation.

### Strategy Draft

| Field | Rules |
| --- | --- |
| `name`, `description` | Bounded text theo public contract |
| `kind`, `source` | Discriminated SINGLE hoặc COMPOSITE |
| `parameters` | Canonical string/boolean draft keyed by descriptor name |
| `expectedLatestVersionNo` | Bắt buộc khi tạo next version |
| `issues` | Field/global safe validation messages |
| `mutationState` | `IDLE`, `SUBMITTING`, `RECONCILING`, `SUCCEEDED`, `FAILED` |

Published version không chuyển về editable; edit bắt đầu draft version mới. Mutation success/timeout/
conflict đều kết thúc bằng authoritative detail/list reload.

## 5. News Query và Page

Selected analysis statuses, cursor và request generation tạo query identity. Page
items newest-first với server tie-break; append dedupe `newsId`, response khác query generation bị bỏ.

## 6. News/Sentiment View

News giữ opaque ID, title, source, safe external URL, published UTC instant, related asset IDs và
analysis status. Sentiment state:

```text
PENDING/ANALYZING -> ANALYZED
                  -> FAILED_RETRYABLE
                  -> FAILED
```

Chỉ `ANALYZED` kèm payload hợp lệ hiển thị label/confidence/polarity. Các state khác giữ News readable
và hiện degraded/pending explanation. Không có content/summary/provenance hay aggregate analytics
nếu public DTO không cung cấp; không có internal model name/hash trong browser view.

## 7. Shared Async State

Mỗi resource độc lập dùng `idle|loading|ready|empty|error`, request generation, last successful data
và safe `PublicError`. Retryable failure có action hữu hạn; unauthorized giao cho F-011 session
lifecycle; error không chứa raw payload/stack/token.
