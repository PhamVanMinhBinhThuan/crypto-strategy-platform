# Kế hoạch triển khai: Public API và Realtime Delivery

**Branch**: `009-public-api-realtime` | **Date**: 2026-09-01 | **Spec**: [spec.md](spec.md)

**Input**: Đặc tả F-009 tại `specs/009-public-api-realtime/spec.md`

## Tóm tắt

F-009 bổ sung public boundary thống nhất cho các capability F-003 đến F-008: REST
đọc dữ liệu dùng chung, thao tác owner-scoped, idempotency/error mapping và native
WebSocket cho Candle, progress, completion và Leaderboard. Business rule, persistence
truth và provider adapter tiếp tục thuộc capability sở hữu; `apps/api` chỉ mapping
request/response và gọi published application ports.

Triển khai theo vertical slice: trước hết là authentication/authorization/error và
contract parity; tiếp theo là read API; sau đó là command API cho Strategy/Experiment/Job;
cuối cùng là WebSocket snapshot/recovery và các endpoint phụ thuộc F-003/F-008.

## Bối cảnh kỹ thuật

**Ngôn ngữ/phiên bản**: Java 21, Spring Boot 3 cho `apps/api`; Java modules hiện có
giữ public application ports; contract fixture dùng JSON; test Python hiện có giữ
Python 3.11.

**Dependency chính**: Spring Web, Spring Security Resource Server/JWT, Jackson,
Spring JDBC và các published ports/factories của F-003–F-008. Không thêm framework
WebSocket hoặc broker mới ngoài native WebSocket boundary đã được ADR-0004 chấp thuận.

**Lưu trữ**: PostgreSQL/Supabase là source of truth thông qua persistence ports; Redis
và notification stream chỉ là nguồn event transient. F-009 không truy cập table hoặc
mapping nội bộ trực tiếp.

**Kiểm thử**: JUnit/Spring MockMvc cho REST và auth, WebSocket protocol/ordering tests,
contract tests đối chiếu OpenAPI và event documents, ArchUnit cho boundary, PostgreSQL
integration tests cho ownership/idempotency/recovery, và smoke tests có cấu hình rõ ràng.

**Nền tảng đích**: Java API chạy cùng modular-monolith deployment hiện tại; browser
dashboard là consumer đầu tiên; WebSocket dùng `/ws`, REST dùng `/api/v1` theo contract
đã có.

**Mục tiêu hiệu năng**: 95% bounded read và command acceptance hoàn thành trong 2 giây;
95% realtime update đến public boundary trong 1 giây; một connection duy trì bốn Candle
subscriptions cùng workload subscriptions trong 30 phút.

**Ràng buộc**: mọi private operation phải owner-scoped; POST tạo work phải idempotent;
decimal dùng biểu diễn chính xác; timestamp là UTC; không gửi secret/stack trace/provider
payload; realtime không được trở thành nguồn truth duy nhất; terminal outcome phải phục
hồi được sau notification loss.

**Quy mô/phạm vi**: MVP phục vụ một user/session browser, tối đa bốn Candle
subscriptions mỗi connection, workload subscriptions có giới hạn cấu hình, collections
được page bằng cursor opaque. Không bao gồm multi-tenant, anonymous business access,
external developer API, giao dịch tiền thật hoặc UI.

## Quyết định từ Phase 0 (research)

Chi tiết và các phương án bị loại được ghi tại [research.md](research.md). Các điểm đã
được chốt để không còn unknown trong thiết kế:

- REST giữ base path `/api/v1`; WebSocket giữ `/ws`; mọi contract dùng version riêng.
- WebSocket handshake dùng one-time ticket ngắn hạn do authenticated REST boundary cấp,
  không đặt access token dài hạn trong query string. Ticket chỉ dùng một lần và gắn với
  user/origin/expiry.
- Subscription registration tạo một synchronization marker phía server. Backend đăng ký
  subscription, chụp marker/snapshot boundary và chỉ phát event sau boundary; client dùng
  marker, event identity và resource revision để deduplicate/reconcile.
- Giới hạn mặc định: 4 Candle subscriptions, 4 workload subscriptions, message 64 KiB,
  30 commands/10 giây/connection, heartbeat 30 giây và timeout 90 giây. Mọi ngưỡng là
  configuration, được expose trong quickstart và test bằng fixed clock/scheduler.
