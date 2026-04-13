Hệ thống AI Service (Python FastAPI)
ai_service/requirements.txt (Mới): Khai báo các thư viện AI chuyên sâu (deepface, opencv, tensorflow).
ai_service/main.py (Mới): Chứa logic AI cốt lõi (Phát hiện mặt, Căn chỉnh, Trích xuất vector, So khớp thông minh).
ai_service/doc.md (Mới): Tài liệu hướng dẫn cài đặt và quy trình vận hành AI (One-shot Learning).
2. Cấu hình & Bảo mật (Java Spring Boot)
src/main/resources/application.properties (Chỉnh sửa): Cấu hình URL kết nối tới AI Service (http://localhost:8000).
src/main/java/com/example/bezma/config/AppConfig.java (Mới): Thêm RestTemplate để Java có thể giao tiếp với Python.
src/main/java/com/example/bezma/security/JwtTokenProvider.java (Chỉnh sửa): Sửa lỗi lưu tenantId (từ String sang Long) và khôi phục trường subject.
src/main/java/com/example/bezma/dto/res/auth/AuthResponse.java (Chỉnh sửa): Trả thêm thông tin user profile ngay sau khi đăng nhập.
3. Cơ sở dữ liệu & Entity
src/main/java/com/example/bezma/entity/user/User.java (Chỉnh sửa): Thêm cột faceEmbedding để lưu "vân tay mặt" và isFaceRegistered.
src/main/java/com/example/bezma/entity/attendance/AttendanceStatus.java (Mới): Enum phân loại kết quả chấm công.
src/main/java/com/example/bezma/entity/attendance/Attendance.java (Mới): Lưu lịch sử chấm công (Thời gian, GPS, Kết quả).
src/main/java/com/example/bezma/repository/UserRepository.java (Chỉnh sửa): Thêm findByPhone để tìm người dùng nhanh.
src/main/java/com/example/bezma/repository/AttendanceRepository.java (Mới): Truy vấn lịch sử chấm công.
4. Logic Nghiệp vụ & Controller
src/main/java/com/example/bezma/mapper/AuthMapper.java (Chỉnh sửa): Map thông tin avatar/phone từ Zalo sang hệ thống.
src/main/java/com/example/bezma/service/impl/AuthServiceImpl.java (Chỉnh sửa): Tích hợp logic tìm kiếm và ánh xạ nhân viên thay vì tự đăng ký mới.
src/main/java/com/example/bezma/service/iService/IAttendanceService.java (Mới): Định nghĩa các hành động chấm công.
src/main/java/com/example/bezma/service/impl/AttendanceServiceImpl.java (Mới): Xử lý logic gọi AI Python, lưu trữ Vector vào MySQL và tính toán kết quả.
src/main/java/com/example/bezma/controller/AttendanceController.java (Mới): Cung cấp API bảo mật (lấy userId từ JWT) cho Mini App gọi vào.

