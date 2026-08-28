# Phase 1 Data Model: Java Backend Foundation

Feature này không tạo persistent business entity hoặc database table. Các model dưới đây
là boundary/value model của foundation.

## CanonicalBoundaryValues

| Concept | Canonical rule |
|---|---|
| Public identity | UUID; không nhận free-form string làm identity đã xác thực |
| Exact decimal | Decimal tùy ý precision, không dùng binary floating-point tại public/domain boundary |
| Instant | Một UTC instant; offset input phải normalize mà không đổi thời điểm |

Architecture/contract fixture phải chứng minh public boundary từ chối convention dùng
binary floating-point hoặc local date-time không có offset. Đây là type convention, không
tạo business entity dùng chung.

## AuthenticatedUserContext

| Field | Rule |
|---|---|
| `userId` | UUID bắt buộc, parse từ JWT `sub`; không nhận từ request parameter/body |

Context chỉ đi vào application boundary sau khi signature, expiry/not-before, issuer và
audience đã được xác thực. Raw token và toàn bộ claims map không được đưa thành shared
domain model hoặc log.

## ErrorEnvelope

Áp dụng schema `ErrorResponse` đang công bố trong `docs/api/openapi.yaml`.

| Field | Rule |
|---|---|
| `code` | Uppercase stable error code; authentication dùng catalog hiện có |
| `message` | Safe public message, không chứa stack trace/provider response/secret |
| `details` | Structured safe details; empty object khi không có chi tiết công khai |
| `correlationId` | Khớp response `X-Correlation-Id` và MDC log |
| `timestamp` | UTC instant |

Foundation xử lý authentication và unexpected error. Business validation/error mapping
được capability feature bổ sung mà không đổi envelope.

## CorrelationId

- Client value: nonblank, tối đa 128 ký tự và không chứa control character.
- Generated value: uppercase ULID 26 ký tự.
- Một request có đúng một effective ID.
- ID đi vào response header, error envelope và mọi log của request.
- Lifecycle: resolve/generate → attach request/MDC → handle → attach response → clear MDC.

## HealthState

| Dimension | Values | Dependency rule |
|---|---|---|
| Liveness | `UP`, `DOWN` | Không phụ thuộc database/Auth/Redis/provider |
| Readiness | `UP`, `DOWN`, `OUT_OF_SERVICE` | Bao gồm application readiness và database connection health |
| Worker mode | `IDLE` trong F-002 | Không có queue connection/job consumer |

Health detail công khai không chứa JDBC URL, username, exception stack hoặc credential.

## ModuleBoundary

| Attribute | Meaning |
|---|---|
| `projectPath` | Gradle identity ổn định, ví dụ `:modules:market-data` |
| `basePackage` | Namespace duy nhất của module |
| `publicPackages` | Package dưới `api`; module được phép có consumer |
| `internalPackages` | Package dưới `internal`; consumer khác bị cấm import |
| `allowedDependencies` | Tập project/public API theo ADR-0002 |
| `owner` | Capability chịu trách nhiệm contract và review |

Module skeleton chưa có lifecycle hoặc business state. Build declaration không tự cấp
quyền import internal package; ArchUnit kiểm tra cả dependency direction và cycle.
