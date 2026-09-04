# F014 Foundation Gates

## Thông tin lần chạy

| Trường | Giá trị |
|---|---|
| Commit nền | `50c28d99c02a4ee28ed1109b231daa4397a22fe4` |
| Thời điểm | `2026-09-04T03:41:08Z` |
| Môi trường | macOS local, JDK Temurin `21.0.12.1` |
| Phạm vi thay đổi | Strategy foundation T006–T015 trên working tree F014 |

Commit nền được ghi để truy vết checkout; các thay đổi F014 tại thời điểm chạy chưa commit nên evidence này chỉ trở thành release evidence sau khi được commit và chạy lại ở final gate.

## Gate bắt buộc cho Strategy foundation

Lệnh:

```bash
./gradlew :architecture-tests:test \
  :modules:strategy-core:test \
  :modules:strategies:test \
  :modules:contracts:test
```

Kết quả: `BUILD SUCCESSFUL`.

| Gate | Tests | Skipped | Failures | Kết luận |
|---|---:|---:|---:|---|
| Architecture tests | 32 | 0 | 0 | PASS |
| Strategy core | 15 | 0 | 0 | PASS |
| Bốn Strategy + registry | 18 | 0 | 0 | PASS |
| Message contracts | 7 | 0 | 0 | PASS |
| **Tổng foundation** | **72** | **0** | **0** | **PASS** |

Các gate trên chứng minh module boundaries không bị phá, Strategy contract dùng chung vẫn hợp lệ, bốn implementation deterministic qua test, registry có identity duy nhất và message contract vẫn tương thích.

## API boundary probe bổ sung

Lệnh:

```bash
./gradlew :apps:api:test \
  --tests '*StrategyApiIntegrationTest' \
  --tests '*StrategyConfigurationTest' \
  --tests '*WebSocketContractTest' \
  --tests '*FoundationalBoundaryTest' \
  --tests '*PublicErrorContractTest'
```

Kết quả: 19 tests, 18 pass, 1 failure, 0 skipped.

- Strategy API, configuration, foundational boundary và WebSocket contract đều pass.
- `PublicErrorContractTest.unexpectedFailureReturnsAndLogsOnlySafeInformation` fail vì stack trace vẫn ghi nội dung exception chứa redaction sentinel. Đây là finding baseline đã ghi tại T001, không phát sinh từ Strategy plugins.
- Failure này **không được tính là pass**. Security/log remediation và full rerun thuộc các quality/security task sau; chưa được phép gọi nhánh hiện tại là release-ready.

## Quyết định T016

- Foundation architecture/contract gate cho remediation Strategy: **PASS**.
- Public API security gate mở rộng: **PARTIAL/KNOWN FAILURE**.
- Có thể bắt đầu Phase 3 test-first, nhưng không được chụp màn hình `BUILD SUCCESSFUL` của API suite làm bằng chứng security cho đến khi lỗi redaction được sửa và chạy lại.
