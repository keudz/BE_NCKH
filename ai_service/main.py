from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from deepface import DeepFace
import cv2
import numpy as np
import shutil
import os
from typing import List

app = FastAPI(title="AI Face Recognition Service")

TEMP_DIR = "temp_images"
if not os.path.exists(TEMP_DIR):
    os.makedirs(TEMP_DIR)


def preprocess_face(image_path: str):
    """
    Sử dụng OpenCV để căn chỉnh và chuẩn hóa ảnh khuôn mặt.
    Giúp tăng độ chính xác đáng kể cho AI.
    """
    try:
        # Load ảnh
        img = cv2.imread(image_path)
        # DeepFace có tích hợp sẵn detector_backend mạnh mẽ ( 'opencv', 'retinaface', 'mtcnn', 'yolov8')
        # opencv
        objs = DeepFace.extract_faces(
            img_path=img, 
            detector_backend='opencv', 
            enforce_detection=True,
            align=True # Tự động xoay mặt cho thẳng
        )
        return objs[0]["face"] # Trả về mảng numpy của khuôn mặt đã cắt
    except Exception as e:
        print(f"Lỗi tiền xử lý: {e}")
        return None



@app.get("/")
async def health_check():
    return {"status": "AI Service is running", "message": "Face Alignment Pipeline ready!"}



@app.post("/verify")
async def verify_face(
    image1: UploadFile = File(...),
    image2: UploadFile = File(...),
):
    """
    So sánh 2 khuôn mặt với Pipeline chuẩn: Detect -> Align -> Verify.
    """
    path1 = os.path.join(TEMP_DIR, image1.filename)
    path2 = os.path.join(TEMP_DIR, image2.filename)

    with open(path1, "wb") as f:
        shutil.copyfileobj(image1.file, f)
    with open(path2, "wb") as f:
        shutil.copyfileobj(image2.file, f)

    try:
        # verify() của DeepFace đã bao gồm cả bước detect và align nếu chúng ta cấu hình
        result = DeepFace.verify(
            img1_path=path1, 
            img2_path=path2, 
            detector_backend='opencv', # Dùng OpenCV phát diện mặt
            model_name="VGG-Face", 
            enforce_detection=True,
            distance_metric="cosine" # Dùng Cosine distance cho độ chính xác cao nhất
        )
        return {
            "verified": result["verified"],
            "distance": result["distance"],
            "threshold": result["threshold"],
            "model": result["model"]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if os.path.exists(path1): os.remove(path1)
        if os.path.exists(path2): os.remove(path2)


@app.post("/verify-with-embedding")
async def verify_with_embedding(
    image: UploadFile = File(...),
    stored_embedding_json: str = Form(...), # Nhận JSON string từ Java
):
    """
    So sánh 1 ảnh chụp với 1 Vector đã lưu trong Database (MySQL).
    """
    import json
    try:
        stored_embedding = json.loads(stored_embedding_json)
    except Exception as e:
        print(f"❌ Lỗi format JSON embedding gửi từ Java: {e}")
        raise HTTPException(status_code=400, detail="Dữ liệu embedding trong Database không hợp lệ.")

    path = os.path.join(TEMP_DIR, image.filename)
    with open(path, "wb") as f:
        shutil.copyfileobj(image.file, f)

    try:
        # 1. Trích xuất vector từ ảnh mới
        new_results = DeepFace.represent(img_path=path, model_name="VGG-Face", detector_backend='opencv')
        if not new_results:
            raise ValueError("Không tìm thấy khuôn mặt trong ảnh.")
            
        new_embedding = new_results[0]["embedding"]

        # 2. Kiểm tra kích thước vector (VGG-Face: 2622, Facenet: 128, etc.)
        len_new = len(new_embedding)
        len_stored = len(stored_embedding)
        
        if len_new != len_stored:
            error_msg = f"Kích thước vector không khớp! Mới: {len_new}, Cũ (trong DB): {len_stored}. Có thể bạn đã đổi model AI. Vui lòng đăng ký lại khuôn mặt."
            print(f"❌ {error_msg}")
            raise ValueError(error_msg)

        # 3. Tính toán khoảng cách Cosine bằng Numpy (ổn định hơn scipy.distance khi có lỗi data)
        a = np.array(new_embedding)
        b = np.array(stored_embedding)
        # Cosine distance = 1 - Cosine Similarity
        cos_sim = np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))
        cos_distance = 1 - cos_sim
        
        is_verified = cos_distance < 0.35 # Ngưỡng chuẩn cho VGG-Face + Cosine
        
        print(f"✅ Verify Result: {is_verified} (Distance: {cos_distance:.4f})")
 
        return {
            "verified": bool(is_verified),
            "distance": float(cos_distance),
            "threshold": 0.35
        }
    except ValueError as val_e:
        print(f"⚠️ Lỗi nhận diện/xử lý: {val_e}")
        raise HTTPException(status_code=400, detail=str(val_e))
    except Exception as e:
        print(f"🔥 Lỗi hệ thống AI nghiêm trọng: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý AI: {str(e)}")
    finally:
        if os.path.exists(path): os.remove(path)



@app.post("/extract-embedding")
async def extract_embedding(image: UploadFile = File(...)):
    """
    Trích xuất vector đặc trưng (128 hoặc 512 số) từ khuôn mặt.
    Sẽ dùng để lưu vào Database của Spring Boot (Lần đầu lấy mẫu).
    """
    path = os.path.join(TEMP_DIR, image.filename)
    with open(path, "wb") as f:
        shutil.copyfileobj(image.file, f)

    try:
        # Trích xuất vector đặc trưng
        embeddings = DeepFace.represent(img_path=path, model_name="VGG-Face")
        return {"embedding": embeddings[0]["embedding"]}
    except ValueError as val_e:
        print(f"Lỗi nhận diện khuôn mặt: {val_e}")
        raise HTTPException(status_code=400, detail="Không tìm thấy khuôn mặt trong ảnh. Vui lòng chụp rõ mặt.")
    except Exception as e:
        print(f"Lỗi hệ thống AI: {e}")
        raise HTTPException(status_code=500, detail=f"Lỗi trích xuất đặc trưng: {str(e)}")
    finally:
        if os.path.exists(path): os.remove(path)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
