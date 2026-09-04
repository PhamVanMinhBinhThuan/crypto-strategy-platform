# F014 Security Evidence

## EV-US5-SECURITY-001: Secret scan và public error redaction

- Criterion/requirement: T053, T056, T061, FR-025; không phát hành secret qua source/browser artifact và không phản chiếu lỗi nội bộ ra response/log công khai.
- Status: VERIFIED
- Commit SHA: `0761a54bfcbb93c3b24cb216b19d6cc79e03e21b`
- Working tree: clean detached worktree khi bắt đầu scan.
- Captured at: `2026-09-04T12:22:15Z`
- Environment/profile: clean repository scan sau Java gate; Java public error/redaction tests trong full Gradle gate.
- Non-secret configuration: allowlist chỉ chấp nhận marker placeholder/test/redaction tường minh; scanner không in giá trị nghi ngờ.
- Command/action: `./scripts/security/scan-demo-secrets.sh` và `JAVA_HOME=<JDK_21> ./gradlew clean check --no-daemon`.
- Expected result: scanner không tìm credential pattern ngoài allowlist; response và log không chứa exception message/cause, provider payload, token, SQL hoặc đường dẫn nhạy cảm.
- Observed result: `F014 secret scan: PASS`; 1.782 text candidate được kiểm tra. Full Java gate pass 0 failure, gồm `PublicErrorContractTest`, `PublicRedactionIntegrationTest` và `MarketApiContractTest`.
- Artifact links: `scripts/security/scan-demo-secrets.sh`; `apps/api/build/reports/tests/test/index.html`; `docs/evidence/f014/quality-gates.md`.
- Limitations: pattern scan không thay thế secret manager, Git history scan, dependency vulnerability scan hoặc manual penetration test; file binary đánh giá không được xem như text candidate.
- Owner/reviewer: implementer F014 / pending reviewer.

Khi có finding, script chỉ xuất `path:line:rule`, không xuất candidate value. Nếu lần sau fail, phải dừng dùng artifact liên quan và rotate/revoke credential thật trước khi tạo bản redact.
