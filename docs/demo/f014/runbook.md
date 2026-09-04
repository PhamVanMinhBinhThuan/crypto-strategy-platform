# F014 End-to-End Demo Runbook

Runbook này dựng và kiểm tra luồng Market → Strategy → Search → Backtest → Evaluation → Leaderboard → Result/Trades → News/Sentiment, hai kịch bản lỗi và reproduction. Mọi lệnh chạy từ repository root trừ khi có ghi khác.

## 1. Quy tắc evidence

- `LIVE`: Web gọi API/Worker/PostgreSQL/Redis/Sentiment/Binance thật, không bật fixture.
- `CONTROLLED`: Playwright chặn network bằng response xác định để kiểm tra UI/contract; không dùng để chứng minh provider hoặc persistence thật.
- Không chụp/copy token, cookie, password, connection string hoặc full environment dump.
- Ghi commit bằng `git rev-parse HEAD`, thời gian UTC bằng `date -u +%Y-%m-%dT%H:%M:%SZ` và tình trạng working tree bằng `git status --short`.
- Chỉ ghi `VERIFIED` khi không có test bắt buộc bị skip và artifact đúng profile tồn tại.

## 2. Prerequisites

| Thành phần | Phiên bản/điều kiện |
|---|---|
| Java | JDK 21 |
| Node/npm | Node 22 |
| Python | 3.11 hoặc 3.12; Sentiment thật cần extra `ml` |
| PostgreSQL/Supabase | Reachable và đã áp dụng migrations đến F010 |
| Redis | Reachable; local mặc định `127.0.0.1:6379` |
| Docker | Docker daemon chạy nếu dùng Sentiment compose |
| Auth | Development user Supabase hợp lệ |
| Network | Binance REST/WebSocket reachable cho profile LIVE |

Trên máy macOS hiện tại:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export PATH=/opt/homebrew/opt/node@22/bin:$PATH
java -version
node --version
redis-cli ping
```

Kết quả mong đợi: Java 21, Node 22 và Redis trả `PONG`.

Không dùng Python mặc định nếu `python3 --version` trả 3.13 trở lên vì package Sentiment khai báo
`>=3.11,<3.13`. Nếu Docker không dùng được, tạo runtime local bằng đúng Python 3.11/3.12:

```bash
python3.12 -m venv .venv-f014-sentiment
source .venv-f014-sentiment/bin/activate
python -m pip install -e './apps/sentiment[ml]'
```

`.venv-f014-sentiment` chỉ là thư mục local, không commit vào repository.

## 3. Cấu hình local an toàn

Tạo file local không tracked từ tên biến trong `.env.example` và `apps/web/.env.example`. Không commit giá trị thật.

Server-side bắt buộc:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
SUPABASE_JWT_ISSUER
SUPABASE_JWT_JWKS_URI
SUPABASE_JWT_AUDIENCE
SENTIMENT_SERVICE_TOKEN
SENTIMENT_BUNDLE_PATH
```

Browser-safe:

```text
NEXT_PUBLIC_SUPABASE_URL
NEXT_PUBLIC_SUPABASE_ANON_KEY
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXT_PUBLIC_ENABLE_FIXTURES=false
```

Live overrides:

```bash
export PLATFORM_MARKET_DATA_PROVIDER=binance
export PLATFORM_FEATURE_SEARCH_START_ENABLED=true
export PLATFORM_FEATURE_SEARCH_REPRODUCE_ENABLED=true
export PLATFORM_SECURITY_ALLOWED_ORIGINS=http://localhost:3000
export NEWS_ENABLED=true
export SENTIMENT_SERVICE_URL=http://127.0.0.1:8000
```

Nếu Redis không chạy local, inject `WORKER_REDIS_HOST`, `WORKER_REDIS_PORT`, optional username/password/SSL cho Worker và cấu hình Spring Data Redis tương ứng cho API. Mỗi Worker song song phải có `WORKER_CONSUMER_CONSUMER_NAME` khác nhau.

## 4. Preflight database và quality smoke

Database phải có tối thiểu:

- `experiment.backtest_result.job_id` từ migration F006;
- `search.search_run` và `search.reproduction_verification` từ migration F010;
- constraint correlation ID mới nhất.

