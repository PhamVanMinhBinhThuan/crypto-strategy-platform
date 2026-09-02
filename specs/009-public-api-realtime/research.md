# Nghiên cứu và quyết định F-009

## 1. Public transport boundary

**Quyết định**: giữ REST dưới `/api/v1` và native WebSocket tại `/ws`, dùng JSON
versioned envelope.

**Lý do**: khớp `docs/api/openapi.yaml`, `docs/api/websocket-events.md` và ADR-0004;
browser có một boundary thống nhất, không phụ thuộc provider hoặc database.

**Phương án khác**: thêm GraphQL, STOMP hoặc broker mới. Không chọn vì mở rộng protocol
và deployment surface ngoài MVP, không giải quyết thêm yêu cầu user.

## 2. Authentication cho WebSocket

**Quyết định**: authenticated REST request cấp one-time WebSocket ticket ngắn hạn; ticket
gắn user, origin và expiry, bị vô hiệu ngay sau handshake. Access token dài hạn không
được đưa vào URL.

**Lý do**: browser WebSocket không cho đặt tùy ý `Authorization` header; ticket tránh
query-string leakage và phù hợp ADR-0011/ADR-0004. Handshake phải kiểm tra origin và
ticket trước khi tạo subscription.

**Phương án khác**: cookie phiên dài hạn hoặc access token trong query string. Cookie
phụ thuộc thêm session bridge; query string dễ lọt vào log/referrer nên bị loại.

## 3. Snapshot và event sequencing

**Quyết định**: subscription registration tạo synchronization marker; server đăng ký,
đọc snapshot boundary và chỉ phát notification sau marker. Mỗi event giữ `eventId` và
resource revision/timestamp phù hợp; client đọc REST snapshot và bỏ qua event cũ/duplicate.

**Lý do**: REST snapshot là source of truth, còn F-007 notifications transient. Cách này
đóng race giữa lúc đọc snapshot và lúc subscribe mà không yêu cầu exactly-once delivery.

**Phương án khác**: client gọi REST trước rồi subscribe. Cách này có khoảng mất event nếu
state thay đổi giữa hai thao tác; chỉ dùng được khi có backfill marker, nên không đủ làm
contract mặc định.

### 3.1. Authentication hết hạn trên connection

**Quyết định**: MVP không reauthenticate ngay trong WebSocket. Connection đóng bằng code
ổn định khi đến thời điểm sớm hơn giữa JWT gốc hết hạn và maximum connection lifetime.
Client refresh session qua auth flow bình thường, xin one-time ticket mới, reconnect,
resubscribe và reconcile bằng REST snapshot.

**Lý do**: refresh token không đi qua WebSocket, connection không phải giữ thêm auth command
hoặc thay principal giữa session, và người dùng vẫn không phải đăng nhập lại khi refresh
session còn hợp lệ.

**Phương án khác**: gửi access/refresh token bằng WebSocket command để thay identity trên
connection hiện tại. Không chọn vì mở rộng credential surface và làm lifecycle authorization,
subscription cleanup khó kiểm chứng hơn trong MVP.

## 4. Giới hạn và backpressure

**Quyết định**: mặc định bốn Candle subscriptions, bốn workload subscriptions, message
64 KiB, 30 commands/10 giây/connection, heartbeat 30 giây và timeout 90 giây; tất cả
được cấu hình và kiểm thử, không hard-code trong browser.

**Lý do**: bốn chart là yêu cầu MVP; giới hạn workload và payload bảo vệ connection; coalesce
được update trung gian nhưng phải giữ close/terminal/revision mới nhất.

**Phương án khác**: không giới hạn hoặc để UI tự điều tiết. Không chọn vì dễ tạo connection
chậm, memory tăng và làm mất khả năng dự đoán của acceptance test.

## 5. Ownership và idempotency

**Quyết định**: API nhận authenticated UUID, truy owner qua parent chain; missing/cross-owner
map cùng inaccessible outcome. Canonical request hash scope theo owner và operation; replay
cùng hash trả outcome gốc, khác hash trả conflict.

**Lý do**: đúng ADR-0011/0012, F-005 contract và error catalog; tránh enumeration và duplicate
business effect.

**Phương án khác**: tin resource ID do client gửi hoặc scope idempotency toàn hệ thống. Cả hai
đều sai isolation khi có nhiều user.

## 6. Dependency readiness

**Quyết định**: chỉ đánh dấu operation functional khi application boundary của capability
phụ thuộc đã sẵn sàng; endpoint không được trả success giả cho Search/Candle/Sentiment
operation chưa có owner implementation.

**Lý do**: F-003 còn work chưa hoàn tất, F-008 còn hardening và module Search đang trống;
giữ spec đầy đủ nhưng không che khuất dependency gate.

**Phương án khác**: trả fixture/mock như production result. Không chọn vì vi phạm evidence
governance và reproducibility.

## 7. Identity và transaction boundary cho Backtest đơn lẻ

**Quyết định**: bổ sung F-005 aggregate `StandaloneBacktest` với `BacktestId` riêng,
được backing bởi single-run Experiment, một immutable Candidate và một Backtest Job.
Toàn bộ graph, Outbox và idempotency outcome được accept trong một database transaction.

**Lý do**: F-006 worker đã thực thi an toàn từ frozen Experiment graph; reuse graph giữ
queue contract và Result lineage ổn định, trong khi typed Backtest identity tránh việc
giả Candidate thành public Backtest resource. Quyết định được ghi tại ADR-0015.

**Phương án khác**: dùng Candidate ID làm Backtest ID hoặc đổi Job sang parent mới. Cách
đầu sai semantics; cách sau tạo breaking queue/Attempt/worker migration không cần thiết.
