# Bằng chứng kiểm chứng Java Backend Foundation

## Phạm vi

- Feature: `002-java-backend-foundation`
- Branch: `feature/002-java-backend-foundation`
- Trạng thái tổng thể: `Planned`
- Verification commit: `TBD` — chỉ điền sau khi kết quả thật đã được commit
- Local environment: macOS, JDK/toolchain identity được ghi tại thời điểm verification
- Shared environment: Supabase development; credential chỉ lấy từ environment
- Non-secret configuration: `offline-check`, `api-local`, `worker-local`, `supabase-readiness`

Không ghi password, token, full JDBC URL hoặc secret value vào tài liệu này.

## Kết quả

| Evidence | Commit | Environment/configuration | Status | Result |
|---|---|---|---|---|
| Root `clean check` | TBD | `offline-check` | Planned | Chưa chạy |
| Gradle project/module boundary | TBD | `offline-check` | Planned | Chưa chạy |
| API/Worker startup và health | TBD | `api-local`, `worker-local` | Planned | Chưa chạy |
| Authentication matrix | TBD | local signing fixture | Planned | Chưa chạy |
| Correlation/error/log redaction | TBD | `api-local` | Planned | Chưa chạy |
| Supabase readiness | TBD | `supabase-readiness` | Planned | Chưa chạy |
| Repository secret scan | TBD | repository | Planned | Chưa chạy |
| Cross-owner extension review | TBD | review artifact | Planned | Chưa review |

## Merge gate

- ADR-0001, ADR-0002, ADR-0006 và ADR-0007 phải `Accepted` trước khi merge.
- Nghi Văn, Văn Minh và Tiến phải xác nhận extension point thuộc phạm vi của mình.
- Chỉ đổi evidence từ `Planned` sang `Verified` sau khi có output thật có thể xem lại.
