import os
import shutil
from fastapi import UploadFile

TEMP_DIR = "temp_images"

def init_temp_dir():
    if not os.path.exists(TEMP_DIR):
        os.makedirs(TEMP_DIR)

def save_temp_file(upload_file: UploadFile) -> str:
    init_temp_dir()
    file_path = os.path.join(TEMP_DIR, upload_file.filename)
    with open(file_path, "wb") as f:
        shutil.copyfileobj(upload_file.file, f)
    return file_path

def remove_temp_file(file_path: str):
    if file_path and os.path.exists(file_path):
        os.remove(file_path)
