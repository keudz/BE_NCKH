import cv2
import requests
import json
import os

# Configuration
AI_URL = "http://localhost:8000"

def test_webcam():
    # Attempt to open the default camera
    cap = cv2.VideoCapture(0)
    
    if not cap.isOpened():
        print("Error: Could not open webcam.")
        return

    stored_embedding = None
    
    print("\n" + "="*40)
    print("   AI FACE RECOGNITION TESTER (LOCAL)   ")
    print("="*40)
    print("Instructions:")
    print(" [S] - Scan: Extract face embedding (Registration)")
    print(" [V] - Verify: Check current face against last scan")
    print(" [Q] - Quit: Close tester")
    print("-" * 40)

    while True:
        ret, frame = cap.read()
        if not ret:
            print("Failed to grab frame")
            break
        
        # --- UI ---
        h, w, _ = frame.shape
        box_size = 320
        x1 = (w - box_size) // 2
        y1 = (h - box_size) // 2
        x2 = x1 + box_size
        y2 = y1 + box_size

        display_frame = frame.copy()
        overlay = display_frame.copy()
        cv2.rectangle(overlay, (0, 0), (w, h), (0, 0, 0), -1) # Black background
        
        # Blend overlay (dimmed effect)
        alpha = 0.7 
        cv2.addWeighted(overlay, alpha, display_frame, 1 - alpha, 0, display_frame)
        
        # Restore the clear box in the middle
        display_frame[y1:y2, x1:x2] = frame[y1:y2, x1:x2]
        
        # Draw framing guide lines (Modern corner look)
        color = (0, 255, 0) # Green
        thickness = 2
        l_size = 40 # Corner line length
        
        # Top-Left
        cv2.line(display_frame, (x1, y1), (x1 + l_size, y1), color, thickness + 2)
        cv2.line(display_frame, (x1, y1), (x1, y1 + l_size), color, thickness + 2)
        # Top-Right
        cv2.line(display_frame, (x2, y1), (x2 - l_size, y1), color, thickness + 2)
        cv2.line(display_frame, (x2, y1), (x2, y1 + l_size), color, thickness + 2)
        # Bottom-Left
        cv2.line(display_frame, (x1, y2), (x1 + l_size, y2), color, thickness + 2)
        cv2.line(display_frame, (x1, y2), (x1, y2 - l_size), color, thickness + 2)
        # Bottom-Right
        cv2.line(display_frame, (x2, y2), (x2 - l_size, y2), color, thickness + 2)
        cv2.line(display_frame, (x2, y2), (x2, y2 - l_size), color, thickness + 2)

        # Display the video feed
        cv2.putText(display_frame, "SCAN (S) | VERIFY (V) | QUIT (Q)", (w // 2 - 150, y2 + 40), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)
        cv2.imshow('AI Face ID Scanner', display_frame)
        
        key = cv2.waitKey(1) & 0xFF

        # --- PREPARE IMAGE (CROP TO BOX) ---
        face_roi = frame[y1:y2, x1:x2]

        # --- STEP 1: SCAN/REGISTER ---
        if key == ord('s'):
            print("\n>>> Scanning for embedding (Cropped ROI)...")
            _, img_encoded = cv2.imencode('.jpg', face_roi)
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

        # --- STEP 2: VERIFY/CHECK-IN ---
        elif key == ord('v'):
            if stored_embedding is None:
                print("\n⚠️ WARNING: Please press 'S' to scan first!")
                continue
                
            print("\n>>> Verifying face (Cropped ROI)...")
            _, img_encoded = cv2.imencode('.jpg', face_roi)
            files = {'image': ('checkin.jpg', img_encoded.tobytes(), 'image/jpeg')}
            data = {'stored_embedding_json': json.dumps(stored_embedding)}
            
            try:
                res = requests.post(f"{AI_URL}/verify-with-embedding", files=files, data=data)
                if res.status_code == 200:
                    result = res.json()
                    status = "MATCHED! ✅" if result['verified'] else "NOT MATCHED! ❌"
                    print(f"Result: {status} (Distance: {result['distance']:.4f})")
                else:
                    error_msg = res.json().get('detail', 'Unknown error')
                    print(f"❌ ERROR: {error_msg}")
            except Exception as e:
                print(f"❌ CONNECTION ERROR: ({e})")

        elif key == ord('q'):
            print("\nClosing tester...")
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    test_webcam()
