# F014 Failure Isolation and Recovery Evidence

## Kết luận hiện tại

Redis/Worker recovery đã được kiểm tra với Redis thật trên máy local và không tạo outcome trùng. Sentiment degraded/recovery đã pass ở backend integration và browser test có điều khiển, nhưng lần chạy service ML thật bị chặn vì host không có optional runtime `tensorflow-cpu` và Docker daemon không hoạt động. Vì vậy hồ sơ này đang ở trạng thái `PARTIAL`; không dùng browser fixture để tuyên bố external Sentiment recovery đã `VERIFIED`.

## EV-US2-REDIS-001: Worker bị ngắt và consumer khác reclaim message

- Criterion/requirement: US2, FR-013–FR-016; reclaim không tạo outcome trùng và durable state không bị mất.
- Status: VERIFIED
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation F014 T001–T036 chưa commit)
- Captured at: `2026-09-04T09:15:56Z`
- Environment/profile: dependency-backed integration; macOS Darwin arm64, Java 21.0.12.1, Redis 8.6.3.
- Non-secret configuration: `F014_REDIS_SMOKE=true`; stream/group/consumer riêng do test tạo; message ID và correlation ID dùng test identifier, không chứa credential.
- Command/action: `JAVA_HOME=<JDK_21> F014_REDIS_SMOKE=true F009_REDIS_SMOKE=true ./gradlew :apps:api:test --tests com.cryptostrategy.platform.api.news.NewsDegradedIsolationTest --tests com.cryptostrategy.platform.api.realtime.RealtimeRedisRecoveryIntegrationTest :apps:worker:test --tests com.cryptostrategy.platform.worker.engine.F014RecoveryScenarioTest --rerun-tasks --no-daemon`
- Expected result: message pending của consumer đã dừng được consumer khác claim sau idle boundary; cùng job/message chỉ tạo một accepted outcome; Experiment, Job, Result, News, Sentiment và publication state vẫn đọc được.
- Observed result: `BUILD SUCCESSFUL in 26s`; cả ba test class pass và Redis test không skip. `F014RecoveryScenarioTest` quan sát message chuyển từ consumer bị ngắt sang recovery consumer, dedup trả `false` ở lần xử lý thứ hai, outcome count vẫn bằng `1`; sau khi xóa Redis key, sáu durable state vẫn đọc được từ store.
- Correlation/identity: job/correlation `01J000000000000000000000W4`; cùng stable message/job identity được dùng ở lần reclaim.
- Duplicate assertion: lần claim đầu được accept, lần xử lý lại bị durable dedup từ chối; accepted outcome count `1`.
- Recovery result: `VERIFIED`; Redis là transport/recovery aid, không phải nguồn dữ liệu bền vững duy nhất.
- Artifact links: `apps/worker/src/test/java/com/cryptostrategy/platform/worker/engine/F014RecoveryScenarioTest.java`; `apps/worker/build/reports/tests/test/index.html`; `apps/api/build/reports/tests/test/index.html`.
- Limitations: persistence trong scenario này dùng H2 để kiểm tra durable boundary; Redis là dependency thật. PostgreSQL/live Worker process journey được để riêng cho runbook dry run.
- Owner/reviewer: project team / pending reviewer.

### Timeline có thể kiểm tra lại

1. Consumer `crashed-consumer` nhận message và để message ở trạng thái pending.
2. Trước idle boundary, `recovery-consumer` không claim được message.
3. Sau idle boundary, `recovery-consumer` claim đúng message/job identity.
4. Durable dedup chỉ nhận xử lý lần đầu; delivery/reclaim tiếp theo không tạo outcome thứ hai.
5. Redis key của scenario bị xóa; sáu durable record vẫn đọc được.

## EV-US2-SENTIMENT-001: Sentiment degraded nhưng News và luồng kỹ thuật còn dùng được

