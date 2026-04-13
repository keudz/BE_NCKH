Trước khi cài thư viện 
kích hoạt môi trường ảo
.\venv\Scripts\activate

# 1. Cài đặt thư viện
pip install -r requirements.txt

python test_cam.py test nếu muốn
# 2. Chạy Service

python main.py


Để phát triển một ứng dụng B2B Hoàn chỉnh chứ không phải mức cơ bản, bạn cần nắm rõ quy trình vận hành của AI trong thực tế. Đây không chỉ là "gọi hàm" mà là cả một hệ thống xử lý (Pipeline).

Dưới đây là lời giải đáp chi tiết cho thắc mắc của bạn về Quy trình và cách thức "Train/Mapping" nhân viên mới:

1. Quy trình "Train" nhân viên mới (Vô cùng quan trọng trong CV)
Trong các hệ thống Face ID hiện đại, chúng ta không bao giờ "Train" (huấn luyện lại) mạng nơ-ron mỗi khi có một nhân viên mới. Đó là cách làm lỗi thời và cực kỳ tốn tài nguyên.

Thay vào đó, chúng ta sử dụng công nghệ Metric Learning (One-shot Learning):

Mô hình (Model): Đã được các "ông lớn" (như Meta, Google) train sẵn trên hàng triệu khuôn mặt để hiểu thế nào là các đặc điểm khuôn mặt (mắt, mũi, miệng, khoảng cách giữa chúng).
Khi có nhân viên mới (Registration):
Admin chụp 1 tấm ảnh của nhân viên.
Hệ thống chạy qua Model AI để trích xuất ra một Vector đặc trưng (Face Embedding) — ví dụ là một chuỗi 128 số (kiểu [0.12, -0.05, 0.88, ... ]).
Bạn chỉ việc lưu chuỗi số này vào Database (MySQL) cạnh tên nhân viên đó. Không cần train gì cả! Việc này chỉ mất khoảng 1 giây.
Khi nhân viên chấm công (Verification):
Hệ thống chụp ảnh lúc chấm công -> Trích xuất ra Vector mới.
Hệ thống tính toán khoảng cách (Distance) giữa Vector mới và Vector đã lưu trong DB. Nếu chúng "rất gần" nhau (nhỏ hơn một ngưỡng nhất định) -> Coi như là cùng 1 người.
2. OpenCV, YOLO dùng ở đâu trong quy trình Hoàn chỉnh?
Trong một hệ thống chuyên nghiệp, chúng ta kết hợp nhiều thư viện để tạo thành một Pipeline:

Zalo Mini App (Frontend): Chụp ảnh nhân viên -> Gửi ảnh thô về Server.
AI Service (Python):
YOLO (You Only Look Once): Được dùng ở bước đầu để "Face Detection" (Phát hiện vị trí khuôn mặt). YOLO cực kỳ nhanh để cắt bỏ phần phông nền, chỉ lấy đúng vùng mặt để AI xử lý chính xác hơn, tránh nhiễu do vật thể xung quanh.
OpenCV: Dùng để "Pre-processing" (Tiền xử lý). Ví dụ: Căn chỉnh khuôn mặt cho thẳng (Alignment), xoay ảnh, tăng độ sáng nếu ảnh tối, chuyển ảnh về kích thước chuẩn.
DeepFace (Brain): Sau khi OpenCV đã cắt và căn chỉnh ảnh đẹp đẽ, DeepFace mới làm nhiệm vụ cuối cùng là Trích xuất Vector (Embedding) hoặc so sánh.
Spring Boot (Logic): Nhận kết quả từ AI, check GPS, lưu bản ghi vào DB.
3. Tại sao cách làm này lại chuyên nghiệp?
Khả năng mở rộng: Bạn có 10 hay 10,000 nhân viên thì Model AI vẫn giữ nguyên, không cần train lại. Bạn chỉ việc thêm dòng dữ liệu vào DB.
Tốc độ: So sánh 2 chuỗi số (Vector) nhanh hơn hàng nghìn lần so với việc so sánh 2 tấm ảnh.
CV: Việc giải thích được cơ chế Face Embedding và Metric Learning sẽ chứng minh bạn thực sự hiểu về AI chứ không phải chỉ "copy-paste" code.

