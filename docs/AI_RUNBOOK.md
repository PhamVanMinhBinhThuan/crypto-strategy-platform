# Runbook cho AI chạy Crypto Strategy Platform trên Windows

Tài liệu này dành cho AI/agent cần khởi động toàn bộ đồ án từ repository root trên Windows PowerShell. Không in nội dung `.env.local`, không đưa secret vào log và không commit các file môi trường.

## 1. Kiến trúc runtime

| Thành phần | Cổng | Cách chạy |
|---|---:|---|
| Web (Next.js) | 3000 | `npm run dev` |
| Sentiment (FastAPI + TensorFlow) | 8000 | Docker Compose |
| API (Spring Boot) | 8080 | Executable JAR |
| Worker (Spring Boot) | 8081 | Executable JAR |
| Redis | 6379 | Docker |
| PostgreSQL và Auth | Cloud | Supabase của nhóm |
| Market data | Internet | Binance REST + WebSocket |

API phải được chạy với `PLATFORM_MARKET_DATA_PROVIDER=binance`; nếu không, giá trị mặc định trong `application.yml` là `fixture`.

## 2. Kiểm tra trước khi chạy

Yêu cầu:

- JDK 21
- Node.js 22 và npm
- Docker Desktop với Linux engine
- `.env.local` ở root chứa cấu hình backend
- `apps/web/.env.local` chứa cấu hình frontend
- Supabase cloud đã được áp dụng migration và seed cần thiết

Chỉ kiểm tra tên biến, tuyệt đối không in giá trị:

```powershell
java -version
node --version
npm --version
docker --version
docker info --format '{{.ServerVersion}}'

$backendRequired = @(
  'DATABASE_URL', 'DATABASE_USERNAME', 'DATABASE_PASSWORD',
  'SUPABASE_JWT_ISSUER', 'SUPABASE_JWT_JWKS_URI', 'SUPABASE_JWT_AUDIENCE',
  'SENTIMENT_BUNDLE_PATH', 'SENTIMENT_SERVICE_TOKEN',
  'NEWS_ENABLED', 'NEWS_AUDIT_SERVICE_TOKEN'
)

$backendNames = Get-Content -LiteralPath '.env.local' |
  Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*\s*=' } |
  ForEach-Object { ($_ -split '=', 2)[0].Trim() }

$backendRequired | ForEach-Object {
  if ($_ -in $backendNames) { "OK $_" } else { "MISSING $_" }
}
```

Kiểm tra cổng trước khi start:

```powershell
Get-NetTCPConnection -State Listen -LocalPort 3000,8000,8080,8081,6379 `
  -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, OwningProcess
```

Không tạo thêm service nếu cổng đã được sử dụng. Xác định process/container hiện có trước.

## 3. Hàm nạp `.env.local` cho PowerShell

Spring/Gradle không tự đọc `.env.local`. Dùng đoạn sau trong mỗi PowerShell process chạy API/Worker hoặc trước Docker Compose:

```powershell
Get-Content -LiteralPath '.env.local' | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
    $parts = $line.Split('=', 2)
    $value = $parts[1].Trim()
    if (($value.StartsWith("'") -and $value.EndsWith("'")) -or
        ($value.StartsWith('"') -and $value.EndsWith('"'))) {
      $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($parts[0].Trim(), $value, 'Process')
  }
}
```

## 4. Khởi động Docker Desktop và Redis

Nếu Docker daemon chưa chạy:

```powershell
Start-Process -FilePath 'C:\Program Files\Docker\Docker\Docker Desktop.exe' `
  -WindowStyle Hidden
```

Chờ đến khi `docker info` thành công. Sau đó kiểm tra Redis đang có:

```powershell
$redisContainer = docker ps --filter 'publish=6379' --format '{{.Names}}' |
  Select-Object -First 1

if ($redisContainer) {
  docker exec $redisContainer redis-cli ping
} else {
  docker run -d --rm --name crypto-strategy-redis -p 6379:6379 redis:7-alpine
  docker exec crypto-strategy-redis redis-cli ping
}
```