- Criterion/requirement: US2, FR-013; Sentiment lỗi phải độc lập, News còn đọc được và retry về kết quả authoritative.
- Status: PARTIAL
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation F014 T001–T036 chưa commit)
- Captured at: `2026-09-04T09:15:56Z`
- Environment/profile: Java backend integration + Playwright desktop controlled profile; Node 22.23.2. External FastAPI/model process chưa chạy thành công.
- Non-secret configuration: retry delays `5s, 30s`; browser correlation ID `f014-browser-recovery`; fixture flag không được dùng để đại diện LIVE.
- Command/action: cùng Gradle command tại EV-US2-REDIS-001, sau đó `npm run test:e2e -- tests/e2e/f014-failure-recovery.spec.ts --project=desktop`.
- Expected result: transient Sentiment failure trả trạng thái degraded trung thực, News vẫn đọc được, retry chỉ chạy khi đủ thời gian, sau recovery trả sentiment authoritative; Market path không bị gọi/chặn.
- Observed result: backend test pass; trạng thái đi qua `FAILED_RETRYABLE` → chờ đủ 5 giây → `ANALYZING` → `ANALYZED`, kết quả `POSITIVE`, confidence `0.91`, polarity `0.72`; Market mock không có interaction. Playwright desktop pass `2/2` trong `3.3s`, gồm realtime stale/reconcile và News degraded/recovery.
- Correlation/identity: News ID `01J00000000000000000000001`; lease đầu `01J00000000000000000000002`; lease retry `01J00000000000000000000003`; browser correlation `f014-browser-recovery`.
- Duplicate assertion: completion chỉ chấp nhận lease retry đang active; lease cũ không được dùng để ghi kết quả.
- Recovery result: logic và UI `VERIFIED` trong controlled profile; external dependency recovery vẫn `BLOCKED` nên record tổng là `PARTIAL`.
- Artifact links: `apps/api/src/test/java/com/cryptostrategy/platform/api/news/NewsDegradedIsolationTest.java`; `apps/web/tests/e2e/f014-failure-recovery.spec.ts`; `apps/api/build/reports/tests/test/index.html`; `apps/web/playwright-report/index.html`.
- Limitations: đây không phải screenshot/runtime evidence của external Sentiment service. Không dùng record này để khai model inference live hoặc provider live.
- Owner/reviewer: project team / pending reviewer.

### Timeline có thể kiểm tra lại

1. `t0`: inference lỗi retryable; News trả `FAILED_RETRYABLE`, không dựng sentiment giả.
2. `t0 + 4s`: chưa đủ retry boundary nên không claim.
3. `t0 + 5s`: worker claim lại bằng lease mới và chuyển `ANALYZING`.
4. `t0 + 6s`: completion hợp lệ chuyển `ANALYZED`; News trả kết quả mới.
5. Browser giữ nội dung News trong lúc degraded và bỏ cảnh báo sau reload/reconcile.

## EV-US2-SENTIMENT-EXT-001: Lần thử khởi động service ML thật

- Criterion/requirement: T037 external Sentiment stop/restart.
- Status: BLOCKED
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation F014 T001–T036 chưa commit)
- Captured at: `2026-09-04T09:15:56Z`
- Environment/profile: macOS arm64, Python 3.12 virtual environment, real checked-in `apps/sentiment/artifacts/active_release` bundle.
- Command/action: start Uvicorn với `SENTIMENT_SERVICE_TOKEN=[REDACTED:SERVICE_TOKEN]`, `SENTIMENT_BUNDLE_PATH=<repo>/apps/sentiment/artifacts/active_release`, sau đó poll `/health/ready`.
- Expected result: service chuyển `LOADING` → `READY`, có thể stop/restart và Worker retry thành công.
- Observed result: Uvicorn mở process nhưng runtime kết thúc với exit code `1` khi load model; kiểm tra environment xác nhận `ModuleNotFoundError: No module named 'tensorflow'`. Optional dependency được khai báo là `tensorflow-cpu==2.19.0`; Docker daemon hiện không khả dụng để chạy image đã cài extra `ml`.
- Artifact links: `apps/sentiment/pyproject.toml`; `apps/sentiment/Dockerfile`; `apps/sentiment/artifacts/active_release/manifest.json`.
- Limitations: chưa có external stop/restart timeline hay inference response thật. Cần môi trường chạy được image Sentiment hoặc Python environment có extra `ml` trước khi đóng T037.
- Owner/reviewer: project team / pending reviewer.

## Cách lấy ảnh minh chứng sau khi external scenario chạy được

1. Chụp News trước khi tắt Sentiment: bài viết có trạng thái `ANALYZED` và sentiment.
2. Tắt riêng Sentiment, giữ API/Worker/Web; chụp cùng bài/News list còn đọc được với nhãn degraded rõ ràng.
3. Start lại Sentiment, chờ retry boundary; chụp trạng thái trở lại `ANALYZED`.
4. Trong ảnh hoặc sheet ghi News ID/correlation ID đã redact và timestamp; không chụp token, cookie hoặc cửa sổ terminal có secret.
5. Ảnh controlled Playwright chỉ dùng để minh họa hành vi UI; ảnh nộp cho dòng external recovery phải lấy từ profile LIVE.
