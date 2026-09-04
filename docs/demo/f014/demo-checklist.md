# F014 Demo Checklist

Điền người chạy, timestamp UTC và link Drive thật ngay sau mỗi lần demo. Không đánh dấu mục LIVE bằng kết quả từ fixture/controlled test.

## Thông tin phiên chạy

- Commit SHA: `<pending>`
- Working tree: `<clean | dirty + mô tả>`
- Profile: `<LIVE | CONTROLLED>`
- Người trình bày: `<pending>`
- Reviewer: `<pending>`
- Bắt đầu/kết thúc UTC: `<pending>`
- Thư mục ảnh/video trên Drive: `<pending>`

## A. Preflight và startup

| Xong | Bước | Kết quả mong đợi | Owner | Evidence |
|---|---|---|---|---|
| [ ] | Ghi commit/environment | Có SHA, timestamp, Java/Node/Python/Redis version; không lộ secret | Demo lead | `docs/evidence/f014/main-flow.md` |
| [ ] | Kiểm tra migration | PostgreSQL có schema F006–F010, gồm result provenance và reproduction verification | Database owner | migration log đã redact / `reproduction.md` |
| [ ] | Start PostgreSQL + Redis | Database reachable; `redis-cli ping` trả `PONG` | Infrastructure | health log |
| [ ] | Start Sentiment | `/health/live` trả `LIVE`, `/health/ready` trả `READY` + model/contract version | ML/Sentiment | health screenshot/log |
| [ ] | Start API + Worker | Cả bốn actuator liveness/readiness endpoint trả `UP` | API/Worker | health screenshot/log |
| [ ] | Start Web LIVE | `/login` mở được, `NEXT_PUBLIC_ENABLE_FIXTURES=false`, không có fixture banner | Web | browser screenshot |
| [ ] | Đăng nhập | Development user vào được trang Market; không ghi token/cookie | Auth/Web | video timestamp |

## B. Luồng chính

| Xong | Bước | Kết quả mong đợi | Owner | Evidence |
|---|---|---|---|---|
| [ ] | Market 4 chart | Bốn chart hiện đồng thời, pair/timeframe/freshness rõ | Market/Web | ảnh `01-market-four-charts` + `main-flow.md` |
| [ ] | Đổi timeframe độc lập | Một chart đổi timeframe, ba chart khác giữ lựa chọn và dữ liệu | Market/Web | video timestamp / ảnh trước-sau |
| [ ] | Catalog 4 Strategy | MA, RSI, Bollinger Bands, Support/Resistance cùng contract | Strategies/Web | ảnh `02-strategy-catalog` + test report |
| [ ] | Personal/composite Strategy | Tạo/publish version từ Strategy nền; composite hiển thị conflict rule | Strategy/Combination | ảnh `03-strategy-composite` + version ID |
| [ ] | Tạo immutable Dataset | Dataset ID thật được trả; không nhập ID bịa | Market/Experiment | API response đã redact / Result provenance |
| [ ] | Cấu hình Search | Random Search, seed, search space, Dataset/Strategy và stop condition hữu hạn | Search/Web | ảnh `04-search-config` |
| [ ] | Start và theo dõi | Một Experiment/Job được accept; progress tới terminal, không browser simulation | Search/Worker | Experiment/Job/correlation IDs + timing |
| [ ] | Leaderboard Top-K | Revision authoritative, thứ tự xác định, entry mở đúng Result | Leaderboard/Web | ảnh `05-leaderboard` + revision ID/fingerprint |
| [ ] | Result/Trades | Entry/Exit, Trades, Return, Win Rate, MDD, Number of Trades và capital/fees | Backtesting/Evaluation/Web | ảnh `06-result-trades` |
| [ ] | Provenance | Dataset/checksum, Strategy/version/parameters, Candidate, Attempt, assumptions, software/commit | Experiment/Result/Web | ảnh `07-provenance` + `reproduction.md` |
| [ ] | News/Sentiment | News đã lưu và sentiment authoritative hiển thị label/confidence/polarity + disclaimer | News/Sentiment/Web | ảnh `08-news-sentiment` |
| [ ] | Tổng thời gian | Toàn bộ B hoàn thành trong tối đa 10 phút | Demo lead | video timestamp + `main-flow.md` |

## C. Reproduction

