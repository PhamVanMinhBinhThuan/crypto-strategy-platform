# 🚀 Hướng dẫn Setup, Cài đặt và Build

Tài liệu này hướng dẫn chuẩn bị môi trường phát triển, cài dependencies và build toàn bộ **Crypto Strategy Lab** từ một bản clone mới. Các lệnh mặc định chạy tại thư mục gốc repository, trừ khi có ghi chú khác.

## 📑 Mục lục

- [🏗️ Kiến trúc runtime](#runtime-architecture)
- [🧰 Yêu cầu hệ thống](#system-requirements)
- [📥 Clone repository](#clone-repository)
- [⚙️ Cấu hình môi trường](#environment-configuration)
- [📦 Cài đặt dependencies](#dependency-installation)
- [🗄️ Chuẩn bị PostgreSQL và Redis](#data-infrastructure)
- [🔨 Build dự án](#build-project)
- [▶️ Chạy ứng dụng sau khi build](#run-application)
- [🧪 Chạy kiểm thử](#testing)
- [✅ Kiểm tra hệ thống](#health-checks)
- [🛠️ Xử lý lỗi thường gặp](#troubleshooting)
- [🔐 Quy tắc bảo mật](#security-rules)

<a id="runtime-architecture"></a>

## 🏗️ Kiến trúc runtime

| Thành phần          | Công nghệ                        |                Port mặc định | Vai trò                                            |
| ------------------- | -------------------------------- | ---------------------------: | -------------------------------------------------- |
| `apps/web`          | Next.js 16, React 19, TypeScript |                       `3000` | Dashboard và Supabase Auth client                  |
| `apps/api`          | Java 21, Spring Boot 3.5         |                       `8080` | REST, WebSocket, JWT và orchestration              |
| `apps/worker`       | Java 21, Spring Boot 3.5         |                       `8081` | Search, Backtest, Evaluation, Ranking và News jobs |
| `apps/sentiment`    | Python 3.11/3.12, FastAPI        |                       `8000` | Sentiment inference độc lập                        |
| PostgreSQL/Supabase | PostgreSQL                       | `5432` hoặc managed endpoint | Business source of truth và Outbox                 |
| Redis               | Redis Streams                    |                       `6379` | Queue, Consumer Groups và transient events         |

```text
Browser → Web → API → PostgreSQL
                 ↕
               Redis → Worker → Sentiment
                 ↑        ↓
              realtime  News RSS
```

<a id="system-requirements"></a>

## 🧰 Yêu cầu hệ thống

| Công cụ             | Phiên bản/điều kiện                  | Kiểm tra                 |
| ------------------- | ------------------------------------ | ------------------------ |
| Git                 | Bản còn được hỗ trợ                  | `git --version`          |
| Java                | JDK 21                               | `java -version`          |
| Node.js             | 22.x                                 | `node --version`         |
| npm                 | Đi kèm Node 22                       | `npm --version`          |
| Python              | 3.11 hoặc 3.12                       | `python --version`       |
| Docker              | Docker Desktop/Engine đang chạy      | `docker version`         |
| Docker Compose      | Compose v2                           | `docker compose version` |
| PostgreSQL/Supabase | Có database và quyền chạy migrations | kiểm tra theo môi trường |

Gradle không cần cài toàn cục; repository dùng **Gradle Wrapper 8.14.5**. Sentiment có thể chạy hoàn toàn bằng Docker, khi đó không bắt buộc cài Python local.

### 🪟 Kiểm tra nhanh trên Windows PowerShell

```powershell
git --version
java -version
node --version
npm --version
python --version
docker version
docker compose version
```

### 🐧 Kiểm tra nhanh trên macOS/Linux/WSL

```bash
git --version
java -version
node --version
npm --version
python3 --version
docker version
docker compose version
```

<a id="clone-repository"></a>

## 📥 Clone repository

```bash
git clone https://github.com/PhamVanMinhBinhThuan/crypto-strategy-platform.git
cd crypto-strategy-platform
git status
```

Nếu đã có repository:

```bash
git switch main
git pull origin main
```

Không chạy `git pull` khi còn thay đổi local chưa được commit/stash nếu chưa kiểm tra khả năng conflict.

<a id="environment-configuration"></a>

## ⚙️ Cấu hình môi trường

### 1. 📄 Tạo file cấu hình local

Windows PowerShell:

```powershell
Copy-Item .env.example .env.local
Copy-Item apps/web/.env.example apps/web/.env.local
```

macOS/Linux/WSL:

```bash
cp .env.example .env.local
cp apps/web/.env.example apps/web/.env.local
```

Hai file `.env.local` đã được ignore và không được commit.

### 2. 🖥️ Cấu hình backend

Điền các placeholder trong `.env.local`:

```dotenv
DATABASE_URL=jdbc:postgresql://<host>:5432/<database>
DATABASE_USERNAME=<database-username>
DATABASE_PASSWORD=<database-password>

SUPABASE_JWT_ISSUER=https://<project-ref>.supabase.co/auth/v1
SUPABASE_JWT_JWKS_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_JWT_AUDIENCE=authenticated

PLATFORM_MARKET_DATA_PROVIDER=binance
PLATFORM_SECURITY_ALLOWED_ORIGINS=http://localhost:3000

SENTIMENT_BUNDLE_PATH=<absolute-path-to-apps/sentiment/artifacts/active_release>
SENTIMENT_SERVICE_TOKEN=<random-local-token>
SENTIMENT_MODEL_NAME=multichannel-english
SENTIMENT_MODEL_VERSION=multichannel-english-1.0.0
SENTIMENT_PREPROCESSING_VERSION=multichannel-whitespace-en-1
NEWS_ENABLED=true
```

API mặc định dùng provider `fixture` nếu không override. Đặt `PLATFORM_MARKET_DATA_PROVIDER=binance` khi cần dữ liệu live và máy có thể truy cập Binance.

### 3. 🌐 Cấu hình frontend

Điền `apps/web/.env.local`:

```dotenv
NEXT_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=<public-anon-key>
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXT_PUBLIC_ENABLE_FIXTURES=false
```

Chỉ đặt giá trị an toàn cho browser trong biến `NEXT_PUBLIC_*`. Không đặt database password, service-role key hoặc `SENTIMENT_SERVICE_TOKEN` tại đây.

### 4. 🔑 Nạp `.env.local` vào terminal

Spring Boot và Docker Compose không tự đọc file `.env.local` ở root. Nạp biến trước khi chạy API, Worker hoặc Sentiment.

Windows PowerShell:

```powershell
Get-Content .env.local | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
    $name, $value = $line.Split('=', 2)
    Set-Item "Env:$($name.Trim())" $value.Trim().Trim('"').Trim("'")
  }
}
```

macOS/Linux/WSL:

```bash
set -a
source .env.local
set +a
```

Mỗi terminal mới cần nạp lại các biến backend.

<a id="dependency-installation"></a>

## 📦 Cài đặt dependencies

### ☕ Java

Xác nhận Gradle Wrapper và danh sách project:

Windows PowerShell:

```powershell
.\gradlew.bat --version
.\gradlew.bat projects
```

macOS/Linux/WSL:

```bash
./gradlew --version
./gradlew projects
```

Wrapper tự tải Gradle và Maven dependencies trong lần chạy đầu. Cần kết nối mạng ở lần này.

### 🌐 Web

```bash
cd apps/web
npm ci
cd ../..
```

Luôn ưu tiên `npm ci` để cài đúng dependency versions từ `package-lock.json`.

### 🐍 Sentiment chạy local không dùng Docker

Windows PowerShell:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -e "apps/sentiment[test,ml]"
```

macOS/Linux/WSL:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -e "apps/sentiment[test,ml]"
```

Python 3.13 không được hỗ trợ bởi `apps/sentiment/pyproject.toml`. Trên một số máy macOS ARM, `tensorflow-cpu==2.19.0` không có wheel phù hợp; nên dùng Docker hoặc xem [F014 Demo Runbook](demo/f014/runbook.md).

<a id="data-infrastructure"></a>

## 🗄️ Chuẩn bị PostgreSQL và Redis

### 🐘 PostgreSQL/Supabase

Nguồn migration duy nhất của project là:

```text
supabase/migrations/
```

Database owner phải áp dụng đầy đủ migrations theo thứ tự trước khi chạy API/Worker. Không sửa trực tiếp schema trên dashboard và không chỉnh lại migration đã chạy ở môi trường dùng chung. Xem quy trình tại [infra/database/README.md](../infra/database/README.md).

### 🔴 Redis bằng Docker

```bash
docker run --rm --name crypto-strategy-redis -p 6379:6379 redis:7-alpine
```

Mở terminal khác để kiểm tra:

```bash
docker exec crypto-strategy-redis redis-cli ping
```

Kết quả mong đợi:

```text
PONG
```

Worker mặc định kết nối `localhost:6379`. Khi chạy nhiều Worker, mỗi instance phải có `WORKER_CONSUMER_CONSUMER_NAME` riêng.

<a id="build-project"></a>

## 🔨 Build dự án

### ☕ Java — build toàn bộ

Windows PowerShell:

```powershell
.\gradlew.bat clean check
```

macOS/Linux/WSL:

```bash
./gradlew clean check
```

Root `check` chạy verification của toàn bộ Java subprojects, `architecture-tests` và `build-logic`. Các integration tests cần external database là task riêng và không tự động chạy trong lệnh trên.

### 📦 Java — chỉ build API và Worker JAR

Windows PowerShell:

```powershell
.\gradlew.bat :apps:api:bootJar :apps:worker:bootJar
```

macOS/Linux/WSL:

```bash
./gradlew :apps:api:bootJar :apps:worker:bootJar
```

Artifacts được tạo tại:

```text
apps/api/build/libs/api-0.1.0-SNAPSHOT.jar
apps/worker/build/libs/worker-0.1.0-SNAPSHOT.jar
```

### 🌐 Web — quality gate và production build

```bash
cd apps/web
npm ci
npm run check
```

`npm run check` lần lượt chạy Prettier check, ESLint, TypeScript typecheck, Vitest và `next build`. Nếu chỉ cần build:

```bash
npm run build
```

Production output nằm trong `apps/web/.next/`.

### 🐳 Sentiment — Docker image

Chạy tại root repository:

```bash
docker build -f apps/sentiment/Dockerfile -t crypto-strategy-sentiment:local .
```

Dockerfile cài Python 3.11 cùng extra `ml` và đóng gói FastAPI runtime.

### 🎯 Build tối thiểu cho từng phần

| Phần thay đổi         | Lệnh kiểm tra/build tối thiểu                                                    |
| --------------------- | -------------------------------------------------------------------------------- |
| Java domain/module    | `./gradlew :modules:<module>:check`                                              |
| API                   | `./gradlew :apps:api:check :apps:api:bootJar`                                    |
| Worker                | `./gradlew :apps:worker:check :apps:worker:bootJar`                              |
| Architecture boundary | `./gradlew :architecture-tests:test`                                             |
| Web                   | `cd apps/web && npm run check`                                                   |
| Sentiment             | `cd apps/sentiment && python -m pytest`                                          |
| Sentiment image       | `docker build -f apps/sentiment/Dockerfile -t crypto-strategy-sentiment:local .` |

<a id="run-application"></a>

## ▶️ Chạy ứng dụng sau khi build

Mở terminal riêng cho từng runtime và nạp `.env.local` tại các terminal backend.

### 🧠 Sentiment

```bash
docker compose -f infra/compose/docker-compose.yml up --build sentiment
```

Compose yêu cầu `SENTIMENT_SERVICE_TOKEN` và `SENTIMENT_BUNDLE_PATH`.

### 🔌 API từ Gradle

```bash
./gradlew :apps:api:bootRun --no-daemon
```

Hoặc chạy JAR đã build:

```bash
java -jar apps/api/build/libs/api-0.1.0-SNAPSHOT.jar
```

### ⚙️ Worker từ Gradle

```bash
export NEWS_ENABLED=true
export SENTIMENT_SERVICE_URL=http://127.0.0.1:8000
export WORKER_CONSUMER_CONSUMER_NAME=worker-local-1
./gradlew :apps:worker:bootRun --no-daemon
```

PowerShell:

```powershell
$env:NEWS_ENABLED = 'true'
$env:SENTIMENT_SERVICE_URL = 'http://127.0.0.1:8000'
$env:WORKER_CONSUMER_CONSUMER_NAME = 'worker-local-1'
.\gradlew.bat :apps:worker:bootRun --no-daemon
```

Hoặc chạy JAR đã build:

```bash
java -jar apps/worker/build/libs/worker-0.1.0-SNAPSHOT.jar
```

### 💻 Web development

```bash
cd apps/web
npm run dev
```

Mở `http://localhost:3000/login`.

### 🌍 Web production build

```bash
cd apps/web
npm run build
npm run start
```

<a id="testing"></a>

## 🧪 Chạy kiểm thử

### ☕ Java unit, contract và architecture tests

```bash
./gradlew test
./gradlew :architecture-tests:test
```

### 🔗 Integration tests với PostgreSQL/Supabase

Nạp `DATABASE_URL`, `DATABASE_USERNAME` và `DATABASE_PASSWORD` trước khi chạy:

```bash
./gradlew :modules:persistence:marketDataIntegrationTest
./gradlew :modules:persistence:strategyIntegrationTest
./gradlew :modules:persistence:experimentIntegrationTest
./gradlew :modules:persistence:backtestEvaluationLeaderboardIntegrationTest
./gradlew :modules:persistence:newsIntegrationTest
./gradlew supabaseIntegrationTest
```

### 📊 Worker controlled performance tests

```bash
./gradlew :apps:worker:performanceTest -Pf015Performance=true
```

Đây là controlled profile, không phải production benchmark. Phải đọc kết quả cùng [F-015 evidence](evidence/f015/).

### 🌐 Web tests

```bash
cd apps/web
npm run format:check
npm run lint
npm run typecheck
npm test
npm run build
```

Browser E2E:

```bash
npm run test:e2e
```

### 🐍 Sentiment tests

```bash
python -m pip install -e "apps/sentiment[test]"
cd apps/sentiment
python -m pytest
```

<a id="health-checks"></a>

## ✅ Kiểm tra hệ thống

Sau khi các runtime khởi động:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health/liveness
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
curl -fsS http://127.0.0.1:8081/actuator/health/liveness
curl -fsS http://127.0.0.1:8081/actuator/health/readiness
curl -fsS http://127.0.0.1:8000/health/live
curl -fsS http://127.0.0.1:8000/health/ready
```

API/Worker readiness chủ yếu xác nhận database. Redis, Sentiment, Binance và authenticated browser flow vẫn cần smoke test riêng.

<a id="troubleshooting"></a>

## 🛠️ Xử lý lỗi thường gặp

### ☕ Sai phiên bản Java

Triệu chứng: Gradle báo không tìm được Java toolchain 21.

```bash
java -version
```

Cài JDK 21 hoặc đặt `JAVA_HOME` trỏ đúng JDK rồi mở terminal mới.

### 🟢 Sai phiên bản Node.js

`apps/web/package.json` yêu cầu Node `>=22 <23` và `.nvmrc` ghi `22`.

```bash
node --version
```

Sau khi đổi Node, xóa dependency cũ theo quy trình của package manager rồi chạy lại `npm ci`.

### 🐘 Gradle `build-logic` lỗi generated source/cache trên Windows

Dừng Gradle daemon và chạy lại build tuần tự, không dùng cache:

```powershell
.\gradlew.bat --stop
.\gradlew.bat --% clean check --no-daemon --no-parallel --no-build-cache --no-configuration-cache -Dorg.gradle.workers.max=1 -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false
```

Nếu workspace đồng bộ cloud/IDE vẫn khóa generated files, dùng isolated runtime worktree theo [Windows Run Guide](RUN_LOCAL.md) thay vì xóa source hoặc sửa Gradle scripts tùy tiện.

### 🔴 Redis không kết nối được

```bash
docker ps
docker exec crypto-strategy-redis redis-cli ping
```

Xác nhận port `6379` không bị process khác chiếm. API dùng Spring Data Redis config; Worker mặc định dùng `localhost:6379`.

### 🧠 Sentiment không `READY`

```bash
docker compose -f infra/compose/docker-compose.yml ps
docker compose -f infra/compose/docker-compose.yml logs sentiment
```

Kiểm tra `SENTIMENT_BUNDLE_PATH`, model bundle, token và Docker volume mount. `live` chỉ chứng minh process sống; `ready` mới cho biết model đã sẵn sàng nhận inference.

### ⚙️ API hoặc Worker không sẵn sàng

Kiểm tra ba biến database, migrations và giới hạn connection pool. Không chạy nhiều API/Worker cũ cùng lúc với chung Supabase connection limit.

### 🔐 Web trả `401`

Đảm bảo frontend và backend dùng cùng Supabase project; kiểm tra `NEXT_PUBLIC_SUPABASE_*` và `SUPABASE_JWT_*`, sau đó restart Web/API.

### 🚧 Port đã được sử dụng

Các port mặc định: Web `3000`, Redis `6379`, Sentiment `8000`, API `8080`, Worker `8081`. Dừng đúng process/container cũ trước khi chạy lại.

<a id="security-rules"></a>

## 🔐 Quy tắc bảo mật

- Không commit `.env.local`, access token, cookie, database credential hoặc service token.
- Không in toàn bộ environment ra log hay artifact CI.
- Chỉ dùng Supabase anon/publishable key trong browser; không dùng service-role key.
- `SENTIMENT_SERVICE_TOKEN` ở Worker và Sentiment phải giống nhau nhưng chỉ tồn tại ngoài source control.
- Không sửa shared database bằng SQL ad-hoc; mọi thay đổi schema phải có forward migration.
- Không gọi fixture/controlled test là live evidence.

## 📚 Tài liệu liên quan

- [README tổng quan](../README.md)
- [Hướng dẫn chạy macOS/Linux/WSL](run.md)
- [Hướng dẫn chạy Windows PowerShell](RUN_LOCAL.md)
- [F014 Demo Runbook](demo/f014/runbook.md)
- [Database Infrastructure](../infra/database/README.md)
- [Architecture Documentation](architecture/README.md)
- [API Documentation](api/README.md)