Kết quả phải là `PONG`. Có thể dùng Redis container đang chạy sẵn, nhưng phải ghi lại tên container và không tự ý dừng container thuộc dự án khác.

## 5. Khởi động Sentiment và xử lý lỗi checksum CRLF

Build lần đầu tải `tensorflow-cpu` khoảng 252 MB và có thể mất nhiều thời gian. Chạy Compose sau khi đã nạp `.env.local`:

```powershell
docker compose -f infra/compose/docker-compose.yml up -d --build sentiment
```

Kiểm tra:

```powershell
Invoke-WebRequest http://127.0.0.1:8000/health/live -UseBasicParsing
Invoke-WebRequest http://127.0.0.1:8000/health/ready -UseBasicParsing
```

### Lỗi đã gặp: container restart/exit code 1 nhưng log chỉ có Uvicorn startup

`ModelRuntime` gọi `os._exit(1)` khi load bundle thất bại nên traceback không xuất hiện trong log. Chạy loader trực tiếp để lấy nguyên nhân:

```powershell
docker run --rm `
  -v 'D:\KienTrucPhanMem\apps\sentiment\artifacts\active_release:/opt/sentiment/bundle:ro' `
  compose-sentiment `
  python -c "from pathlib import Path; from app.model.runtime import ModelRuntime; print(ModelRuntime._load_bundle(Path('/opt/sentiment/bundle')))"
```

Trên checkout Windows đã gặp:

```text
ValueError: Checksum mismatch: vocabulary.json
```

Nguyên nhân là Git chuyển `vocabulary.json` từ LF sang CRLF. Không sửa model bundle trong workspace chỉ để chạy. Tạo bản runtime tạm và chuẩn hóa file JSON sang LF:

```powershell
$runtimeBundle = 'D:\CodexTemp\sentiment-runtime\active_release'
New-Item -ItemType Directory -Path $runtimeBundle -Force | Out-Null
Copy-Item -LiteralPath 'apps\sentiment\artifacts\active_release\manifest.json' `
  -Destination $runtimeBundle -Force
Copy-Item -LiteralPath 'apps\sentiment\artifacts\active_release\model.keras' `
  -Destination $runtimeBundle -Force
Copy-Item -LiteralPath 'apps\sentiment\artifacts\active_release\vocabulary.json' `
  -Destination $runtimeBundle -Force

wsl.exe bash -lc "sed -i 's/\r//' /mnt/d/CodexTemp/sentiment-runtime/active_release/vocabulary.json"

$manifest = Get-Content "$runtimeBundle\manifest.json" -Raw | ConvertFrom-Json
$expected = $manifest.checksums.'vocabulary.json'
$actual = 'sha256:' + (Get-FileHash "$runtimeBundle\vocabulary.json" -Algorithm SHA256).Hash.ToLower()
if ($actual -ne $expected) { throw "Sentiment vocabulary checksum mismatch" }

$env:SENTIMENT_BUNDLE_PATH = $runtimeBundle
docker compose -f infra/compose/docker-compose.yml up -d --force-recreate sentiment
```

Giải pháp lâu dài cho repository là thêm quy tắc LF phù hợp vào `.gitattributes`, nhưng không tự sửa nếu nhiệm vụ chỉ là chạy ứng dụng.

## 6. Build API và Worker ổn định trên Windows

### Lỗi đã gặp

VS Code Java/Gradle extension chạy build nền trên cùng `build-logic`, làm mất generated sources/cache trong lúc CLI build. Các lỗi đã thấy:

```text
Unable to parse script-resolver-environment argument implicit-imports=...
Source file or directory not found: ...PluginSpecBuilders.kt
...accessors is not a directory
EOFException trong Kotlin incremental storage
```

Không chạy đồng thời hai lệnh Gradle `bootRun` trong cùng workspace. Cách ổn định là:

1. Tạo detached worktree tạm bên ngoài workspace để VS Code không can thiệp.
2. Build cả API và Worker thành executable JAR trong một lệnh tuần tự.
3. Chạy hai JAR bằng `java -jar`, không giữ hai Gradle build sống cùng lúc.

