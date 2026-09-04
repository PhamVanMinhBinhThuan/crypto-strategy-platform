# F014 Security Evidence

## EV-US5-SECURITY-001: Secret scan và public error redaction

- Criterion/requirement: T053, T056, FR-025; không phát hành secret qua source/browser artifact và không phản chiếu lỗi nội bộ ra response/log công khai.
- Status: PARTIAL
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation và tài liệu F014 chưa commit).
- Captured at: `2026-09-04T10:15:57Z`
- Environment/profile: repository scan sau Next production build; Java public error/redaction tests trong full Gradle gate.
- Non-secret configuration: allowlist chỉ chấp nhận marker placeholder/test/redaction tường minh; scanner không in giá trị nghi ngờ.
- Command/action: `./scripts/security/scan-demo-secrets.sh` và `JAVA_HOME=<JDK_21> ./gradlew clean check --no-daemon`.
- Expected result: scanner không tìm credential pattern ngoài allowlist; response và log không chứa exception message/cause, provider payload, token, SQL hoặc đường dẫn nhạy cảm.
- Observed result: `F014 secret scan: PASS`; 1.806 text candidate từ tracked/untracked repository files và browser artifacts được kiểm tra. Full Java gate pass 0 failure, bao gồm `PublicErrorContractTest`, `PublicRedactionIntegrationTest` và `MarketApiContractTest`.
- Artifact links: `scripts/security/scan-demo-secrets.sh`; `apps/api/build/reports/tests/test/index.html`; `docs/evidence/f014/quality-gates.md`.
- Limitations: pattern scan không thay thế secret manager, lịch sử Git scan, dependency vulnerability scan hoặc manual penetration test; file binary đánh giá không được xem như text candidate. Kết quả chưa gắn final clean commit nên chỉ chuyển `VERIFIED` sau T061 rerun.
- Owner/reviewer: implementer F014 / pending reviewer.

Khi phát hiện finding, script chỉ xuất `path:line:rule`, không xuất secret candidate. Nếu lần chạy sau fail, phải dừng dùng artifact liên quan và rotate/revoke credential thật trước khi tạo bản đã redact.