Không tự sửa bảng bằng SQL ad-hoc. Nếu thiếu migration, dừng live run, giao database owner chạy migration theo quy trình của môi trường rồi chạy lại.

Kiểm tra read-only ba contract schema bắt buộc mà không in credential:

```bash
set -a
source .env.local
set +a
export PGPASSWORD="$DATABASE_PASSWORD"
psql "${DATABASE_URL#jdbc:}" -U "$DATABASE_USERNAME" -v ON_ERROR_STOP=1 -X -Atc "
select check_name || '=' || passed
from (values
  ('f006_backtest_job_id', exists(
    select 1 from information_schema.columns
    where table_schema='experiment' and table_name='backtest_result' and column_name='job_id'
  )),
  ('f010_search_run', to_regclass('search.search_run') is not null),
  ('f010_reproduction_verification', to_regclass('search.reproduction_verification') is not null)
) checks(check_name, passed);"
unset PGPASSWORD
```

Kết quả mong đợi là cả ba dòng đều `=true`. Lệnh chỉ đọc schema; việc apply migration vẫn thuộc
database owner và quy trình trong `infra/database/README.md`.

Focused preflight không ghi secret ra terminal:

```bash
set -a
source .env.local
set +a
./gradlew :apps:api:test --tests '*DocumentationParityTest' --no-daemon
cd apps/web
npm ci
npm run typecheck
cd ../..
```

## 5. Startup order

Mở mỗi runtime ở một terminal riêng để giữ log/timestamp độc lập.

### Terminal A — PostgreSQL và Redis

PostgreSQL/Supabase có thể là shared service. Start Redis theo cách cài đặt của máy, sau đó:

```bash
redis-cli ping
```

### Terminal B — Sentiment

Khuyến nghị dùng image vì Dockerfile cài đúng extra ML:

```bash
set -a
source .env.local
set +a
docker compose -f infra/compose/docker-compose.yml up --build sentiment
```

Ở terminal kiểm tra:

```bash
curl -fsS http://127.0.0.1:8000/health/live
curl -fsS http://127.0.0.1:8000/health/ready
```

Chỉ tiếp tục News/Sentiment live khi readiness trả `READY` cùng contract/model version. `LIVE` chỉ chứng minh process sống, không chứng minh model đã load.

Nếu Docker daemon không khả dụng nhưng đã tạo Python 3.11/3.12 environment ở bước prerequisites:

```bash
set -a
source .env.local
set +a
export SENTIMENT_BUNDLE_PATH="$(pwd)/apps/sentiment/artifacts/active_release"
uvicorn app.main:create_app --factory --app-dir apps/sentiment --host 127.0.0.1 --port 8000
```

Trên macOS ARM, PyPI không phát hành `tensorflow-cpu==2.19.0`. Có thể tạo `.venv` local bị ignore,
cài core/test dependencies rồi cài `numpy==2.1.3 tensorflow==2.19.0` để kiểm tra cùng model bundle;
không sửa `pyproject.toml`, vì image Linux production vẫn dùng extra `ml` với `tensorflow-cpu`.

Không dùng cách local này nếu `SENTIMENT_SERVICE_TOKEN` chưa được inject hoặc model dependency chưa
cài thành công. Bundle checked-in giúp tái lập model; service token vẫn chỉ nằm ngoài repository.

### Terminal C — API

```bash
set -a
source .env.local
set +a
export PLATFORM_MARKET_DATA_PROVIDER=binance
export PLATFORM_FEATURE_SEARCH_START_ENABLED=true
export PLATFORM_FEATURE_SEARCH_REPRODUCE_ENABLED=true
export PLATFORM_SECURITY_ALLOWED_ORIGINS=http://localhost:3000
./gradlew :apps:api:bootRun --no-daemon
```

### Terminal D — Worker

```bash
set -a
source .env.local
set +a
export NEWS_ENABLED=true
export SENTIMENT_SERVICE_URL=http://127.0.0.1:8000
export WORKER_CONSUMER_CONSUMER_NAME=worker-demo-1
./gradlew :apps:worker:bootRun --no-daemon
```

### Terminal E — Web

