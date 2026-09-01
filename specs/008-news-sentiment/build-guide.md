# Hướng dẫn chạy và test Sentiment Model (F-008)

Tài liệu này dành cho các thành viên trong nhóm (Teammate) để khởi chạy và kiểm thử Sentiment AI Model bằng Docker. 

**🚨 LƯU Ý QUAN TRỌNG:**
1. **Tuyệt đối KHÔNG cần build lại hay train lại Model.** File model.keras hiện tại (phiên bản 1.0.0) đã được huấn luyện sẵn và đóng gói hoàn chỉnh.
2. Code Backend chỉ làm nhiệm vụ **Inference (Dự đoán)**, không có chức năng Train. Mọi thao tác chỉnh sửa Model phải được thực hiện trên Kaggle/Jupyter Notebook, sau đó mới dùng công cụ đóng gói để nạp vào hệ thống.

---

## 1. Khởi chạy hệ thống bằng Docker Compose

Yêu cầu: Đã cài đặt Docker và Docker Compose.

### Bước 1: Chuẩn bị file .env
Đảm bảo bạn đang đứng ở **thư mục gốc của dự án** (crypto-strategy-platform).
Kế tiếp, hãy chắc chắn file .env của bạn có chứa các biến môi trường bắt buộc sau:

``env
SENTIMENT_SERVICE_TOKEN=crypto_secret_token_123
NEWS_ENABLED=true
NEWS_AUDIT_SERVICE_TOKEN=audit_secret_token
``

### Bước 2: Chạy Container
Mở Terminal/PowerShell tại thư mục gốc và chạy lệnh sau để khởi động Sentiment Service ở chế độ chạy ngầm (detached):

``bash
docker compose -f infra/compose/docker-compose.yml --env-file .env up -d sentiment
``

### Bước 3: Kiểm tra trạng thái
Đảm bảo container đang chạy ổn định ở port 8000:
``bash
docker ps | findstr sentiment
``
*(Nếu dùng macOS/Linux, thay indstr bằng grep)*

---

## 2. Hướng dẫn Test API (Test Cases)

Dưới đây là các Test Case mẫu bằng PowerShell (curl.exe). 
**Lưu ý:** Các ID (equestId, 
ewsId) bắt buộc phải là chuỗi ULID dài chuẩn **26 ký tự** (chỉ gồm số và chữ in hoa).

### Test Case 1: Tin cực kỳ Tích Cực (Positive)
Dự đoán: POSITIVE (Độ tự tin > 75%)

``powershell
$body = @{
  requestId = "01H4F8G5K9PQRZTVWX2B3C4D51"
  newsId = "01H4F8G5K9PQRZTVWX2B3C4D52"
  title = "Ethereum ETF approved by SEC"
  content = "Huge news for the crypto community as the SEC finally approves Ethereum ETFs. Markets are rallying massively across the board."
  language = "en"
  contentHash = "sha256:1111111111111111111111111111111111111111111111111111111111111111"
  contractVersion = "sentiment-v1"
  modelName = "multichannel-english"
  modelVersion = "1.0.0"
  preprocessingVersion = "multichannel-whitespace-en-1"
} | ConvertTo-Json -Compress

$body | curl.exe -X POST http://127.0.0.1:8000/api/v1/sentiment/analyze 
  -H "Authorization: Bearer crypto_secret_token_123" 
  -H "Content-Type: application/json" 
  --data-binary "@-"
``

### Test Case 2: Tin cực kỳ Tiêu Cực (Negative / Hack)
Dự đoán: NEGATIVE (Độ tự tin > 50%)

``powershell
$body = @{
  requestId = "01H4F8G5K9PQRZTVWX2B3C4D53"
  newsId = "01H4F8G5K9PQRZTVWX2B3C4D54"
  title = "Major crypto exchange hacked"
  content = "A top cryptocurrency exchange has lost millions of dollars in a catastrophic security breach. Prices are plummeting in panic selling."
  language = "en"
  contentHash = "sha256:2222222222222222222222222222222222222222222222222222222222222222"
  contractVersion = "sentiment-v1"
  modelName = "multichannel-english"
  modelVersion = "1.0.0"
  preprocessingVersion = "multichannel-whitespace-en-1"
} | ConvertTo-Json -Compress

$body | curl.exe -X POST http://127.0.0.1:8000/api/v1/sentiment/analyze 
  -H "Authorization: Bearer crypto_secret_token_123" 
  -H "Content-Type: application/json" 
  --data-binary "@-"
``

### Test Case 3: Lỗi Validation (Sai Model Version)
Dự đoán: Trả về HTTP 422 / Lỗi INVALID_REQUEST vì khai báo sai phiên bản Model.

``powershell
$body = @{
  requestId = "01H4F8G5K9PQRZTVWX2B3C4D55"
  newsId = "01H4F8G5K9PQRZTVWX2B3C4D56"
  title = "Testing model mismatch validation"
  content = "This should fail because we send the wrong expected model version."
  language = "en"
  contentHash = "sha256:3333333333333333333333333333333333333333333333333333333333333333"
  contractVersion = "sentiment-v1"
  modelName = "multichannel-english"
  modelVersion = "99.9.9"  # Cố tình truyền sai version
  preprocessingVersion = "multichannel-whitespace-en-1"
} | ConvertTo-Json -Compress

$body | curl.exe -X POST http://127.0.0.1:8000/api/v1/sentiment/analyze 
  -H "Authorization: Bearer crypto_secret_token_123" 
  -H "Content-Type: application/json" 
  --data-binary "@-"
``

---

## 3. Khắc phục sự cố thường gặp (Troubleshooting)

- **Lỗi missing a value: required khi chạy Docker:** Bạn đang chạy Docker ở thư mục con. Hãy đảm bảo bạn dùng lệnh cd ra ngoài cùng thư mục gốc của dự án (crypto-strategy-platform), nơi chứa file .env.
- **Lỗi Request validation failed:** Đảm bảo equestId và 
ewsId của bạn có độ dài đúng 26 ký tự chuẩn ULID. Không được điền tuỳ tiện chữ thường hoặc ký tự đặc biệt.
- **CURL báo lỗi kết nối:** Đảm bảo Docker container đã được start thành công và đang listen ở port 8000 (docker logs compose-sentiment-1 để xem log).
