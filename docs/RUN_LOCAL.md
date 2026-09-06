# Chạy code mới nhất trên máy local (Windows PowerShell)

Hướng dẫn này dành cho repository `D:\KienTrucPhanMem`, kể cả khi workspace đang
có nhiều file thay đổi nhưng chưa muốn commit.

Quy trình sử dụng một runtime worktree ở `D:\CodexTemp`. Trước mỗi lần build,
các thay đổi hiện tại trong workspace (file sửa, file mới và file đã xóa) được
đồng bộ sang runtime worktree. Vì vậy JAR được build chứa code mới nhất mà không
cần `commit`, `stash` hoặc `git add`.

> Không chạy `git worktree add ... HEAD` rồi build ngay. Worktree vừa tạo chỉ có
> nội dung của commit `HEAD`, chưa có các thay đổi chưa commit trong workspace.

## Quy trình nhanh cho những lần mở đồ án tiếp theo

Nếu worktree đã được tạo và file
`D:\CodexTemp\current-crypto-run-tree.txt` vẫn tồn tại, thực hiện theo thứ tự:

1. Chạy **Bước 0** để dừng API/Worker cũ còn sót.
2. Mở Redis theo **Terminal 1** và kiểm tra kết quả `PONG`.
3. Chạy nguyên khối **Bước 2.2**. Khối này tự nạp `$sourceRoot` và `$runTree`;
   không cần khai báo lại bằng tay.
4. Build API và Worker theo phần **Terminal 2**. Chỉ tiếp tục sau
   `BUILD SUCCESSFUL`.
5. Mở ba terminal riêng và lần lượt chạy **API**, **Worker**, **Web**.
6. Chạy kiểm tra sức khỏe trong **Terminal 6**.

Nếu file lưu worktree bị thiếu hoặc báo worktree không còn tồn tại, chạy lại
**Bước 2.1** một lần rồi tiếp tục từ Bước 2.2.

> Khi sao chép lệnh, chỉ sao chép nội dung bên trong khung code. Không sao chép
> dòng mở/đóng khung code, dấu nhắc `PS D:\...>` hoặc ký tự `>`.

## Yêu cầu

- Docker Desktop đang chạy với Linux engine.
- JDK 21, Node.js 22 và npm.
- Có `.env.local` tại root repository với đủ ba biến kết nối Supabase:

```dotenv
DATABASE_URL=jdbc:postgresql://<supabase-host>:5432/<database>
DATABASE_USERNAME=<username>
DATABASE_PASSWORD=<password>
```

  Không ghi credential thật vào `RUN_LOCAL.md` và không commit `.env.local`.
  API đã bật `reWriteBatchedInserts` qua Hikari để tăng tốc lưu Frozen Dataset theo
  batch. Không cần thêm tham số này vào `DATABASE_URL` và không cần tăng connection pool.
- Có `apps\web\.env.local` với tối thiểu:

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_ENABLE_FIXTURES=false
```

Tất cả lệnh bên dưới chạy trong terminal PowerShell của VS Code. Không sao chép
dấu nhắc như `PS D:\...>` hoặc ký tự `>` vào câu lệnh.

## Bước 0 — Dừng API và Worker cũ

Trước khi build/chạy lại, dừng mọi API và Worker JAR cũ để tránh trùng cổng và
tránh chiếm hết database connection của Supabase:

```powershell
$services = Get-CimInstance Win32_Process | Where-Object {
  $_.Name -eq 'java.exe' -and
  $_.CommandLine -match 'apps\\(api|worker)\\build\\libs\\.*\.jar'
}

$services | Select-Object ProcessId, CommandLine
$services | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

Kiểm tra cổng `8080` và `8081` đã trống:

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
  Where-Object LocalPort -In 8080, 8081 |
  Select-Object LocalPort, OwningProcess
