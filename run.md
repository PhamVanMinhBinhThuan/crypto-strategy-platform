# Chạy Crypto Strategy Lab trên máy local

Tài liệu này dành cho thành viên trong nhóm muốn chạy toàn bộ hệ thống thật gồm Web, API,
Worker, Redis, PostgreSQL/Supabase và Sentiment. Các lệnh bên dưới dùng cho macOS/Linux/WSL và
được chạy từ thư mục gốc của repository, trừ khi có ghi chú khác.

## 1. Yêu cầu

- JDK 21
- Node.js 22 và npm
- Docker Desktop hoặc Redis cài trực tiếp
- Quyền truy cập Supabase của nhóm
- Tài khoản Supabase Auth dùng để đăng nhập giao diện

Kiểm tra phiên bản:

```bash
java -version
node --version
npm --version
docker --version
```

Nếu máy macOS có nhiều phiên bản Java, có thể chọn JDK 21 bằng:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

## 2. Cấu hình môi trường

Tạo hai file cấu hình local:

```bash
cp .env.example .env.local
cp apps/web/.env.example apps/web/.env.local
```

Điền cấu hình backend vào `.env.local`:

```dotenv
DATABASE_URL=jdbc:postgresql://<host>:5432/<database>
DATABASE_USERNAME=<database-username>
DATABASE_PASSWORD=<database-password>

SUPABASE_JWT_ISSUER=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWT_JWKS_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_JWT_AUDIENCE=authenticated

SENTIMENT_BUNDLE_PATH=/absolute/path/to/crypto-strategy-platform/apps/sentiment/artifacts/active_release
SENTIMENT_SERVICE_TOKEN=<random-token>
NEWS_ENABLED=true
NEWS_AUDIT_SERVICE_TOKEN=<random-audit-token>
```

Có thể tạo token local bằng lệnh `openssl rand -hex 32`. Token
`SENTIMENT_SERVICE_TOKEN` trong Sentiment và Worker phải giống nhau.

Điền cấu hình frontend vào `apps/web/.env.local`:

```dotenv
NEXT_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=<publishable-anon-key>
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXT_PUBLIC_ENABLE_FIXTURES=false
```

Chỉ đặt biến `NEXT_PUBLIC_*` trong frontend. Không đưa database password, service-role key hoặc
service token vào `apps/web/.env.local`. Hai file `.env.local` đã được ignore và không được commit.

## 3. Cài dependency lần đầu

```bash
cd apps/web
npm ci
cd ../..
```

Gradle Wrapper sẽ tự tải dependency Java trong lần chạy đầu tiên.

Database dùng chung phải được người quản lý database áp dụng toàn bộ migration trong
`supabase/migrations/`. Không tự sửa schema trực tiếp trên Supabase Dashboard.

Nếu database chưa có cặp BTC/USDT dùng cho demo, người có quyền database chạy:

```bash
set -a
source .env.local
set +a
export PGPASSWORD="$DATABASE_PASSWORD"
psql "${DATABASE_URL#jdbc:}" -U "$DATABASE_USERNAME" \
  -v ON_ERROR_STOP=1 -X -f infra/database/seeds/f014-live-demo.sql
unset PGPASSWORD
```

Kết quả mong đợi: `f014_market_reference=ready`.

## 4. Khởi động hệ thống

Mở năm terminal riêng và giữ chúng chạy.

### Terminal 1 — Redis

Nếu đã cài Redis bằng Homebrew:

```bash
brew services start redis
redis-cli ping
```

Hoặc chạy Redis bằng Docker:

```bash
docker run --rm --name crypto-strategy-redis -p 6379:6379 redis:7-alpine
```

`redis-cli ping` phải trả về `PONG`.

### Terminal 2 — Sentiment service

```bash
set -a
source .env.local
set +a
docker compose -f infra/compose/docker-compose.yml up --build sentiment
```

Đợi container healthy rồi kiểm tra:

```bash
curl -fsS http://127.0.0.1:8000/health/live
curl -fsS http://127.0.0.1:8000/health/ready
```

### Terminal 3 — API

