# F014 Evidence Guide

Thư mục này lưu bằng chứng có thể kiểm tra lại cho F014. Evidence trả lời ba câu hỏi: đã kiểm tra điều gì, kiểm tra trên phiên bản/môi trường nào và kết quả thật là gì.

## Trạng thái

| Trạng thái | Khi nào sử dụng |
|---|---|
| `PLANNED` | Đã xác định evidence cần thu nhưng chưa chạy |
| `BLOCKED` | Không thể chạy do dependency/quyền truy cập/môi trường; phải ghi blocker cụ thể |
| `PARTIAL` | Chỉ một phần acceptance được chứng minh hoặc có test bị skip |
| `VERIFIED` | Toàn bộ acceptance tương ứng pass trên commit/môi trường đã ghi và có artifact xem lại được |

`SKIPPED` là kết quả của một test/gate, không phải trạng thái đạt. Record có test bắt buộc bị skip không được chuyển thành `VERIFIED`.

## Evidence Record template

Sao chép khối này vào file evidence tương ứng:

```markdown
## EV-<criterion>-<sequence>: <tên ngắn>

- Criterion/requirement: <rubric #, FR/SC/acceptance scenario>
- Status: PLANNED | BLOCKED | PARTIAL | VERIFIED
- Commit SHA: <full hoặc unambiguous SHA>
- Working tree: clean | dirty (<mô tả thay đổi liên quan>)
- Captured at: <UTC ISO-8601 instant>
- Environment/profile: <LIVE | FIXTURE | test; OS/runtime/service versions>
- Non-secret configuration: <symbol, timeframe, seed, stop condition, concurrency...>
- Command/action: `<lệnh hoặc thao tác có thể lặp lại>`
- Expected result: <kết quả cần đạt>
- Observed result: <kết quả thực tế, gồm failure/timeout/skip>
- Artifact links: <report, screenshot, log, video timestamp, result ID>
- Limitations: <giới hạn hoặc none>
- Owner/reviewer: <người chạy và người kiểm tra>
```

## Quy tắc artifact

- Ưu tiên report máy tạo ra, test result, benchmark raw result và log có correlation ID.
- Screenshot phải thể hiện trạng thái cần chứng minh; không dùng screenshot prototype làm evidence runtime.
- Video cần link và timestamp tới đúng bước rubric, không chỉ trỏ tới đầu video.
- Result/Leaderboard evidence cần giữ ID truy vết nhưng phải loại token và dữ liệu cá nhân.
- LIVE và FIXTURE dùng record riêng. Fixture không chứng minh provider live, remote persistence hoặc external recovery.
- Failure record không bị xóa khi remediation; tạo record chạy lại để giữ lịch sử.
- Không sửa số đo hoặc chỉ chọn lần benchmark tốt nhất. Lưu từng lần chạy và median.

## Redaction policy

### Không được xuất hiện

- Password database/Redis, private key, service-role key, bearer/JWT token và service token.
- Cookie, authorization header, signed URL hoặc query parameter chứa credential.
- Full environment dump, connection string có credential hoặc provider response không cần thiết.
- Email, user ID hoặc dữ liệu cá nhân không cần cho acceptance.
- Exception body/stack trace chứa secret, SQL nhạy cảm hoặc đường dẫn nội bộ của provider.

### Được phép giữ

- Tên biến môi trường và placeholder dạng `<...>`.
- Public endpoint, contract version, model version và checksum không phải credential.
- Correlation/experiment/candidate/job/result IDs khi cần truy vết và không mang PII.
- Cấu hình workload không nhạy cảm: symbol, timeframe, seed, candidate limit, concurrency.

### Cách redact

- Thay giá trị bằng `[REDACTED:<TYPE>]`, ví dụ `[REDACTED:BEARER_TOKEN]`.
- Giữ tên field để người review hiểu dữ liệu nào đã được loại bỏ.
- Redact trước khi commit; `.gitignore` không bảo vệ secret đã từng được add/commit.
- Nếu phát hiện secret thật trong artifact: dừng sử dụng artifact, rotate/revoke credential, tạo bản đã redact và ghi incident mà không sao chép secret.

## Verification checklist trước khi chuyển `VERIFIED`

- [ ] Commit SHA và working-tree state được ghi.
- [ ] Timestamp UTC, environment và profile được ghi.
- [ ] Command/action cùng expected và observed result rõ ràng.
- [ ] Có ít nhất một artifact xem lại được.
- [ ] Không có required test bị skip hoặc dependency chưa chạy.
- [ ] Artifact không chứa secret/PII không cần thiết.
- [ ] Fixture được gắn nhãn và không dùng thay live evidence.
- [ ] Failure/limitation được công bố, không bị lược bỏ.

## Evidence cho phần nâng cao

Mỗi claim nâng cao phải có thêm:

```markdown
- Advanced item: <ML / Worker Pool / Redis / Loop Engineering / ...>
- Core baseline already required: <phần rubric cốt lõi đã yêu cầu>
- Increment beyond baseline: <phần thực sự vượt yêu cầu>
- Architecture problem solved: <vấn đề và trade-off>
- Implementation links: <code/ADR/contract>
- Demo scenario: <cách trình diễn phần nâng cao>
- Measurement/test: <accuracy/F1, throughput, failure recovery, replacement test...>
- Claim status: NO_CLAIM | CANDIDATE | VERIFIED_ADVANCED
```

Việc dùng tên công nghệ không tự tạo điểm nâng cao. Ví dụ:

- Sentiment phân loại cơ bản thuộc tiêu chí bắt buộc; ML nâng cao cần model evaluation, model replacement hoặc SentimentStrategy có evidence.
- Redis phục vụ queue/recovery cơ bản chưa chắc là nâng cao; cần chứng minh lợi ích kiến trúc và failure/throughput.
- Worker Pool cần benchmark scaling, không chỉ có thread pool trong code.
- Random Search là yêu cầu cơ bản; Loop Engineering nâng cao phải chứng minh vòng lặp/reconciliation/measurement vượt phần tối thiểu.

## Index dự kiến

| File | Nội dung |
|---|---|
| `baseline.md` | Trạng thái quality gate trước remediation |
| `rubric-matrix.md` | 23 tiêu chí cốt lõi và 1 dòng nâng cao tùy chọn |
| `foundation-gates.md` | Strategy/architecture/contract gates |
| `main-flow.md` | Live end-to-end journey |
| `failure-recovery.md` | Hai failure scenario |
| `reproduction.md` | Provenance và reproduction |
| `performance.md` | Raw runs, median và environment |
| `security.md` | Secret/security scan |
| `accessibility-responsive.md` | Keyboard và viewport checks |
| `quality-gates.md` | Java/Web/Python final gates |
| `final-commit-verification.md` | Clean-checkout quickstart trên candidate SHA |
| `release-review.md` | Constitution/ADR/ownership/contract review |
| `advanced-evidence.md` | Chỉ các claim nâng cao đã đủ bằng chứng |
