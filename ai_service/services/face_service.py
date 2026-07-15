import cv2
import numpy as np
import json
import os
import requests
import onnxruntime as ort
from utils.metrics import MetricsTracker

class FaceService:
    MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models")
    YUNET_PATH = os.path.join(MODEL_DIR, "face_detection_yunet_2023mar.onnx")
    SFACE_PATH = os.path.join(MODEL_DIR, "face_recognition_sface_2021dec.onnx")
    MINIFAS_PATH = os.path.join(MODEL_DIR, "minifasnet_v2.onnx")

    YUNET_URL = "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx"
    SFACE_URL = "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx"
    MINIFAS_URL = "https://github.com/facenox/face-antispoof-onnx/raw/main/models/best_model_quantized.onnx"

    THRESHOLD = 0.750  

    # Cấu hình ngưỡng chất lượng và liveness (có thể cấu hình qua .env)
    BLUR_THRESHOLD = float(os.getenv("FACE_QUALITY_BLUR_THRESHOLD", "50.0"))
    LOW_LIGHT_THRESHOLD = float(os.getenv("FACE_QUALITY_LOW_LIGHT_THRESHOLD", "50.0"))
    HIGH_LIGHT_THRESHOLD = float(os.getenv("FACE_QUALITY_HIGH_LIGHT_THRESHOLD", "240.0"))
    MIN_FACE_SIZE = int(os.getenv("FACE_QUALITY_MIN_SIZE", "80"))
    CONFIDENCE_THRESHOLD = float(os.getenv("FACE_QUALITY_CONFIDENCE_THRESHOLD", "0.90"))
    MAX_ROLL_ANGLE = float(os.getenv("FACE_QUALITY_MAX_ROLL", "15.0"))
    MAX_YAW_RATIO = float(os.getenv("FACE_QUALITY_MAX_YAW", "2.5"))
    MIN_PITCH_RATIO = float(os.getenv("FACE_QUALITY_MIN_PITCH", "0.4"))
    MAX_PITCH_RATIO = float(os.getenv("FACE_QUALITY_MAX_PITCH", "2.2"))
    LIVENESS_THRESHOLD = float(os.getenv("FACE_LIVENESS_THRESHOLD", "0.0"))  # Disabled for local webcam testing

    _detector = None
    _recognizer = None
    _liveness_session = None

    @classmethod
    def ensure_models_downloaded(cls):
        if not os.path.exists(cls.MODEL_DIR):
            os.makedirs(cls.MODEL_DIR, exist_ok=True)
            
        for path, url, name in [
            (cls.YUNET_PATH, cls.YUNET_URL, "YuNet (Detection)"), 
            (cls.SFACE_PATH, cls.SFACE_URL, "SFace (Recognition)"),
            (cls.MINIFAS_PATH, cls.MINIFAS_URL, "MiniFASNetV2 (Anti-Spoofing)")
        ]:
            if not os.path.exists(path):
                print(f"Downloading model {name}...")
                response = requests.get(url, stream=True)
                if response.status_code == 200:
                    with open(path, "wb") as f:
                        for chunk in response.iter_content(chunk_size=8192):
                            f.write(chunk)
                    print(f"Model {name} downloaded successfully.")
                else:
                    raise Exception(f"Không thể tải model {name} từ {url}. HTTP Status: {response.status_code}")

    @classmethod
    def get_detector(cls):
        if cls._detector is None:
            cls.ensure_models_downloaded()
            cls._detector = cv2.FaceDetectorYN.create(cls.YUNET_PATH, "", (320, 320))
        return cls._detector

    @classmethod
    def get_recognizer(cls):
        if cls._recognizer is None:
            cls.ensure_models_downloaded()
            cls._recognizer = cv2.FaceRecognizerSF.create(cls.SFACE_PATH, "")
        return cls._recognizer

    @classmethod
    def get_liveness_session(cls):
        if cls._liveness_session is None:
            cls.ensure_models_downloaded()
            cls._liveness_session = ort.InferenceSession(cls.MINIFAS_PATH, providers=["CPUExecutionProvider"])
        return cls._liveness_session

    @staticmethod
    def crop_face(image: np.ndarray, bbox: list, scale: float, out_w: int, out_h: int) -> np.ndarray:
        """Cắt và chuẩn hóa vùng mặt với tỉ lệ padding xác định."""
        src_h, src_w = image.shape[:2]
        x, y, box_w, box_h = bbox
        
        # Giới hạn scale không vượt quá biên ảnh
        scale = min((src_h - 1) / box_h, (src_w - 1) / box_w, scale)
        new_w = box_w * scale
        new_h = box_h * scale
        
        center_x = x + box_w / 2
        center_y = y + box_h / 2
        
        x1 = max(0, int(center_x - new_w / 2))
        y1 = max(0, int(center_y - new_h / 2))
        x2 = min(src_w - 1, int(center_x + new_w / 2))
        y2 = min(src_h - 1, int(center_y + new_h / 2))
        
        cropped = image[y1 : y2 + 1, x1 : x2 + 1]
        return cv2.resize(cropped, (out_w, out_h))

    @classmethod
    def assess_face_quality(cls, img: np.ndarray, face: np.ndarray) -> tuple[bool, dict, np.ndarray]:
        """Đánh giá chất lượng ảnh dựa trên độ mờ, ánh sáng, kích thước, che khuất và góc xoay đầu."""
        x, y, w, h = int(face[0]), int(face[1]), int(face[2]), int(face[3])
        score = face[14]
        
        # Khởi tạo chi tiết đánh giá
        details = {
            "blur": False,
            "brightness_low": False,
            "brightness_high": False,
            "size_too_small": False,
            "occluded": False,
            "pose_roll": False,
            "pose_yaw": False,
            "pose_pitch": False
        }
        
        # 1. Kiểm tra kích thước mặt
        if w < cls.MIN_FACE_SIZE or h < cls.MIN_FACE_SIZE:
            details["size_too_small"] = True
            
        # 2. Kiểm tra che khuất (điểm tin cậy của YuNet)
        if score < cls.CONFIDENCE_THRESHOLD:
            details["occluded"] = True
            
        # Trích xuất landmarks để tính góc pose
        x_le, y_le = face[4], face[5]  # Mắt trái
        x_re, y_re = face[6], face[7]  # Mắt phải
        x_n, y_n = face[8], face[9]    # Mũi
        y_lm = face[11]                # Khóe miệng trái Y
        y_rm = face[13]                # Khóe miệng phải Y
        
        # 3. Kiểm tra Roll Angle (Nghiêng đầu)
        roll = abs(np.degrees(np.arctan2(y_re - y_le, x_re - x_le)))
        if roll > cls.MAX_ROLL_ANGLE:
            details["pose_roll"] = True
            
        # 4. Kiểm tra Yaw Ratio (Xoay mặt trái/phải)
        dist_le_n = abs(x_n - x_le)
        dist_re_n = abs(x_re - x_n)
        yaw_ratio = max(dist_le_n, dist_re_n) / max(1e-5, min(dist_le_n, dist_re_n))
        if yaw_ratio > cls.MAX_YAW_RATIO:
            details["pose_yaw"] = True
            
        # 5. Kiểm tra Pitch Ratio (Gật đầu cúi/ngẩng)
        eye_y = (y_le + y_re) / 2.0
        mouth_y = (y_lm + y_rm) / 2.0
        dist_eye_nose = abs(y_n - eye_y)
        dist_nose_mouth = abs(mouth_y - y_n)
        pitch_ratio = dist_eye_nose / max(1e-5, dist_nose_mouth)
        if pitch_ratio < cls.MIN_PITCH_RATIO or pitch_ratio > cls.MAX_PITCH_RATIO:
            details["pose_pitch"] = True
            
        # Cắt ảnh xám khuôn mặt để đo ánh sáng và độ mờ
        src_h, src_w = img.shape[:2]
        x1, y1 = max(0, x), max(0, y)
        x2, y2 = min(src_w - 1, x + w), min(src_h - 1, y + h)
        gray_face = cv2.cvtColor(img[y1:y2+1, x1:x2+1], cv2.COLOR_BGR2GRAY)
        
        # 6. Kiểm tra ánh sáng (brightness)
        mean_bright = np.mean(gray_face)
        if mean_bright < cls.LOW_LIGHT_THRESHOLD:
            details["brightness_low"] = True
        elif mean_bright > cls.HIGH_LIGHT_THRESHOLD:
            details["brightness_high"] = True
            
        # 7. Kiểm tra độ mờ (Blur - phương sai Laplacian)
        lap_var = cv2.Laplacian(gray_face, cv2.CV_64F).var()
        if lap_var < cls.BLUR_THRESHOLD:
            details["blur"] = True
            
        passed = not any(details.values())
        return passed, details, gray_face

    @classmethod
    def check_liveness(cls, img: np.ndarray, face: np.ndarray) -> tuple[bool, float]:
        """Thực hiện kiểm tra chống giả mạo khuôn mặt bằng mô hình MiniFASNetV2."""
        # 1. Cắt ảnh với scale 2.7 tương tự kịch bản huấn luyện MiniFASNet
        bbox = [int(face[0]), int(face[1]), int(face[2]), int(face[3])]
        cropped = cls.crop_face(img, bbox, scale=2.7, out_w=128, out_h=128)
        
        # 2. Chuyển BGR sang RGB
        rgb_crop = cv2.cvtColor(cropped, cv2.COLOR_BGR2RGB)
        
        # 3. Chuẩn hóa về [0, 1] và đổi sang float32
        input_data = rgb_crop.astype(np.float32) / 255.0
        
        # 4. Chuyển HWC sang CHW
        input_data = input_data.transpose((2, 0, 1))
        
        # 5. Thêm chiều batch (NCHW)
        input_tensor = np.expand_dims(input_data, axis=0)
        
        # 6. Chạy inference
        session = cls.get_liveness_session()
        input_name = session.get_inputs()[0].name
        outputs = session.run(None, {input_name: input_tensor})
        
        # 7. Tính toán Softmax để lấy xác suất lớp người thật (lớp index 1)
        logits = outputs[0][0]
        exp_logits = np.exp(logits - np.max(logits))
        probs = exp_logits / exp_logits.sum(axis=0)
        liveness_score = float(probs[1])
        
        passed = liveness_score >= cls.LIVENESS_THRESHOLD
        return passed, liveness_score

    @staticmethod
    def extract_embedding(img_path: str, action: str = "verify") -> list[float]:
        """Trích xuất vector 128 chiều, tích hợp đầy đủ kiểm tra chất lượng và liveness."""
        try:
            img = cv2.imread(img_path)
            if img is None:
                raise ValueError(f"Không thể đọc file ảnh tại {img_path}")
            
            h, w, _ = img.shape
            detector = FaceService.get_detector()
            detector.setInputSize((w, h))
            
            retval, faces = detector.detect(img)
            if not retval or faces is None or len(faces) == 0:
                # Log sự kiện nhận diện thất bại
                MetricsTracker.log_event(
                    action=action,
                    face_detected=False,
                    quality_passed=False
                )
                raise ValueError("Không phát hiện thấy khuôn mặt trong ảnh.")
            
            face = faces[0]
            
            # 1. Đánh giá chất lượng ảnh
            quality_passed, q_details, _ = FaceService.assess_face_quality(img, face)
            if not quality_passed:
                # Tìm lý do đầu tiên không đạt để thông báo
                failed_reasons = [k for k, v in q_details.items() if v]
                reason_map = {
                    "blur": "Ảnh bị mờ",
                    "brightness_low": "Ảnh quá tối",
                    "brightness_high": "Ảnh quá sáng",
                    "size_too_small": "Mặt quá nhỏ",
                    "occluded": "Khuôn mặt bị che khuất",
                    "pose_roll": "Đầu bị nghiêng quá nhiều",
                    "pose_yaw": "Quay mặt sang bên quá nhiều",
                    "pose_pitch": "Đầu ngẩng hoặc cúi quá nhiều"
                }
                reason_str = ", ".join([reason_map[r] for r in failed_reasons])
                
                # Log sự kiện thất bại chất lượng
                MetricsTracker.log_event(
                    action=action,
                    face_detected=True,
                    quality_passed=False,
                    quality_details=q_details
                )
                raise ValueError(f"Chất lượng ảnh không đạt: {reason_str}")
            
            # 2. Kiểm tra Liveness (Anti-Spoofing)
            liveness_passed, liveness_score = FaceService.check_liveness(img, face)
            if not liveness_passed:
                # Log sự kiện thất bại liveness (spoofing)
                MetricsTracker.log_event(
                    action=action,
                    face_detected=True,
                    quality_passed=True,
                    quality_details=q_details,
                    liveness_passed=False,
                    liveness_score=liveness_score
                )
                raise ValueError(f"Phát hiện giả mạo khuôn mặt (liveness score: {liveness_score:.2f})")
            
            # 3. Trích xuất embedding
            recognizer = FaceService.get_recognizer()
            aligned_face = recognizer.alignCrop(img, face)
            feature = recognizer.feature(aligned_face)
            
            return feature[0].tolist(), q_details, liveness_score
        except Exception as e:
            if "Chất lượng ảnh không đạt" in str(e) or "Phát hiện giả mạo khuôn mặt" in str(e) or "Không phát hiện thấy khuôn mặt" in str(e):
                raise e
            raise Exception(f"Lỗi trích xuất đặc trưng khuôn mặt: {str(e)}")

    @staticmethod
    def verify_two_faces(img1_path: str, img2_path: str):
        try:
            emb1, _, _ = FaceService.extract_embedding(img1_path, action="verify")
            emb2, q_details, liveness_score = FaceService.extract_embedding(img2_path, action="verify")
            
            a = np.array(emb1)
            b = np.array(emb2)
            
            cos_sim = np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))
            cos_distance = 1.0 - cos_sim
            is_verified = cos_distance < FaceService.THRESHOLD
            
            # Log sự kiện verify thành công
            event_id = MetricsTracker.log_event(
                action="verify",
                face_detected=True,
                quality_passed=True,
                quality_details=q_details,
                liveness_passed=True,
                liveness_score=liveness_score,
                verified=bool(is_verified),
                best_distance=float(cos_distance)
            )

            return {
                "verified": bool(is_verified),
                "distance": float(cos_distance),
                "threshold": FaceService.THRESHOLD,
                "model": "YuNet-SFace-Liveness",
                "verification_id": event_id
            }
        except Exception as e:
            raise e

    @staticmethod
    def extract_multiple_embeddings(img_paths: list):
        """Xử lý 5 ảnh đăng ký các góc độ mặt, kiểm tra chất lượng/liveness từng ảnh và ghép lại thành vector 640 chiều."""
        embeddings = []
        
        for i, path in enumerate(img_paths):
            try:
                emb, q_details, liveness_score = FaceService.extract_embedding(path, action="register")
                embeddings.append(emb)
                
                # Log sự kiện đăng ký thành công cho từng ảnh
                MetricsTracker.log_event(
                    action="register",
                    face_detected=True,
                    quality_passed=True,
                    quality_details=q_details,
                    liveness_passed=True,
                    liveness_score=liveness_score
                )
            except ValueError as val_e:
                raise ValueError(f"Ảnh thứ {i+1} không đạt yêu cầu: {str(val_e)}")
            except Exception as e:
                raise Exception(f"Lỗi xử lý ảnh đăng ký thứ {i+1}: {str(e)}")
        
        # Ghép nối (concatenate) toàn bộ 5 vector 128 chiều thành 1 vector phẳng 640 chiều
        # Java Backend sẽ nhận được vector 640 chiều này và lưu vào DB MySQL bình thường
        flat_embeddings = []
        for emb in embeddings:
            flat_embeddings.extend(emb)
            
        return flat_embeddings

    @staticmethod
    def verify_with_stored_embedding(new_img_path: str, stored_embedding_json: str):
        """So khớp ảnh chấm công mới với tập hợp đa-embedding đã lưu, trả về kết quả khớp tốt nhất."""
        try:
            stored_embedding = json.loads(stored_embedding_json)
            
            # Kiểm tra kích thước vector đã lưu để tách biệt các embedding
            total_elements = len(stored_embedding)
            if total_elements % 128 != 0:
                raise ValueError(f"Kích thước vector không hợp lệ ({total_elements}). Cần là bội số của 128.")
                
            num_stored = total_elements // 128
            stored_vectors = [stored_embedding[i*128 : (i+1)*128] for i in range(num_stored)]
            
            # Trích xuất và kiểm tra ảnh điểm danh mới
            new_embedding, q_details, liveness_score = FaceService.extract_embedding(new_img_path, action="verify")
            
            a = np.array(new_embedding)
            
            # So sánh với toàn bộ các embedding đã lưu và chọn khoảng cách nhỏ nhất (tốt nhất)
            best_distance = 999.0
            for stored_vec in stored_vectors:
                b = np.array(stored_vec)
                cos_sim = np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))
                cos_distance = 1.0 - cos_sim
                if cos_distance < best_distance:
                    best_distance = cos_distance
            
            is_verified = best_distance < FaceService.THRESHOLD
            
            # Log sự kiện xác thực thành công vào metrics
            event_id = MetricsTracker.log_event(
                action="verify",
                face_detected=True,
                quality_passed=True,
                quality_details=q_details,
                liveness_passed=True,
                liveness_score=liveness_score,
                verified=bool(is_verified),
                best_distance=float(best_distance)
            )

            return {
                "verified": bool(is_verified),
                "distance": float(best_distance),
                "threshold": FaceService.THRESHOLD,
                "verification_id": event_id
            }
        except json.JSONDecodeError:
            raise ValueError("Dữ liệu embedding lưu trong DB không đúng định dạng JSON.")
        except Exception as e:
            raise e
