Tôi đã hoàn tất việc thiết lập hệ thống xác thực và tích hợp Zalo Mini App. Dưới đây là tóm tắt các thay đổi:

1. Cấu trúc DTO mới
Tôi đã tạo các DTO cần thiết để xử lý dữ liệu đầu vào và đảm bảo Validation:

RegisterRequest: Hỗ trợ đăng ký với username, password, phone (định dạng VN) và zaloId.
ZaloLoginRequest: Chứa thông tin từ Zalo SDK (zaloId, fullName, avatar, phone).
AuthResponse: Trả về accessToken và refreshToken.
2. Triển khai AuthServiceImpl
Đăng ký: Mã hóa mật khẩu bằng BCrypt, kiểm tra trùng lặp và gán Role/Tenant mặc định.
Đăng nhập truyền thống: Xác thực bằng AuthenticationManager, trả về bộ Token JWT.
Zalo Login (SSO):
Kiểm tra zaloId trong hệ thống.
Nếu chưa có: Tự động tạo tài khoản mới (Auto Register).
Nếu đã có: Tiến hành đăng nhập và trả về Token mà không cần mật khẩu.
3. API Endpoints (AuthController)
Tôi đã tạo các điểm cuối sau:

POST /api/auth/register: Đăng ký tài khoản.
POST /api/auth/login/basic: Đăng nhập bằng username/password.
POST /api/auth/login/zalo: Đăng nhập/Đăng ký nhanh qua Zalo Mini App.
4. Bảo mật & Validation
JWT: Token chứa userId, role, tenantId và thời gian hết hạn (exp).
Validation:
username: 3-50 ký tự.
password: Tối thiểu 6 ký tự.
phone: Kiểm tra đúng định dạng số điện thoại Việt Nam.
Xử lý lỗi: Cập nhật GlobalExceptionHandler để trả về định dạng lỗi INVALID_INPUT khi dữ liệu đầu vào không hợp lệ.
5. Tích hợp Zalo Mini App
Hỗ trợ luồng: Mở Mini App → Lấy zaloId → Gọi API login/zalo. Hệ thống sẽ tự động nhận diện người dùng hoặc tạo mới nếu cần, giúp trải nghiệm người dùng liền mạch (SSO).

TIP

Bạn có thể cấu hình thêm jwt.expiration và jwt.refresh-expiration trong file application.properties để điều chỉnh thời gian sống của Token.

Bạn có thể kiểm tra các file đã được cập nhật hoặc tạo mới trong thư mục src/main/java/com/example/bezma/.

# Báo cáo Cập nhật hệ thống Auth (B2B Standard)

## 1. Các thay đổi chính (What's New)
*   **AuthMapper**: Cấu hình `IGNORE` cho các trường không tìm thấy và ánh xạ đúng `avatarZalo`/`subPhone`.
*   **UserRepository**: Thêm phương thức `findByPhone` để hỗ trợ cơ chế Mapping nhân viên.
*   **AuthResponse**: Trả thêm thông tin chi tiết người dùng (`user`) cùng với Token để Frontend hiển thị Profile ngay lập tức.
*   **JwtTokenProvider**: Sửa lỗi logic `tenantId` (chuyển từ lưu Name sang lưu Long ID) đảm bảo lọc dữ liệu đa doanh nghiệp chính xác.

## 2. Logic Auth chuẩn B2B (The Logic)
1.  **Whitelist**: Bỏ tính năng tự đăng ký. Chỉ những SĐT được Admin tạo trước mới có thể vào hệ thống.
2.  **Auto-mapping**:
    *   **Lần đầu**: Khớp `zaloId` dựa trên SĐT -> Lưu vào DB.
    *   **Lần sau**: Đăng nhập 1 chạm dựa trên `zaloId`.
3.  **Security**: Kiểm tra trạng thái `isActive` trước khi cấp Token (Chặn nhân viên bị sa thải/khóa tài khoản).

## 3. Lưu ý kỹ thuật
*   **JWT Claims**: Chứa `userId`, `tenantId` (Long), `role`, `authorities`.
*   **Zalo Login**: Hiện đang dùng `zaloId` trực tiếp. Cần nâng cấp lên `accessToken` Zalo khi lên Production