```bash
cd apps/web
export NEXT_PUBLIC_ENABLE_FIXTURES=false
npm run dev
```

## 6. Health/readiness gate

```bash
curl -fsS http://127.0.0.1:8080/actuator/health/liveness
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
curl -fsS http://127.0.0.1:8081/actuator/health/liveness
curl -fsS http://127.0.0.1:8081/actuator/health/readiness
curl -fsS http://127.0.0.1:8000/health/ready
curl -fsSI http://127.0.0.1:3000/login
```

Tất cả phải thành công. API/Worker readiness hiện chủ yếu chứng minh database; vẫn phải thực hiện Redis, Sentiment và browser smoke riêng.

## 7. Setup dữ liệu demo

1. Mở `http://localhost:3000/login` và đăng nhập development user.
2. Mở Market, chọn `BTC/USDT` và xác nhận dữ liệu ghi rõ Binance/live.
3. Mở Search và dùng nút `Create dataset` để tạo immutable dataset từ pair/timeframe/khoảng thời gian đã chọn.
4. Ghi Dataset ID và checksum từ Result provenance sau khi chạy; không dùng ID tự bịa.
5. Xác nhận Strategy catalog có MA, RSI, Bollinger Bands, Support/Resistance.
6. Nếu demo personal/composite Strategy, tạo, publish version rồi ghi User Strategy version ID.

## 8. Main live flow

Mục tiêu hoàn thành trong 10 phút.

1. **Market**: mở bốn chart, đổi timeframe của một chart và xác nhận ba chart còn lại không đổi; chụp ảnh trạng thái live/freshness.
2. **Strategy**: mở catalog bốn Strategy; tạo/publish một personal hoặc composite Strategy, chọn conflict rule; chụp catalog + published version.
3. **Search**: chọn Dataset, `random-search@1.0.0`, seed cố định, search space nhỏ, `maximumCandidates=3`, duration hữu hạn, Top-K; nhấn Start.
4. Ghi Experiment ID và correlation ID đã redact; quan sát `QUEUED/RUNNING` đến `COMPLETED` hoặc `STOPPED`.
5. **Leaderboard**: mở revision terminal, ghi revision ID/fingerprint, chọn entry đầu.
6. **Result**: xác nhận bốn metrics, Entry/Exit, Trades, capital/fees, Experiment/Candidate/Job/successful Attempt.
7. **Provenance**: chụp Dataset version/checksum, Strategy ID/version/parameters, Candidate definition/fingerprint, assumptions, software/git commit.
8. **Reproduction**: quay lại source Experiment terminal, nhấn `Reproduce Experiment`, mở target ID mới, xác nhận liên kết source và chờ `MATCHED` hoặc `MISMATCHED`; chụp ba comparison flags.
9. **News**: mở News, xác nhận ít nhất một item `ANALYZED`, sentiment label/confidence/polarity và disclaimer.
10. Ghi timing, IDs và screenshot links vào `docs/evidence/f014/main-flow.md` và `reproduction.md` bằng record mới; không sửa/xóa record thất bại cũ.

## 9. Failure scenario A — Sentiment isolation/recovery

1. Khi stack đang chạy, mở News và ghi một News ID đã `ANALYZED`.
2. Tắt riêng Sentiment:

   ```bash
   docker compose -f infra/compose/docker-compose.yml stop sentiment
   ```

3. Giữ API/Worker/Web. Reload News; xác nhận nội dung News còn đọc được và UI hiện degraded/`FAILED_RETRYABLE`, không dựng sentiment giả.
4. Mở Market và một Result kỹ thuật đã có; xác nhận hai phần vẫn dùng được.
5. Start lại Sentiment:

   ```bash
   docker compose -f infra/compose/docker-compose.yml start sentiment
   curl -fsS http://127.0.0.1:8000/health/ready
   ```

6. Chờ retry boundary; xác nhận News chuyển `ANALYZING` rồi `ANALYZED` từ authoritative API.
7. Lưu timestamp, News ID, lease/correlation ID đã redact và ba ảnh trước lỗi/trong lỗi/sau recovery vào `docs/evidence/f014/failure-recovery.md`.

## 10. Failure scenario B — Worker/Redis reclaim và duplicate safety