```powershell
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runTree = "D:\CodexTemp\crypto-strategy-run-$stamp"
New-Item -ItemType Directory -Path 'D:\CodexTemp' -Force | Out-Null
git worktree add --detach $runTree HEAD

Push-Location $runTree
.\gradlew.bat --% :apps:api:bootJar :apps:worker:bootJar --no-daemon --no-parallel --no-build-cache --no-configuration-cache -Dorg.gradle.workers.max=1 -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false
Pop-Location
```

Không build API và Worker tiếp nếu lệnh trên chưa báo `BUILD SUCCESSFUL`.

Nếu vẫn build trực tiếp trong workspace, cần dừng các Gradle/Kotlin daemon nền trước; không dừng Java process ứng dụng đang chạy:

```powershell
.\gradlew.bat --stop
Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
  Where-Object { $_.CommandLine -match 'KotlinCompileDaemon|GradleDaemon' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## 7. Chạy API và Worker từ JAR

Trong hai PowerShell process riêng, nạp `.env.local` bằng hàm ở mục 3 rồi đặt override.

API:

```powershell
$env:PLATFORM_MARKET_DATA_PROVIDER = 'binance'
$env:PLATFORM_FEATURE_SEARCH_START_ENABLED = 'true'
$env:PLATFORM_FEATURE_SEARCH_REPRODUCE_ENABLED = 'true'
$env:PLATFORM_SECURITY_ALLOWED_ORIGINS = 'http://localhost:3000'
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = '5'
$env:SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = '1'
java -jar "$runTree\apps\api\build\libs\api-0.1.0-SNAPSHOT.jar"
```

Worker:

```powershell
$env:NEWS_ENABLED = 'true'
$env:SENTIMENT_SERVICE_URL = 'http://127.0.0.1:8000'
$env:WORKER_CONSUMER_CONSUMER_NAME = 'worker-local-1'
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = '5'
$env:SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = '1'
java -jar "$runTree\apps\worker\build\libs\worker-0.1.0-SNAPSHOT.jar"
```

AI dùng process/session nền phải giữ các session này sống đến khi người dùng yêu cầu dừng dự án.

## 8. Chạy Web

```powershell
Set-Location apps\web
npm ci
npm run dev
```

Nếu `node_modules` đã đúng với lockfile thì có thể bỏ qua `npm ci`. `apps/web/.env.local` phải có:

```dotenv
NEXT_PUBLIC_ENABLE_FIXTURES=false
```

Biến này chỉ tắt fixture phía Web; API vẫn phải có `PLATFORM_MARKET_DATA_PROVIDER=binance` để dùng Binance thật.

## 9. Xác minh toàn hệ thống

```powershell
$urls = @(
  'http://127.0.0.1:8080/actuator/health/liveness',
  'http://127.0.0.1:8080/actuator/health/readiness',
  'http://127.0.0.1:8081/actuator/health/liveness',
  'http://127.0.0.1:8081/actuator/health/readiness',
  'http://127.0.0.1:8000/health/live',
  'http://127.0.0.1:8000/health/ready',
  'http://127.0.0.1:3000/login'
)

foreach ($url in $urls) {
  try {
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
    "$($response.StatusCode) $url"
  } catch {
    "FAIL $url :: $($_.Exception.Message)"
  }
}
```

Chỉ báo chạy thành công khi cả bảy URL trả `200`. URL sử dụng là:

```text
http://localhost:3000/login
```

## 10. Dừng và dọn runtime

- Dừng Web, API và Worker bằng `Ctrl+C` hoặc kết thúc đúng process/session đã khởi động.
- Dừng Sentiment:

```powershell
docker compose -f infra/compose/docker-compose.yml down
```

- Chỉ dừng Redis nếu chính quy trình này đã tạo container `crypto-strategy-redis`. Không dừng Redis của dự án khác.
- Sau khi toàn bộ Java process chạy từ worktree tạm đã dừng, gỡ worktree bằng lệnh chạy tại repository chính:

```powershell
git worktree remove '<đường-dẫn-runTree>'
git worktree prune
```

Không xóa worktree khi API/Worker còn chạy từ JAR bên trong đó.