```bash
set -a
source .env.local
set +a
export PLATFORM_MARKET_DATA_PROVIDER=binance
export PLATFORM_FEATURE_SEARCH_START_ENABLED=true
export PLATFORM_FEATURE_SEARCH_REPRODUCE_ENABLED=true
export PLATFORM_SECURITY_ALLOWED_ORIGINS=http://localhost:3000
export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
export SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
./gradlew :apps:api:bootRun --no-daemon
```

Khi API khởi động, catalog MA, RSI, Bollinger Bands và Support/Resistance sẽ được đồng bộ vào
database.

### Terminal 4 — Worker

```bash
set -a
source .env.local
set +a
export NEWS_ENABLED=true
export SENTIMENT_SERVICE_URL=http://127.0.0.1:8000
export WORKER_CONSUMER_CONSUMER_NAME=worker-local-1
export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
export SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
./gradlew :apps:worker:bootRun --no-daemon
```

Nếu chạy thêm Worker, mỗi Worker phải có `WORKER_CONSUMER_CONSUMER_NAME` khác nhau.

### Terminal 5 — Web

```bash
cd apps/web
npm run dev
```

Mở [http://localhost:3000/login](http://localhost:3000/login) và đăng nhập bằng tài khoản
Supabase Auth của nhóm.

## 5. Kiểm tra hệ thống đã sẵn sàng

Chạy trong một terminal mới:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health/liveness
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
curl -fsS http://127.0.0.1:8081/actuator/health/liveness
curl -fsS http://127.0.0.1:8081/actuator/health/readiness
curl -fsS http://127.0.0.1:8000/health/ready
curl -fsSI http://127.0.0.1:3000/login
```

Sau khi đăng nhập, kiểm tra nhanh:

1. `Market Dashboard` hiển thị bốn biểu đồ BTC/USDT.
2. `Strategy Composer` hiển thị đủ bốn Strategy hệ thống.
3. Tạo Strategy riêng, chọn Strategy vừa tạo, mở form version mới, thay đổi ít nhất một tham số rồi
   lưu.
4. `Search & Leaderboard` tạo dataset và chạy một Search nhỏ.
5. `News Sentiment` hiển thị dữ liệu từ API thật.

## 6. Lỗi thường gặp

### API trả `401`

Kiểm tra ba biến `SUPABASE_JWT_*` ở backend và hai biến `NEXT_PUBLIC_SUPABASE_*` ở frontend có
cùng một Supabase project. Sau khi sửa `apps/web/.env.local`, phải khởi động lại Web.

### Tạo Strategy hoặc version trả `409`

Đảm bảo đang chạy source mới nhất và khởi động lại API. API phải hoàn thành bước đồng bộ Strategy
catalog khi startup. Version mới phải thay đổi ít nhất một tham số hoặc thành phần vì database không
lưu hai version có cùng fingerprint. Không bấm nút lưu liên tục; xem status code và correlation ID
trong log API.

### Supabase báo `max clients reached`

Đảm bảo API và Worker đều có:

```bash
export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
export SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
```

Tắt các API/Worker cũ đang chạy trùng trước khi start lại.

### Market báo chưa có Candle

Kiểm tra API đang dùng `PLATFORM_MARKET_DATA_PROVIDER=binance`, mạng truy cập được Binance và seed
BTC/USDT đã chạy thành công.

### Port đã được sử dụng

```bash
lsof -nP -iTCP:3000 -iTCP:8000 -iTCP:8080 -iTCP:8081 -iTCP:6379 -sTCP:LISTEN
```

Tắt đúng process cũ hoặc container cũ rồi chạy lại. Không mở hai API trên cùng port.

## 7. Dừng hệ thống

- Nhấn `Ctrl+C` tại terminal Web, API và Worker.
- Nhấn `Ctrl+C` tại Redis Docker nếu dùng chế độ foreground.
- Dừng Sentiment bằng:

```bash
docker compose -f infra/compose/docker-compose.yml down
```

Nếu Redis chạy bằng Homebrew và không còn sử dụng:

```bash
brew services stop redis
```

Runbook demo, failure/recovery và quy trình lấy minh chứng đầy đủ nằm tại
[`docs/demo/f014/runbook.md`](docs/demo/f014/runbook.md).