1. Dùng Search nhỏ nhưng đủ để còn job đang chạy. Ghi Experiment/Job/correlation ID.
2. Khi Backtest job đã được consume nhưng chưa terminal, dừng Worker bằng `Ctrl-C`; không xóa PostgreSQL hoặc Redis.
3. Chờ pending idle boundary. Với demo nhanh có thể start Worker từ đầu bằng `WORKER_CONSUMER_PENDING_IDLE_TIME=5s`, nhưng phải ghi override vào evidence.
4. Start Worker mới với consumer name khác:

   ```bash
   export WORKER_CONSUMER_CONSUMER_NAME=worker-demo-recovery
   export WORKER_CONSUMER_PENDING_IDLE_TIME=5s
   ./gradlew :apps:worker:bootRun --no-daemon
   ```

5. Xác nhận consumer mới reclaim, Job đạt terminal và chỉ có một accepted Result cho Job.
6. Kiểm tra Experiment, Job, Result, News/Sentiment và publication state vẫn đọc được. Redis là transport; không được mất durable records khi Worker restart.
7. Lưu timeline, message/job/correlation IDs đã redact, duplicate count và recovery result vào `docs/evidence/f014/failure-recovery.md`.

Dependency-backed automated cross-check:

```bash
F014_REDIS_SMOKE=true F009_REDIS_SMOKE=true \
./gradlew :apps:api:test \
  --tests com.cryptostrategy.platform.api.news.NewsDegradedIsolationTest \
  --tests com.cryptostrategy.platform.api.realtime.RealtimeRedisRecoveryIntegrationTest \
  :apps:worker:test \
  --tests com.cryptostrategy.platform.worker.engine.F014RecoveryScenarioTest \
  --rerun-tasks --no-daemon
```

## 11. Controlled fallback/rehearsal

Fallback này kiểm tra UI/contract khi live dependency bị chặn. Nó không chứng minh Binance, PostgreSQL, Redis hay model ML thật.

```bash
cd apps/web
NEXT_PUBLIC_ENABLE_FIXTURES=false npm run test:e2e -- \
  tests/e2e/f014-market-demo.spec.ts \
  tests/e2e/f014-research-flow.spec.ts \
  tests/e2e/f014-news-demo.spec.ts \
  tests/e2e/f014-failure-recovery.spec.ts \
  tests/e2e/f014-reproduction.spec.ts \
  --project=desktop
```

Mọi ảnh lấy từ suite này phải ghi `CONTROLLED/TEST`, không ghi `LIVE`.

## 12. Cleanup

1. Dừng Web, Worker và API bằng `Ctrl-C` ở đúng terminal.
2. Dừng Sentiment container/network:

   ```bash
   docker compose -f infra/compose/docker-compose.yml down
   ```

3. Không xóa shared PostgreSQL data, Redis toàn cục hoặc user account. Chỉ cleanup record demo bằng command/API được dự án phát hành hoặc theo quy trình database owner.
4. Kiểm tra không còn process listen ngoài ý muốn:

   ```bash
   lsof -nP -iTCP:3000 -sTCP:LISTEN
   lsof -nP -iTCP:8080 -sTCP:LISTEN
   lsof -nP -iTCP:8081 -sTCP:LISTEN
   lsof -nP -iTCP:8000 -sTCP:LISTEN
   ```

5. Redact artifact trước khi upload Drive hoặc commit. Giữ lại failed/blocked record để reviewer thấy lịch sử remediation.

## 13. Known blockers tại lần soạn runbook

- Shared PostgreSQL đã được reconcile đến migration `20260904000100`; postflight và dependency-backed reproduction đã pass.
- Local chưa có browser Supabase credentials/development session để chạy authenticated journey.
- External Sentiment model đã load/inference/stop/restart thành công bằng Python 3.12 local; vẫn thiếu phiên authenticated LIVE chứng minh Market và technical Backtest tiếp tục hoạt động trong lúc service down.
- Ba redaction failure ở baseline đã được remediation; clean candidate gate và secret scan đã pass theo `docs/evidence/f014/final-commit-verification.md`.

Không đổi các blocker trên thành pass cho tới khi có một Evidence Record chạy lại trên đúng commit/environment.
