from deepface import DeepFace
import numpy as np
import json
from fastapi import HTTPException

class FaceService:
    MODEL_NAME = "VGG-Face"
    DETECTOR_BACKEND = "opencv"
    DISTANCE_METRIC = "cosine"
    THRESHOLD = 0.35

    @staticmethod
    def verify_two_faces(img1_path: str, img2_path: str):
        try:
            result = DeepFace.verify(
                img1_path=img1_path,
                img2_path=img2_path,
                detector_backend=FaceService.DETECTOR_BACKEND,
                model_name=FaceService.MODEL_NAME,
                enforce_detection=True,
                distance_metric=FaceService.DISTANCE_METRIC
            )
            return {
                "verified": result["verified"],
                "distance": result["distance"],
                "threshold": result["threshold"],
                "model": result["model"]
            }
        except Exception as e:
            raise Exception(f"Lỗi so sánh khuôn mặt: {str(e)}")

    @staticmethod
    def extract_embedding(img_path: str):
        try:
            embeddings = DeepFace.represent(
                img_path=img_path, 
                model_name=FaceService.MODEL_NAME,
                detector_backend=FaceService.DETECTOR_BACKEND
            )
            return embeddings[0]["embedding"]
        except Exception as e:
            raise Exception(f"Lỗi trích xuất đặc trưng tại {img_path}: {str(e)}")

    WEIGHTS = [0.4, 0.15, 0.15, 0.15, 0.15]  # Thẳng, Trái, Phải, Trên, Dưới

    @staticmethod
    def extract_multiple_embeddings(img_paths: list):
        embeddings = []
        for path in img_paths:
            try:
                embedding = FaceService.extract_embedding(path)
                embeddings.append(embedding)
            except Exception as e:
                # Nếu một hướng bị lỗi, chúng ta không thể tính trọng số chuẩn.
                # Tuy nhiên để linh hoạt, ta sẽ ném lỗi yêu cầu chụp lại.
                raise Exception(f"Không thể trích xuất khuôn mặt từ một trong các hướng ({path}). Vui lòng chụp lại rõ ràng.")
        
        if len(embeddings) != 5:
            # Nếu số lượng không phải 5, ta tính trung bình cộng bình thường (fallback)
            avg_embedding = np.mean(embeddings, axis=0).tolist()
        else:
            # Tính trung bình có trọng số theo 5 hướng
            weighted_embeddings = []
            for i in range(5):
                weighted_embeddings.append(np.array(embeddings[i]) * FaceService.WEIGHTS[i])
            
            avg_embedding = np.sum(weighted_embeddings, axis=0).tolist()
            
        return avg_embedding

    @staticmethod
    def verify_with_stored_embedding(new_img_path: str, stored_embedding_json: str):
        try:
            # 1. Parse embedding từ DB
            stored_embedding = json.loads(stored_embedding_json)
            
            # 2. Trích xuất embedding từ ảnh mới
            new_embedding = FaceService.extract_embedding(new_img_path)

            # 3. Tính toán khoảng cách
            a = np.array(new_embedding)
            b = np.array(stored_embedding)
            
            if len(a) != len(b):
                raise ValueError(f"Kích thước vector không khớp ({len(a)} vs {len(b)}).")

            cos_sim = np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))
            cos_distance = 1 - cos_sim
            is_verified = cos_distance < FaceService.THRESHOLD

            return {
                "verified": bool(is_verified),
                "distance": float(cos_distance),
                "threshold": FaceService.THRESHOLD
            }
        except json.JSONDecodeError:
            raise ValueError("Dữ liệu embedding trong Database không hợp lệ (JSON Error).")
        except Exception as e:
            raise e
