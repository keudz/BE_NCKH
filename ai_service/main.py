from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from services.face_service import FaceService
from utils.file_utils import save_temp_file, remove_temp_file
from utils.metrics import MetricsTracker
from pydantic import BaseModel
import traceback

app = FastAPI(title="AI Face Recognition & Liveness Service")

# Đảm bảo các mô hình ONNX đã được tải đầy đủ khi khởi chạy dịch vụ
FaceService.ensure_models_downloaded()

class FeedbackRequest(BaseModel):
    verification_id: str
    is_correct: bool

@app.get("/")
async def health_check():
    return {
        "status": "AI Face Service is running", 
        "features": ["Face Detection (YuNet)", "Face Recognition (SFace)", "Anti-Spoofing (MiniFASNetV2)", "Face Quality Assessment"]
    }

@app.post("/verify")
async def verify_face(image1: UploadFile = File(...), image2: UploadFile = File(...)):
    path1 = save_temp_file(image1)
    path2 = save_temp_file(image2)
    try:
        return FaceService.verify_two_faces(path1, path2)
    except ValueError as val_e:
        raise HTTPException(status_code=400, detail=str(val_e))
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống AI: {str(e)}")
    finally:
        remove_temp_file(path1)
        remove_temp_file(path2)

@app.post("/verify-with-embedding")
async def verify_with_embedding(
    image: UploadFile = File(...),
    stored_embedding_json: str = Form(...),
):
    path = save_temp_file(image)
    try:
        return FaceService.verify_with_stored_embedding(path, stored_embedding_json)
    except ValueError as val_e:
        raise HTTPException(status_code=400, detail=str(val_e))
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống AI: {str(e)}")
    finally:
        remove_temp_file(path)

@app.post("/extract-embeddings")
async def extract_multiple_embeddings(files: list[UploadFile] = File(...)):
    paths = []
    try:
        for file in files:
            path = save_temp_file(file)
            paths.append(path)
        
        embedding = FaceService.extract_multiple_embeddings(paths)
        return {"embedding": embedding}
    except ValueError as val_e:
        raise HTTPException(status_code=400, detail=str(val_e))
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống AI: {str(e)}")
    finally:
        for path in paths:
            remove_temp_file(path)

@app.post("/extract-embedding")
async def extract_embedding(image: UploadFile = File(...)):
    path = save_temp_file(image)
    try:
        embedding, _, _ = FaceService.extract_embedding(path)
        return {"embedding": embedding}
    except ValueError as val_e:
        raise HTTPException(status_code=400, detail=str(val_e))
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống AI: {str(e)}")
    finally:
        remove_temp_file(path)

# --- Endpoints cho báo cáo & giám sát chất lượng (Biometrics Metrics) ---

@app.get("/metrics")
async def get_system_metrics():
    try:
        return MetricsTracker.get_metrics()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi truy xuất metrics hệ thống: {str(e)}")

@app.post("/metrics/feedback")
async def submit_metrics_feedback(req: FeedbackRequest):
    try:
        success = MetricsTracker.submit_feedback(req.verification_id, req.is_correct)
        if not success:
            raise HTTPException(status_code=404, detail="Không tìm thấy mã xác thực tương ứng trong lịch sử ghi log.")
        return {"status": "Phản hồi đã được ghi nhận thành công."}
    except HTTPException as http_e:
        raise http_e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi lưu trữ phản hồi: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
