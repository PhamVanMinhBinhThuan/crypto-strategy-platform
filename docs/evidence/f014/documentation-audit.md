# F014 Documentation Audit

## EV-F014-DOC-001: Secret, claim, link và financial-advice audit

- Criterion/requirement: T060; tài liệu F014 không chứa secret thật, số liệu không có nguồn, link nội bộ hỏng hoặc nội dung được trình bày như lời khuyên đầu tư.
- Status: VERIFIED
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation và tài liệu F014 chưa commit).
- Captured at: `2026-09-04T10:15:57Z`.
- Scope: toàn bộ Markdown trong `specs/014-end-to-end-demo-hardening/`, `docs/demo/f014/` và `docs/evidence/f014/`.
- Observed result: không có Markdown link nội bộ hỏng; không thấy credential value/connection string; mọi giá trị nhạy cảm chỉ là tên biến, placeholder hoặc marker redact; số đo quality/performance/security/accessibility đều trỏ tới command/report; nội dung trading chỉ mô tả simulation/research và loại trừ giao dịch tiền thật/lời khuyên đầu tư.
- Artifact links: `docs/evidence/f014/security.md`, `docs/evidence/f014/quality-gates.md`, `docs/evidence/f014/performance.md`, `docs/evidence/f014/accessibility-responsive.md`.
- Limitations: link checker kiểm tra sự tồn tại của đường dẫn local, không xác nhận URL bên ngoài; audit nội dung không thay thế legal/compliance review.
- Owner/reviewer: implementer F014 / pending reviewer.

## Kết quả kiểm tra

| Hạng mục | Kết quả | Ghi chú |
|---|---|---|
| Secret/credential | PASS | Secret scanner pass 1.806 text candidate; docs chỉ dùng `<placeholder>`/`[REDACTED:*]` và tên biến |
| Markdown local links | PASS | Không có target local bị thiếu trong ba thư mục F014 |
| Số liệu và claim | PASS | 545/275/10 tests, 2.298×, 1.799 candidates và 5/5 đều có raw report/command; LIVE blockers giữ `BLOCKED/PARTIAL` |
| Financial advice | PASS | `spec.md` ghi giao dịch tiền thật, ví và lời khuyên đầu tư ngoài phạm vi; không có khuyến nghị mua/bán hay cam kết lợi nhuận |
| Fixture/LIVE labeling | PASS | Controlled/FIXTURE không được dùng thay Binance/PostgreSQL/Sentiment LIVE evidence |

Các số trong `spec.md` như bốn Strategy, bốn viewport, ba benchmark run, giới hạn 10 phút và mục tiêu
2× là acceptance target, không phải kết quả đo. Kết quả quan sát chỉ nằm trong Evidence Record và giữ
raw values/limitations tương ứng.
