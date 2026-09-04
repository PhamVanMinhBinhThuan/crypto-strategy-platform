# F014 Environment and Startup Audit

## Kết luận

Các runtime riêng lẻ đã có entry point và health boundary, nhưng repository **chưa có một lệnh thống nhất** để dựng toàn bộ PostgreSQL/Supabase + Redis + Sentiment + API + Worker + Web. Compose hiện chỉ dựng Sentiment. Vì vậy live end-to-end chưa được xem là ready cho tới khi runbook chốt startup order và dry run thành công.

## Runtime inventory

| Runtime/dependency | Entry point | Port | Health/readiness | Trạng thái audit |
|---|---|---:|---|---|
| PostgreSQL/Supabase | External/shared service qua `DATABASE_*` | Provider-defined | API/Worker `databaseReadiness` dùng JDBC validation | Cần credential local và remote readiness run |
| Redis | Worker explicit `worker.redis.*`; API dùng Spring Redis connection | `6379` mặc định | Chưa có Redis health trong readiness group | Gap: readiness chỉ phản ánh database |
| Sentiment | `infra/compose/docker-compose.yml` hoặc FastAPI/Uvicorn | `8000` | `/health/live`, `/health/ready` | Compose support có sẵn; cần model bundle thật |
| API | `./gradlew :apps:api:bootRun` | `8080` | `/actuator/health/liveness`, `/actuator/health/readiness` | Entry point có sẵn; cần DB/auth/Redis/provider config |
| Worker | `./gradlew :apps:worker:bootRun` | `8081` | `/actuator/health/liveness`, `/actuator/health/readiness` | Entry point có sẵn; cần DB/Redis; News cần Sentiment |
| Web | `npm run dev` trong `apps/web` | `3000` mặc định | Không có health route riêng | Gap: readiness cần kiểm tra page/client composition thủ công hoặc thêm smoke check |

## Dependency graph và startup order

```text
PostgreSQL/Supabase ─┬─→ API ───────────────→ Web
                     └─→ Worker ─→ Redis ───→ API realtime/WebSocket
Sentiment model bundle → Sentiment service ─→ Worker News analysis
Binance REST/WebSocket ─────────────────────→ API Market adapter
Supabase Auth ──────────────────────────────→ Web session → API JWT validation
```

Thứ tự dự kiến:

1. PostgreSQL/Supabase và Redis.
2. Sentiment với reviewed model bundle.
3. API.
4. Worker.
5. Web.
6. Health/readiness checks, authenticated session và live-provider smoke trước demo.

Không start Search khi database, Worker/Redis path hoặc Strategy catalog chưa ready. Sentiment có thể degraded cho failure scenario nhưng phải hiển thị trung thực và không chặn technical Backtest.

## Configuration inventory

### Shared server-side secrets/config

Nguồn placeholder: `.env.example`.

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `SUPABASE_JWT_ISSUER`, `SUPABASE_JWT_JWKS_URI`, `SUPABASE_JWT_AUDIENCE`
- `SENTIMENT_SERVICE_TOKEN`, `SENTIMENT_BUNDLE_PATH`
- `NEWS_ENABLED`, `NEWS_AUDIT_SERVICE_TOKEN`

Gap: root template chưa liệt kê các override cần cho live Binance, Sentiment URL và Redis/Worker identity. T045/T047 phải tài liệu hóa hoặc cập nhật placeholder template mà không thêm giá trị thật.

### API non-secret live overrides

- `PLATFORM_MARKET_DATA_PROVIDER=binance`
- Binance REST/WebSocket base URL, timeout, retry và reconnect limits
- `PLATFORM_REALTIME_MAX_CANDLE_SUBSCRIPTIONS=4`
- Search start/reproduce feature flags
- Allowed browser origins

API mặc định dùng provider `fixture`; nếu quên override thì không được ghi evidence là Binance LIVE.

### Worker configuration

- `worker.redis.*`: host, port, optional username/password/SSL, timeout
- `worker.streams.*` và consumer group/name
- Backtest/ranking/search concurrency và per-experiment in-flight limits
- Bounded retry, execution timeout và reconciliation intervals
- News provider, Sentiment endpoint/model/preprocessing versions

Mỗi Worker chạy song song cần consumer name riêng. Redis credential là server-only.

### Sentiment configuration

- `SENTIMENT_SERVICE_TOKEN`
- `SENTIMENT_BUNDLE_PATH`
- `SENTIMENT_LOAD_TIMEOUT_SECONDS`
- `SENTIMENT_MAX_CONCURRENCY`

