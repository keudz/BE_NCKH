import cv2
import requests
import json
import os
import threading

# Configuration
AI_URL = "http://localhost:8000"
MODEL_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models", "face_detection_yunet_2023mar.onnx")

# Global variables for background threading
live_verify_mode = False
verify_thread_running = False
live_status_text = ""
live_status_color = (255, 255, 255)

def verify_worker(frame, stored_embedding):
    """Gửi frame lên server ở chế độ nền (background thread) để không làm đơ camera."""
    global verify_thread_running, live_status_text, live_status_color
    try:
        _, img_encoded = cv2.imencode('.jpg', frame)
        files = {'image': ('checkin.jpg', img_encoded.tobytes(), 'image/jpeg')}
        data = {'stored_embedding_json': json.dumps(stored_embedding)}
        
        res = requests.post(f"{AI_URL}/verify-with-embedding", files=files, data=data, timeout=3)
        if res.status_code == 200:
            result = res.json()
            if result['verified']:
                live_status_text = f"MATCHED! ({result['distance']:.2f})"
                live_status_color = (0, 255, 0) # Xanh lá
            else:
                live_status_text = f"NOT MATCHED ({result['distance']:.2f})"
                live_status_color = (0, 0, 255) # Đỏ
        elif res.status_code == 400:
            # Lấy lý do lỗi chi tiết từ server (như Không phát hiện mặt, Ảnh mờ...)
            detail = res.json().get('detail', 'BAD REQUEST')
            if "Không phát hiện" in detail:
                live_status_text = "NO FACE"
            elif "Chất lượng" in detail:
                reasons = detail.split(":")[-1].strip()
                live_status_text = f"QC FAIL: {reasons}"
            else:
                live_status_text = detail
            live_status_color = (0, 0, 255) # Đỏ
        else:
            live_status_text = "API ERROR"
            live_status_color = (0, 0, 255)
    except Exception as e:
        live_status_text = "CONNECTION ERROR"
        live_status_color = (0, 0, 255)
    finally:
        verify_thread_running = False

def test_webcam():
    global live_verify_mode, verify_thread_running, live_status_text, live_status_color

    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("Error: Could not open webcam.")
        return

    ret, frame = cap.read()
    if not ret:
        print("Failed to grab frame")
        return
    h, w, _ = frame.shape

    # Init YuNet Face Detector
    detector = cv2.FaceDetectorYN.create(MODEL_PATH, "", (w, h))
    stored_embedding = None
    
    print("\n" + "="*40)
    print("   AI FACE RECOGNITION TESTER   ")
    print("="*40)
    print("Instructions:")
    print(" [S] - Scan: Extract face embedding (Registration)")
    print(" [V] - Toggle LIVE Verify Mode (Bật/Tắt nhận diện thời gian thực)")
    print(" [Q] - Quit: Close tester")
    print("-" * 40)

    while True:
        ret, frame = cap.read()
        if not ret:
            break
            
        display_frame = frame.copy()
        
        # Face detection for UI tracking
        detector.setInputSize((frame.shape[1], frame.shape[0]))
        _, faces = detector.detect(frame)
        
        # Draw bounding box
        if faces is not None:
            for face in faces:
                x, y, w_box, h_box = map(int, face[0:4])
                
                # Đổi màu ô vuông theo kết quả Live
                box_color = live_status_color if (live_verify_mode and live_status_text) else (0, 255, 0)
                thickness = 2
                l_size = 30
                
                cv2.line(display_frame, (x, y), (x + l_size, y), box_color, thickness)
                cv2.line(display_frame, (x, y), (x, y + l_size), box_color, thickness)
                cv2.line(display_frame, (x + w_box, y), (x + w_box - l_size, y), box_color, thickness)
                cv2.line(display_frame, (x + w_box, y), (x + w_box, y + l_size), box_color, thickness)
                cv2.line(display_frame, (x, y + h_box), (x + l_size, y + h_box), box_color, thickness)
                cv2.line(display_frame, (x, y + h_box), (x, y + h_box - l_size), box_color, thickness)
                cv2.line(display_frame, (x + w_box, y + h_box), (x + w_box - l_size, y + h_box), box_color, thickness)
                cv2.line(display_frame, (x + w_box, y + h_box), (x + w_box, y + h_box - l_size), box_color, thickness)
                
                display_text = live_status_text if live_verify_mode else "Face Detected"
                cv2.putText(display_frame, display_text, (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.6, box_color, 2)

        # --- LIVE VERIFICATION LOGIC ---
        if live_verify_mode and stored_embedding is not None:
            # Nếu luồng trước đã gửi xong thì mới bắn luồng mới (giữ cho camera không bị giật)
            if not verify_thread_running:
                verify_thread_running = True
                threading.Thread(target=verify_worker, args=(frame.copy(), stored_embedding), daemon=True).start()

        # UI Instructions overlay
        mode_text = "LIVE VERIFY: ON" if live_verify_mode else "LIVE VERIFY: OFF"
        cv2.putText(display_frame, mode_text, (20, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255) if live_verify_mode else (255, 255, 255), 2)
        cv2.putText(display_frame, "SCAN(S) | TOGGLE VERIFY(V) | QUIT(Q)", (20, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)
        
        cv2.imshow('AI Face ID Scanner', display_frame)
        
        key = cv2.waitKey(1) & 0xFF

        # --- Send Full Frame for Registration ---
        if key == ord('s'):
            print("\n>>> Scanning for embedding...")
            _, img_encoded = cv2.imencode('.jpg', frame)
            files = {'image': ('scan.jpg', img_encoded.tobytes(), 'image/jpeg')}
            
            try:
                res = requests.post(f"{AI_URL}/extract-embedding", files=files)
                if res.status_code == 200:
                    stored_embedding = res.json()['embedding']
                    print("✅ SUCCESS: Face registered in memory!")
                else:
                    error_msg = res.json().get('detail', 'Unknown error')
                    print(f"❌ ERROR: {error_msg}")
            except Exception as e:
                print(f"❌ CONNECTION ERROR: ({e})")

        # --- Toggle Live Verify Mode ---
        elif key == ord('v'):
            if stored_embedding is None:
                print("\n⚠️ WARNING: Please press 'S' to scan first!")
            else:
                live_verify_mode = not live_verify_mode
                live_status_text = "Verifying..." if live_verify_mode else ""
                live_status_color = (255, 255, 0)
                print(f"\n>>> Live Verification Mode: {'ON' if live_verify_mode else 'OFF'}")

        elif key == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    test_webcam()