```

Nếu lệnh không in dòng nào thì hai cổng đã trống. Redis, Sentiment và tiến trình
Java không phải API/Worker của repository sẽ không bị dừng.

## Terminal 1 — Redis

```powershell
Set-Location D:\KienTrucPhanMem
docker info
```

Nếu chưa có Redis local:

```powershell
docker run -d --rm --name crypto-strategy-redis -p 6379:6379 redis:7-alpine
```

Nếu Docker báo container đã tồn tại thì không tạo thêm. Kiểm tra:

```powershell
docker exec crypto-strategy-redis redis-cli ping
```

Kết quả phải là `PONG`.

## Terminal 2 — Tạo runtime worktree và đồng bộ code chưa commit

### 2.1. Tạo worktree mới

```powershell
Set-Location D:\KienTrucPhanMem

$sourceRoot = (Resolve-Path '.').Path
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runTree = "D:\CodexTemp\crypto-strategy-run-$stamp"

New-Item -ItemType Directory -Path 'D:\CodexTemp' -Force | Out-Null
git worktree add --detach $runTree HEAD

if ($LASTEXITCODE -ne 0) {
  throw "Unable to create runtime worktree: $runTree"
}

Set-Content `
  -LiteralPath 'D:\CodexTemp\current-crypto-run-tree.txt' `
  -Value $runTree

Write-Host "RUN_TREE=$runTree"
```

### 2.2. Đồng bộ mọi thay đổi hiện tại sang worktree

Đoạn này sao chép file tracked đã sửa, file mới chưa tracked và xử lý file đã
xóa. Nó không thay đổi workspace chính. Khối lệnh này tự khởi tạo các biến cần
thiết, vì vậy có thể chạy độc lập trong một terminal PowerShell mới:

```powershell
Set-Location D:\KienTrucPhanMem

$sourceRoot = (Resolve-Path -LiteralPath '.').Path
$runTreeFile = 'D:\CodexTemp\current-crypto-run-tree.txt'

if (-not (Test-Path -LiteralPath $runTreeFile -PathType Leaf)) {
  throw "Runtime worktree pointer is missing. Run Step 2.1 first: $runTreeFile"
}

$runTree = (Get-Content -LiteralPath $runTreeFile -Raw).Trim()

if ([string]::IsNullOrWhiteSpace($runTree) -or
    -not (Test-Path -LiteralPath $runTree -PathType Container)) {
  throw "Runtime worktree does not exist. Run Step 2.1 first: $runTree"
}

$sourceHead = (git -C $sourceRoot rev-parse HEAD).Trim()
$runTreeHead = (git -C $runTree rev-parse HEAD).Trim()

if ($LASTEXITCODE -ne 0 -or $sourceHead -ne $runTreeHead) {
  throw "Runtime worktree is based on another commit. Create a new worktree from Step 2.1."
}

$copyFiles = @(
  git -c core.quotepath=false diff HEAD --name-only --no-renames --diff-filter=ACMRTUXB
  git -c core.quotepath=false ls-files --others --exclude-standard
) | Where-Object { $_ } | Sort-Object -Unique

foreach ($relativePath in $copyFiles) {
  $source = Join-Path $sourceRoot $relativePath
  $destination = Join-Path $runTree $relativePath
  $destinationDirectory = Split-Path -Parent $destination

  if (Test-Path -LiteralPath $source -PathType Leaf) {
    New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $destination -Force
  }
}

$deletedFiles = @(
  git -c core.quotepath=false diff HEAD --name-only --no-renames --diff-filter=D
) | Where-Object { $_ } | Sort-Object -Unique

