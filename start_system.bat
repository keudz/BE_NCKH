@echo off
TITLE NCKH-ZMA Multi-System Runner
echo ========================================================
echo       NCKH-ZMA AI ATTENDANCE SYSTEM STARTUP
echo ========================================================
echo.
echo NOTE: Since your DB is on Aiven Cloud, no XAMPP is needed.
echo Ensure you have a stable internet connection.
echo.

:: 1. Start AI Service (Python)
echo [1/2] Launching AI Face Service (FastAPI)...
:: Kiểm tra và kích hoạt venv nếu có, sau đó mới chạy main.py
start "AI Service" cmd /k "cd ai_service && (if exist venv\Scripts\activate.bat (call venv\Scripts\activate.bat) else (echo Venv not found, using global python)) && python main.py"

:: 2. Start AI Agent Service (Python)
echo [2/3] Launching AI Agent Service (FastAPI - Port 8001)...
start "AI Agent Service" cmd /k "cd ai_agent_service && if exist venv\Scripts\activate.bat (call venv\Scripts\activate.bat) && uvicorn main:app --host 0.0.0.0 --port 8001 --reload"

:: 3. Start Backend (Java Spring Boot)
echo [3/3] Launching Java Backend (Maven)...
start "Java Backend" cmd /k "mvnw spring-boot:run"

echo.
echo --------------------------------------------------------
echo All services are launching in separate windows.
echo - AI Face Service: http://localhost:8000
echo - AI Agent Service: http://localhost:8001
echo - Java Backend: http://localhost:8080
echo.
echo Once the AI windows say 'Uvicorn running', you can run:
echo 'python ai_service/test_cam.py'
echo --------------------------------------------------------
pause