- Cross-owner resource dùng cùng inaccessible response như not-found (404 trong public
  mapping) để tránh enumeration. Internal audit dùng service credential riêng.
- Idempotency hash dùng canonical JSON stable với scope `owner + operation`; cùng key/cùng
  hash replay outcome, cùng key/khác hash trả conflict.

## Kiểm tra Constitution

*Gate trước Phase 0: PASS.*

- **Đặc tả trước/ADR**: F-009 spec tồn tại; ADR-0004, ADR-0011 và ADR-0012 đều Accepted.
  Quyết định ticket và sequencing là chi tiết triển khai được ghi trong plan/contracts;
  nếu thay đổi semantics public hoặc deployment sẽ tạo ADR amendment trước merge.
- **Ownership module/data**: API chỉ gọi application ports công khai; ownership đi qua
  authenticated identity và parent chain, không truy cập persistence internal.
- **Reproducibility/immutability**: API chỉ expose snapshot/result đã được capability
  sở hữu đóng băng; reproduction tạo run mới, không overwrite evidence.
- **Versioned contract**: REST, error, WebSocket command/event và compatibility rule được
  đồng bộ trong một PR; provider payload không thoát ra public boundary.
- **An toàn/tin cậy/quan sát**: correlation ID, bounded retry/idempotency, safe error,
  durable-read recovery và security redaction là acceptance evidence bắt buộc.
- **Database/migration**: F-009 không sửa migration đã áp dụng; thiếu invariant phải
  được capability owner xử lý bằng forward migration riêng.

*Gate sau Phase 1: dự kiến PASS nếu các contract dưới đây giữ đúng các ràng buộc trên và
  không thêm dependency ngược hoặc public secret boundary.*

## Cấu trúc dự án

### Tài liệu của feature

```text
specs/009-public-api-realtime/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/requirements.md
└── contracts/
    ├── rest-api-contract.md
    ├── websocket-contract.md
    ├── error-contract.md
    └── authorization-contract.md
```

### Source code

```text
apps/api/src/main/java/com/cryptostrategy/platform/api/
├── auth/                 # authenticated user and WebSocket ticket boundary
├── error/                # public error envelope and exception mapping
├── market/               # Candle/Dataset REST mapping
├── strategy/             # system catalog and private Strategy mapping
├── experiment/           # Experiment, Candidate, Job and command mapping
├── backtest/             # Backtest/Result mapping
├── leaderboard/          # Leaderboard mapping
├── news/                 # public News mapping; internal audit remains isolated
├── realtime/             # WebSocket session, subscription and event mapping
└── observability/        # correlation and safe request lifecycle context

apps/api/src/test/java/com/cryptostrategy/platform/api/
├── auth/
├── error/
├── market/
├── strategy/
├── experiment/
├── backtest/
├── leaderboard/
├── news/
├── realtime/
├── security/
└── observability/
```

Capability modules remain unchanged unless a published application port is missing;
such a change belongs to the owning F-003–F-008 feature and is not implemented by
importing an internal class into `apps/api`.

**Quyết định cấu trúc**: chọn `apps/api` làm transport/orchestration boundary, tách DTO
theo public capability và realtime concern; contract fixtures/documents nằm trong feature
spec, còn business model và persistence adapter giữ nguyên module owner.

## Chuỗi triển khai

1. Đồng bộ OpenAPI, error catalog và WebSocket document với các contract F-009; thêm
   missing operation definitions (Dataset, private Strategy, Experiment/Job reads and
   commands) trước khi viết controller.
2. Xây auth/correlation/error boundary, WebSocket ticket và test không leak secret.
3. Xây read-only slices: Candle/Dataset, system/private Strategy, News, Experiment/Job,
   Result và Leaderboard.
4. Xây command slices: start Backtest/Experiment, stop/cancel/reproduce, idempotency,
   Location/202 responses và state conflict.
5. Xây realtime session/subscription, marker-based synchronization, ordering, backpressure,
   reconnect và recovery tests.
6. Chạy unit/contract/architecture, sau đó chạy database/Compose smoke khi dependency
   environment sẵn sàng; chỉ ghi evidence `Verified` khi có kết quả thật gắn commit.

## Theo dõi độ phức tạp

Không có violation Constitution cần biện minh. WebSocket là boundary đã được ADR-0004
Accepted; one-time ticket và marker sequencing làm rõ security/recovery semantics chứ
không thêm capability owner hoặc hệ thống message broker mới.
