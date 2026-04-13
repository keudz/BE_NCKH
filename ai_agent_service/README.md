# 🤖 Hệ Thống AI Agent Service (FastAPI)

Đây là microservice chịu trách nhiệm chạy các AI Agent, đóng vai trò "Bộ não" trong kiến trúc hệ thống, trực tiếp gọi đến các mô hình LLM (ví dụ: Gemma qua Ollama) bằng cơ chế Pipeline (Orchestrator Pattern).

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy

### 1. Chuẩn Bị Mô Hình (Ollama)
Service này được cấu hình mặc định để sử dụng LLM local thông qua [Ollama](https://ollama.com/). Trước khi khởi chạy service, hãy làm các bước sau:
1. Tải và cài đặt phần mềm Ollama (nếu chưa có).
2. Tải model `gemma` (hoặc `gemma:4b` tuỳ cấu hình của bạn) bằng lệnh:
   ```bash
   ollama pull gemma
   ```
3. Đảm bảo Ollama đang chạy ngầm trên port `11434` (mặc định).

### 2. Thiết Lập Môi Trường (Cho Python)
Trong thư mục `ai_agent_service`, chúng ta đã có thư mục `.env` có sẵn file cấu hình.
Nếu chưa có môi trường ảo (`venv`), bạn cần tạo và cài đặt module như sau:

Trên **Windows (PowerShell)**:
```powershell
# Di chuyển vào thư mục chứa service
cd d:\Code\PROJECT\NCKH_ZMA\BE_ZMA\BE_NCKH\ai_agent_service

# Kích hoạt môi trường ảo (nếu bạn sử dụng venv của folder này)
.\venv\Scripts\activate

# Cài đặt các thư viện (nếu chưa cài)
pip install -r requirements.txt
```

### 3. Cấu hình biến môi trường
Kiểm tra file `.env` ở trong thư mục này. Nó phải có nội dung tương tự:
```env
LLM_PROVIDER=ollama
LLM_OLLAMA_BASE_URL=http://localhost:11434
LLM_MODEL=gemma4:31b-cloud
IMAGE_PROVIDER=pollinations

BACKEND_URL=http://localhost:8080
```
> **Lưu ý**: Nếu bạn xài tag model khác (ví dụ `gemma:4b` thay vì `gemma`), hãy sửa `LLM_MODEL=gemma:4b` trong file này.

### 4. Khởi Chạy Service
Đảm bảo bạn vẫn đang ở trong thư mục `ai_agent_service` và đã kích hoạt môi trường `.venv` (sẽ thấy chữ `(venv)` ở đầu dòng lệnh terminal).

Khởi chạy bằng server Uvicorn với lệnh:
```bash
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```
Thành công là khi bạn thấy thông báo:
```text
INFO:     Uvicorn running on http://0.0.0.0:8001 (Press CTRL+C to quit)
```

Bạn có thể kiểm tra service đang chạy ổn định chưa bằng cách mở trình duyệt vào link:
👉 `http://localhost:8001/` hoặc xem tài liệu API chi tiết để test ngay trên web tại 👉 `http://localhost:8001/docs`

---

## 🔗 Liên Kết Với Spring Boot
Sau khi service Python này đã chạy trên cổng **8001**, bạn sẽ có thể khởi động Gateway backend bằng Spring Boot:
1. Mở một terminal/bảng lệnh khác.
2. Trỏ tới thư mục `BE_ZMA\BE_NCKH\` và chạy:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   Java Spring Boot backend sẽ tự động giao tiếp với AI Agent của Python mỗi khi gọi các Controller có route `/api/v1/agent/**`.
