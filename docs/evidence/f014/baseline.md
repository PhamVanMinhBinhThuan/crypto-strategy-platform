# F014 Baseline Quality Gates

## Evidence metadata

- **Captured at**: 2026-09-04 (Asia/Ho_Chi_Minh)
- **Branch**: `feature/014-end-to-end-demo-hardening`
- **Base commit**: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- **Working tree**: dirty; chứa F014 specification/planning artifacts và thay đổi người dùng có trước. Kết quả này không được xem là release evidence của riêng base commit.
- **Environment**: macOS Darwin arm64; Java 21.0.12.1; Node.js 22.23.2; Python 3.12.10
- **Profile**: local baseline; không chạy remote Supabase integration hoặc live browser E2E

## Summary

| Runtime | Command | Status | Observed result |
|---|---|---|---|
| Java | `PATH=<JDK21>/bin:$PATH ./gradlew clean check` | FAILED | Root gate dừng tại `:apps:api:test`: 155 API tests, 3 failed, 1 skipped. Các task đã chạy trước đó không đại diện cho toàn bộ release gate. |
| Web | `PATH=<NODE22>/bin:$PATH npm run check` | FAILED | Dừng tại `format:check`; `src/features/market/model/market-range.ts` chưa đúng Prettier. Lint, typecheck, test và build bị skip do command chain dừng sớm. |
| Sentiment | `/tmp/f014-sentiment-baseline/bin/python -m pytest` | PASSED | 10 passed, 1 deprecation warning, 0 failed, trong 0.98 giây. |

## Java findings

Ba test thất bại:

1. `PublicErrorContractTest.unexpectedFailureReturnsAndLogsOnlySafeInformation`
2. `MarketApiContractTest.providerFailureUsesSafeStableError`
3. `PublicRedactionIntegrationTest.publicErrorsAndLogsRedactExceptionMessagesCausesAndProviderContext`

Nguyên nhân quan sát được: exception stack trace trong captured application logs vẫn chứa các chuỗi nhạy cảm giả lập của test như token, SQL và provider path. Public response vẫn cần được xem cùng report, nhưng gate hiện chứng minh log redaction chưa đạt contract.

Một test bị skip:

- `RealtimeRedisRecoveryIntegrationTest.resumesNotificationDeliveryAfterRedisClientConnectionLoss`

Report có thể xem tại `apps/api/build/reports/tests/test/index.html` sau lần chạy này.

## Web finding

Prettier báo lỗi tại:

- `apps/web/src/features/market/model/market-range.ts`

Không tự format trong T001 để giữ baseline nguyên trạng. Vì `npm run check` dùng chuỗi `&&`, các gate sau `format:check` chưa được chạy và được ghi là **SKIPPED**, không phải pass.

## Sentiment result

Test dependencies được cài vào virtual environment tạm `/tmp/f014-sentiment-baseline`; repository dependency files không bị thay đổi. Pytest hoàn thành với 10 test pass. Warning duy nhất là deprecation của alias `anyio.abc.BlockingPortal` từ Starlette test client.

## Environment corrections made before valid runs

- Java mặc định là JDK 25 và Gradle không cấu hình được; baseline hợp lệ được chạy lại bằng JDK 21 theo project requirement.
- `npm` mặc định trỏ tới Node 24 bị thiếu `libsimdjson.27.dylib`; baseline hợp lệ được chạy lại với Node 22.
- Python mặc định là 3.13 và thiếu pytest; baseline hợp lệ dùng Python 3.12 cùng virtualenv tạm.

Các lần thử lỗi toolchain này không được tính là product test failure.

## Baseline conclusion

T001 hoàn tất vì trạng thái thật của cả ba runtime đã được thu thập. Baseline tổng thể là **FAILED/PARTIAL** và chưa đủ điều kiện release:

- Java cần remediation log redaction và chạy lại full gate.
- Web cần sửa formatting rồi chạy tiếp lint, typecheck, unit tests và build.
- Redis recovery test bị skip phải được chạy trong môi trường đáp ứng dependency trước khi tính Verified.
- Sentiment unit/integration test baseline đang pass.