| Xong | Bước | Kết quả mong đợi | Owner | Evidence |
|---|---|---|---|---|
| [ ] | Gửi reproduction | Nút chỉ bật khi source terminal; POST dùng idempotency key | Experiment/Web | network/log ID đã redact |
| [ ] | Xác nhận run mới | Target Experiment ID khác source và có `reproducesExperimentId` | Experiment/Persistence | ảnh `09-reproduction-link` |
| [ ] | Xác nhận source immutable | Manifest, Candidate, Result, Trades, Evaluation, revision nguồn không đổi | Persistence/Experiment | query/test artifact + `reproduction.md` |
| [ ] | Chờ verdict | UI hiển thị `MATCHED` hoặc `MISMATCHED`; ba comparison flags và differences hợp lệ | Experiment/Web | ảnh `10-reproduction-verdict` + verification ID |

## D. Failure isolation và recovery

| Xong | Bước | Kết quả mong đợi | Owner | Evidence |
|---|---|---|---|---|
| [ ] | Tắt Sentiment | News còn đọc được, degraded rõ; Market và technical Result vẫn hoạt động | Sentiment/News/API | ảnh trước-trong lỗi + timeline |
| [ ] | Start lại Sentiment | Retry hữu hạn; item về `ANALYZED` từ authoritative API | Sentiment/Worker | ảnh sau recovery + News/lease ID |
| [ ] | Ngắt realtime Market | Snapshot cũ được giữ, freshness stale/recovering rõ | Market/API/Web | ảnh trạng thái stale |
| [ ] | Reconnect/backfill | Snapshot reconcile, candle sắp thứ tự và không trùng | Market/API/Web | log/test + ảnh recovered |
| [ ] | Dừng Worker khi có job | Durable Experiment/Job không mất | Worker/Persistence | timeline + Job/correlation ID |
| [ ] | Start recovery consumer | Pending message được reclaim/retry; job terminal | Worker/Redis | consumer/message timeline |
| [ ] | Kiểm tra duplicate | Chỉ một accepted Result/outcome cho cùng job/message | Worker/Persistence | duplicate count/assertion |
| [ ] | Hoàn tất hồ sơ lỗi | Hai scenario có timestamp, IDs, result và limitation | Architecture/Demo | `docs/evidence/f014/failure-recovery.md` |

## E. Fallback

| Xong | Bước | Kết quả mong đợi | Owner | Evidence |
|---|---|---|---|---|
| [ ] | Ghi blocker LIVE | Dependency nào lỗi, thời điểm và ảnh hưởng được công bố | Demo lead | Evidence Record `BLOCKED` |
| [ ] | Chạy controlled suite | Market/research/news/recovery/reproduction Playwright pass | Web/QA | Playwright report |
| [ ] | Gắn nhãn fallback | Mọi ảnh/video ghi `CONTROLLED/TEST`; không khai provider/persistence LIVE | Demo lead | ảnh fallback |

## F. Hồ sơ và release gate

| Xong | Bước | Kết quả mong đợi | Owner | Evidence |
|---|---|---|---|---|
| [ ] | Rubric 24 dòng | 23 core + 1 advanced có status và evidence/gap | Documentation | `docs/evidence/f014/rubric-matrix.md` |
| [ ] | Advanced claim | Chỉ khai mục có code + demo + measurement vượt core; nếu không thì `NO_CLAIM` | Architecture | `advanced-evidence.md` |
| [ ] | Security scan | 0 privileged credential thật trong tracked/browser artifact | Security | `security.md` |
| [ ] | Performance 3 lần | Cùng workload, lưu cả ba run và median | Performance | `performance.md` |
| [ ] | Keyboard/viewport | Main actions dùng bằng keyboard; 360/768/1024/1440 không tràn toàn trang | Web/QA | `accessibility-responsive.md` |
| [ ] | Full quality gates | Java/Web/Python pass; skip/blocker được liệt kê, không tính pass | QA | `quality-gates.md` |
| [ ] | Cleanup | Runtime demo dừng đúng scope; không xóa shared data | Infrastructure | cleanup note |

## Ảnh nên đưa vào sheet đánh giá

Ưu tiên 10 ảnh `01`–`10` ở trên. Với mỗi ảnh trên Drive, phần mô tả nên có: tiêu chí rubric, commit SHA, profile, timestamp UTC, ID truy vết đã redact và đường dẫn file evidence trong repository. Nếu một ảnh chứa nhiều chức năng, vẫn liên kết từng dòng rubric tới đúng vùng/ảnh thay vì chỉ trỏ về đầu video.