Pipeline chuyên nghiệp:

Ảnh thô (Raw) -> OpenCV Face Detection (Phát hiện mặt) -> Alignment (Xoay/Căn chỉnh mặt cho thẳng) -> DeepFace Verification (So sánh).
Cơ chế này giúp tăng độ chính xác lên rất nhiều so với việc chỉ so sánh ảnh thô.

Giải thích về việc "Lưu nhân viên mới":
Khi Admin thêm một nhân viên mới, bạn sẽ gọi endpoint /extract-embedding.

AI sẽ trả về một mảng số (ví dụ: 128 số).
Bạn lưu mảng này vào cột face_embedding trong bảng users của MySQL.
Không cần train lại: Model AI đã thông minh sẵn rồi, nó chỉ làm nhiệm vụ "dịch" khuôn mặt thành "mật mã số". Bạn chỉ việc so sánh mật mã này thôi.


tương thích hoàn toàn với MySQL:

Lưu trữ: Chúng ta lưu Vector khuôn mặt vào cột face_embedding (kiểu TEXT) trong MySQL dưới dạng một chuỗi JSON (ví dụ: "[0.1, -0.2, ... ]").
Tính toán: Thay vì bắt MySQL tính (vì MySQL không mạnh về toán vector), chúng ta sẽ đẩy việc tính toán độ giống nhau sang cho AI Service (Python). Python xử lý việc này cực nhanh và chính xác.
Tôi sẽ viết AttendanceServiceImpl.java để thực hiện đúng luồng này: Lấy chuỗi TEXT từ MySQL -> Gửi sang Python xử lý -> Nhận kết quả.



1. Cơ chế lưu trữ trên MySQL
Tôi đã cấu hình cột face_embedding kiểu TEXT trong MySQL.
Cách thức: Khi Java nhận được Vector từ AI (là một mảng số), nó sẽ dùng thư viện Jackson để biến mảng đó thành chuỗi JSON và lưu vào MySQL. Khi cần chấm công, Java sẽ đọc chuỗi này ra và gửi sang cho Python tính toán. Việc này giúp MySQL không cần cài thêm bất kỳ plugin AI nào mà vẫn chạy tốt.
2. Các thành phần đã triển khai
AI Service (Python - cổng 8000): Đã có thêm API verify-with-embedding để so sánh trực tiếp "Ảnh mới" với "Chuỗi Vector từ MySQL".
Spring Boot (Java):
AppConfig.java: Cấu hình RestTemplate để Java có thể "gọi điện" cho Python.
AttendanceServiceImpl.java: Xử lý logic nghiệp vụ (Đăng ký mặt, Chấm công, Kiểm tra kết quả AI).
AttendanceController.java: Cung cấp các API chuẩn REST cho Zalo Mini App gọi vào (/check-in, /register-face).
3. Quy trình Kiểm thử (Test Flow)
Để tính năng này hoạt động, bạn chỉ cần thực hiện 2 bước đơn giản trên Postman (hoặc từ UI Mini App sau này):

Đăng ký mặt (Lấy mẫu):
Gửi một ảnh Selfie của nhân viên vào API: POST /api/v1/attendance/register-face/{userId}.
Hệ thống sẽ tự động lưu "Mật mã khuôn mặt" vào MySQL.
Chấm công thật:
Gửi ảnh Selfie lúc đang đi làm kèm Tọa độ GPS vào API: POST /api/v1/attendance/check-in/{userId}.
Hệ thống sẽ lấy mật mã từ MySQL ra so sánh -> Trả về kết quả Thành công/Thất bại ngay lập tức.
Tại sao đây là điểm cộng lớn cho CV?
Bạn đã áp dụng kiến trúc Hybrid Architecture (Đa ngôn ngữ): Java làm Back-office, Python làm AI Engine.
Bạn đã xử lý được bài toán Inter-service Communication (truyền nhận dữ liệu giữa 2 server khác nhau).
Bạn giải quyết được vấn đề Data Processing (biến đổi dữ liệu AI thành JSON để lưu vào MySQL).



