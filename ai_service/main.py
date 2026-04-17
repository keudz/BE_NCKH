from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from services.face_service import FaceService
from utils.file_utils import save_temp_file, remove_temp_file
import traceback

app = FastAPI(title="AI Face Recognition Service - Refactored")

@app.get("/")
async def health_check():
    return {"status": "AI Service is running", "architecture": "Service Oriented"}

@app.post("/verify")
async def verify_face(image1: UploadFile = File(...), image2: UploadFile = File(...)):
    path1 = save_temp_file(image1)
    path2 = save_temp_file(image2)
    try:
        return FaceService.verify_two_faces(path1, path2)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
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
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    finally:
        for path in paths:
            remove_temp_file(path)

@app.post("/extract-embedding")
async def extract_embedding(image: UploadFile = File(...)):
    path = save_temp_file(image)
    try:
        embedding = FaceService.extract_embedding(path)
        return {"embedding": embedding}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
    finally:
        remove_temp_file(path)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
