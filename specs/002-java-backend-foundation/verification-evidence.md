# Bằng chứng kiểm chứng Java Backend Foundation

## Phạm vi

- Feature: `002-java-backend-foundation`
- Branch: `feature/002-java-backend-foundation`
- Trạng thái tổng thể: `In progress`
- Verification commit gần nhất: `e9f7eb4e346bceeb6d75a25ce742bb1d89eee11f`
- Local environment: macOS 26.5.2 arm64, OpenJDK 21.0.12.1, Gradle Wrapper 8.14.5
- Shared environment: Supabase development; credential chỉ lấy từ environment
- Non-secret configuration: `offline-check`, `api-local`, `worker-local`, `supabase-readiness`

Không ghi password, token, full JDBC URL hoặc secret value vào tài liệu này.

## Kết quả

| Evidence | Commit | Environment/configuration | Status | Result |
|---|---|---|---|---|
| Root `clean check` | `e9f7eb4e346bceeb6d75a25ce742bb1d89eee11f` | `offline-check`; external services unset | Verified | PASS, 4.5 giây; không cần Docker, database, Redis hoặc provider |
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