Docker image cài optional dependency `ml`, load `model.keras`, frozen vocabulary và warm-up trước khi readiness trở thành `READY`.

### Web browser-safe configuration

Nguồn placeholder: `apps/web/.env.example`.

- `NEXT_PUBLIC_SUPABASE_URL`
- `NEXT_PUBLIC_SUPABASE_ANON_KEY`
- `NEXT_PUBLIC_API_BASE_URL`
- `NEXT_PUBLIC_WS_URL`
- `NEXT_PUBLIC_ENABLE_FIXTURES`

Fixture flag bị từ chối trong production. Không đưa database, Redis, service-role hoặc service token vào `NEXT_PUBLIC_*`.

## Health semantics

| Check | Ready nghĩa là gì | Không chứng minh được |
|---|---|---|
| API/Worker liveness | Spring process còn sống | Database, Redis, Binance hoặc downstream hoạt động |
| API/Worker readiness | Database config hợp lệ và JDBC connection valid | Redis path, Worker consumer, Binance stream, Sentiment model |
| Sentiment liveness | FastAPI process phản hồi | Model đã load |
| Sentiment readiness | Runtime `READY`, trả contract/model version | News collector hoặc Worker đã kết nối |
| Web page smoke | Next.js render và client config hợp lệ | Backend workflow end-to-end |

Runbook phải kiểm tra cả health endpoint lẫn một smoke action tại từng public boundary; không suy diễn readiness toàn hệ thống từ một `/health` duy nhất.

## Script và documentation gaps

| Gap | Tác động | Remediation owner/task |
|---|---|---|
| Compose chỉ có Sentiment | Không thể dựng toàn stack bằng một lệnh | T045 runbook; chỉ mở rộng compose/script nếu dry run chứng minh cần |
| README chỉ nêu API/Worker startup | Người mới thiếu Redis, Sentiment, Web và startup order | T045, T047 |
| Không có unified env validation | Lỗi thường chỉ lộ khi service start | T045 checklist và T049 dry run |
| API/Worker readiness không gồm Redis/downstream | Có thể báo ready trong khi async/realtime path hỏng | T015 audit contract, T030–T037 recovery evidence |
| Web không có health endpoint | Automation cần dùng page/request smoke | T017–T019 Playwright và T045 |
| Live auth cần tài khoản/session Supabase | Browser E2E có thể bị skip nếu thiếu credential | T005 ghi dependency; T029/T049 không được tính pass nếu skip |
| Local default JDK/Node/Python có thể sai | Lệnh fail trước khi vào test | Pin JDK 21, Node 22, Python 3.11/3.12 trong T045/T047 |
| Redis recovery test cần `F009_REDIS_SMOKE=true` và Redis thật | Baseline đang skip một recovery test | T031–T037 phải chạy dependency-backed scenario |
| Java redaction tests đang fail và Web formatting dừng gate | Chưa đủ điều kiện release | Remediation trước T054 final gates |

## Phần nâng cao liên quan môi trường

- **Machine Learning**: model Keras, frozen vocabulary, version/checksum và service boundary có thật. Cần chạy container với bundle thật, đo cold start/inference hoặc chứng minh thay model trước khi khai nâng cao.
- **Worker Pool**: concurrency controls có thật. Cần chạy nhiều Worker với consumer name riêng và benchmark 1→3 Worker trước khi khai nâng cao.
- **Redis Streams**: queue/outbox/recovery code có thật. Cần failure/recovery evidence và giải thích phần vượt reliability cốt lõi.
- **Loop Engineering**: orchestration components có thật. Cần trace full loop, stop/reconcile và measurement trước khi khai nâng cao.

Không có dependency/startup requirement nào cho advanced feature chưa tồn tại; F014 không cài hoặc dựng Genetic/LLM/Kafka/CQRS chỉ để phục vụ demo.

## Readiness decision tại T005

- **Unit/contract baseline**: PARTIAL — xem `docs/evidence/f014/baseline.md`.
- **Fixture UI rehearsal**: cấu hình đã xác định nhưng chưa dry run.
- **Live end-to-end demo**: NOT READY — cần hoàn thành strategy remediation, sửa baseline gates và chạy dependency-backed smoke.
- **Advanced demo claims**: CANDIDATE ONLY — chưa mục nào đủ measurement để tự khai.