$resolvedRunRoot = [System.IO.Path]::GetFullPath($runTree).TrimEnd('\') + '\'
foreach ($relativePath in $deletedFiles) {
  $destination = [System.IO.Path]::GetFullPath((Join-Path $runTree $relativePath))
  if (-not $destination.StartsWith($resolvedRunRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to remove a path outside runtime worktree: $destination"
  }
  if (Test-Path -LiteralPath $destination -PathType Leaf) {
    Remove-Item -LiteralPath $destination -Force
  }
}
```

Kiểm tra worktree đã nhận thay đổi:

```powershell
git -C $runTree status --short

Test-Path `
  -LiteralPath "$runTree\apps\api\src\main\java\com\cryptostrategy\platform\api\experiment\CandidatePipelineCursor.java"

Select-String `
  -LiteralPath "$runTree\apps\api\src\main\java\com\cryptostrategy\platform\api\experiment\ExperimentController.java" `
  -Pattern 'candidate-pipeline'
```

Kết quả phải liệt kê các file đang sửa/mới tương tự workspace chính.
Hai lệnh kiểm tra cuối phải trả về `True` và dòng chứa
`@GetMapping("/{id}/candidate-pipeline")` khi tính năng Candidate Pipeline có
trong workspace. Đường dẫn worktree đã được lưu ở Bước 2.1 nên các terminal mới
không phải nhớ `$runTree`.

## Terminal 2 — Build API và Worker

Vẫn trong terminal trên:

```powershell
Push-Location $runTree

.\gradlew.bat --% :apps:api:bootJar :apps:worker:bootJar --no-daemon --no-parallel --no-build-cache --no-configuration-cache -Dorg.gradle.workers.max=1 -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false

Pop-Location
```

Chỉ tiếp tục khi có dòng:

```text
BUILD SUCCESSFUL
```

Các cảnh báo `serialVersionUID` không làm build thất bại. Nếu có `error:` hoặc
`BUILD FAILED`, chưa chạy JAR vì JAR có thể vẫn là bản build cũ.

Kiểm tra hai JAR mới tồn tại:

```powershell
Get-Item `
  "$runTree\apps\api\build\libs\api-0.1.0-SNAPSHOT.jar", `
  "$runTree\apps\worker\build\libs\worker-0.1.0-SNAPSHOT.jar" |
  Select-Object FullName, LastWriteTime, Length
```

## Khi tiếp tục sửa code sau lần build đầu tiên

Không cần tạo worktree mới nếu `HEAD` chưa thay đổi:

1. Dừng API và Worker bằng **Bước 0**.
2. Chạy lại toàn bộ **Bước 2.2**; không cần tự khai báo biến trước.
3. Chạy lại lệnh build.

Nếu đã commit/chuyển branch làm `HEAD` thay đổi, hãy xóa worktree cũ sau khi các
service đã dừng và tạo worktree mới từ Bước 2.1.

## Terminal 3 — Chạy API tại cổng 8080

Mở terminal mới:

```powershell
Set-Location D:\KienTrucPhanMem
$runTree = (Get-Content -LiteralPath 'D:\CodexTemp\current-crypto-run-tree.txt').Trim()

if (-not (Test-Path -LiteralPath "$runTree\apps\api\build\libs\api-0.1.0-SNAPSHOT.jar")) {
  throw "API JAR not found. Build must finish successfully first."
}
```

Nạp biến môi trường backend:

```powershell
Get-Content .env.local | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
    $name, $value = $line.Split('=', 2)
    Set-Item "Env:$($name.Trim())" $value.Trim().Trim('"').Trim("'")
  }
}

$requiredDatabaseVariables = @(
  'DATABASE_URL',
  'DATABASE_USERNAME',
  'DATABASE_PASSWORD'
)

$missingDatabaseVariables = @(
  $requiredDatabaseVariables | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process'))
  }
)

if ($missingDatabaseVariables.Count -gt 0) {
  throw "Missing database variables in root .env.local: $($missingDatabaseVariables -join ', ')"
}

# application.yml đọc DATABASE_*. Các alias SPRING_DATASOURCE_* dưới đây được đặt
# rõ ràng để Spring Boot và lệnh chẩn đoán đều nhìn thấy cùng một cấu hình.
$env:SPRING_DATASOURCE_URL = $env:DATABASE_URL
$env:SPRING_DATASOURCE_USERNAME = $env:DATABASE_USERNAME
$env:SPRING_DATASOURCE_PASSWORD = $env:DATABASE_PASSWORD

$requiredDatabaseVariables | ForEach-Object {
  "$_=SET"
}
```

Chạy đúng một API với pool nhỏ phù hợp Supabase session pool:

```powershell
$env:PLATFORM_MARKET_DATA_PROVIDER = 'binance'
$env:PLATFORM_FEATURE_SEARCH_START_ENABLED = 'true'
$env:PLATFORM_FEATURE_SEARCH_REPRODUCE_ENABLED = 'true'
$env:PLATFORM_SECURITY_ALLOWED_ORIGINS = 'http://localhost:3000'
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = '2'
$env:SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = '0'

java -jar "$runTree\apps\api\build\libs\api-0.1.0-SNAPSHOT.jar"
```

API sẵn sàng khi có cả hai dòng:

```text
Tomcat started on port 8080
Started ApiApplication
```

Giữ terminal này mở. Không gõ lệnh mới trong terminal trong khi API đang chạy.

## Terminal 4 — Chạy Worker tại cổng 8081

Mở terminal mới:

```powershell
Set-Location D:\KienTrucPhanMem
$runTree = (Get-Content -LiteralPath 'D:\CodexTemp\current-crypto-run-tree.txt').Trim()

if (-not (Test-Path -LiteralPath "$runTree\apps\worker\build\libs\worker-0.1.0-SNAPSHOT.jar")) {
  throw "Worker JAR not found. Build must finish successfully first."
}
```

Nạp `.env.local`:

```powershell
Get-Content .env.local | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
    $name, $value = $line.Split('=', 2)
    Set-Item "Env:$($name.Trim())" $value.Trim().Trim('"').Trim("'")
  }
}

$requiredDatabaseVariables = @(
  'DATABASE_URL',
  'DATABASE_USERNAME',
  'DATABASE_PASSWORD'
)

$missingDatabaseVariables = @(
  $requiredDatabaseVariables | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process'))
  }
)

if ($missingDatabaseVariables.Count -gt 0) {
  throw "Missing database variables in root .env.local: $($missingDatabaseVariables -join ', ')"
}

# Worker dùng cùng datasource Supabase với API.
$env:SPRING_DATASOURCE_URL = $env:DATABASE_URL
$env:SPRING_DATASOURCE_USERNAME = $env:DATABASE_USERNAME
$env:SPRING_DATASOURCE_PASSWORD = $env:DATABASE_PASSWORD

$requiredDatabaseVariables | ForEach-Object {
  "$_=SET"
}
```

Chạy đúng một Worker. Với luồng Search/Backtest, tắt News mặc định để giảm kết
nối database và không phụ thuộc Sentiment:

```powershell
$env:NEWS_ENABLED = 'false'
$env:WORKER_CONSUMER_CONSUMER_NAME = 'worker-local-1'
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = '2'
$env:SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = '0'

java -jar "$runTree\apps\worker\build\libs\worker-0.1.0-SNAPSHOT.jar"
```

Worker sẵn sàng khi có cả hai dòng:

```text
Tomcat started on port 8081
Started WorkerApplication
```

Giữ terminal này mở.

### Tùy chọn: bật News và Sentiment

Chỉ bật sau khi API/Worker Search chạy ổn và database còn connection. Trước tiên
chạy Sentiment:

```powershell
Set-Location D:\KienTrucPhanMem
docker compose -f infra/compose/docker-compose.yml up -d --build sentiment
```

Sau đó dừng Worker bằng `Ctrl+C` và chạy lại với:

```powershell
$env:NEWS_ENABLED = 'true'
$env:SENTIMENT_SERVICE_URL = 'http://127.0.0.1:8000'
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = '2'
$env:SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = '0'

java -jar "$runTree\apps\worker\build\libs\worker-0.1.0-SNAPSHOT.jar"
```

## Terminal 5 — Chạy Web tại cổng 3000

Web chạy trực tiếp từ workspace chính nên luôn dùng source frontend mới nhất:

```powershell
Set-Location D:\KienTrucPhanMem\apps\web
npm run dev
```

Chỉ cần chạy `npm ci` khi dependencies thay đổi hoặc `node_modules` chưa tồn tại:

```powershell
npm ci
```

Mở ứng dụng tại:

```text
http://localhost:3000/login
```

Luôn dùng thống nhất `localhost`, không trộn với `127.0.0.1` khi đăng nhập.

## Terminal 6 — Kiểm tra toàn hệ thống

```powershell
$urls = @(
  'http://localhost:8080/actuator/health/liveness',
  'http://localhost:8080/actuator/health/readiness',
  'http://localhost:8081/actuator/health/liveness',
  'http://localhost:8081/actuator/health/readiness',
  'http://localhost:3000/login'
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

Năm URL phải trả về `200`. Khi bật News/Sentiment, kiểm tra thêm:

```powershell
Invoke-WebRequest http://127.0.0.1:8000/health/live -UseBasicParsing
Invoke-WebRequest http://127.0.0.1:8000/health/ready -UseBasicParsing
```

Kiểm tra chỉ có đúng một API và một Worker:

```powershell
Get-CimInstance Win32_Process | Where-Object {
  $_.Name -eq 'java.exe' -and
  $_.CommandLine -match 'apps\\(api|worker)\\build\\libs\\.*\.jar'
} | Select-Object ProcessId, CommandLine
```

## Xử lý các lỗi thường gặp

### `EMAXCONNSESSION: max clients reached in session mode`

Database Supabase session pool đã hết connection. Đây không phải lỗi Search hay
Backtest.

1. Chạy **Bước 0** để dừng mọi API/Worker dư.
2. Đóng SQL Editor hoặc database client không dùng.
3. Chờ 15–60 giây để connection được giải phóng.
4. Chạy lại đúng một API và một Worker với `maximum-pool-size=2`,
   `minimum-idle=0`.
5. Để `NEWS_ENABLED=false` nếu chỉ kiểm tra Search/Backtest.

### API có `Started ApiApplication` nhưng PowerShell trở về dấu nhắc

Nếu Java process vẫn đang chạy, terminal sẽ không trở về dấu nhắc. Nếu đã trở
về `PS ...>`, xem các dòng sau `Started ApiApplication` hoặc kiểm tra cổng:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
```

### Monitor báo `Candidate progress is temporarily unavailable`

Mở DevTools → Network và kiểm tra endpoint:

```text
GET /api/v1/experiments/{id}/candidate-pipeline?view=RESULTS&limit=10
```

Nếu `404`, API đang chạy JAR cũ. Dừng service, đồng bộ lại Bước 2.2, build thành
công và chạy JAR mới.

## Dừng hệ thống và xóa runtime worktree

Nhấn `Ctrl+C` trong terminal Web, API và Worker. Có thể dùng **Bước 0** để dọn
process API/Worker còn sót.

Dừng Sentiment nếu đã bật:

```powershell
Set-Location D:\KienTrucPhanMem
docker compose -f infra/compose/docker-compose.yml down
```

Chỉ dừng Redis nếu container này do bạn tạo cho đồ án:

```powershell
docker stop crypto-strategy-redis
```

Sau khi chắc chắn API và Worker đã dừng:

```powershell
$runTree = (Get-Content -LiteralPath 'D:\CodexTemp\current-crypto-run-tree.txt').Trim()
$resolvedRunTree = (Resolve-Path -LiteralPath $runTree).Path
$allowedRoot = (Resolve-Path -LiteralPath 'D:\CodexTemp').Path.TrimEnd('\') + '\'

if (-not $resolvedRunTree.StartsWith($allowedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to remove worktree outside D:\CodexTemp: $resolvedRunTree"
}

Set-Location D:\KienTrucPhanMem
git worktree remove $resolvedRunTree
git worktree prune
Remove-Item -LiteralPath 'D:\CodexTemp\current-crypto-run-tree.txt' -Force
```
