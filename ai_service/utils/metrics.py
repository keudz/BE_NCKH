import os
import json
import uuid
from datetime import datetime

class MetricsTracker:
    # Đường dẫn file log sự kiện xác thực
    LOG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data")
    LOG_PATH = os.path.join(LOG_DIR, "verification_logs.jsonl")

    @classmethod
    def ensure_log_file_exists(cls):
        if not os.path.exists(cls.LOG_DIR):
            os.makedirs(cls.LOG_DIR, exist_ok=True)
        if not os.path.exists(cls.LOG_PATH):
            # Tạo file trống
            with open(cls.LOG_PATH, "w", encoding="utf-8") as f:
                pass

    @classmethod
    def log_event(cls, action: str, face_detected: bool, quality_passed: bool, 
                  quality_details: dict = None, liveness_passed: bool = None, 
                  liveness_score: float = None, verified: bool = None, 
                  best_distance: float = None) -> str:
        """Ghi log sự kiện xác thực sinh trắc học và chất lượng ảnh vào file JSONL."""
        cls.ensure_log_file_exists()
        
        event_id = str(uuid.uuid4())
        log_entry = {
            "id": event_id,
            "timestamp": datetime.now().isoformat(),
            "action": action, # "register" hoặc "verify"
            "face_detected": face_detected,
            "quality_passed": quality_passed,
            "quality_details": quality_details,
            "liveness_passed": liveness_passed,
            "liveness_score": liveness_score,
            "verified": verified,
            "best_distance": best_distance,
            "feedback": None # Sẽ cập nhật sau qua API feedback (True/False)
        }
        
        try:
            with open(cls.LOG_PATH, "a", encoding="utf-8") as f:
                f.write(json.dumps(log_entry, ensure_ascii=False) + "\n")
        except Exception as e:
            print(f"Error logging event to {cls.LOG_PATH}: {str(e)}")
            
        return event_id

    @classmethod
    def submit_feedback(cls, verification_id: str, is_correct: bool) -> bool:
        """Cập nhật phản hồi kết quả xác thực thực tế từ phía Client."""
        cls.ensure_log_file_exists()
        if not os.path.exists(cls.LOG_PATH):
            return False

        updated = False
        temp_entries = []
        
        try:
            with open(cls.LOG_PATH, "r", encoding="utf-8") as f:
                for line in f:
                    if not line.strip():
                        continue
                    entry = json.loads(line)
                    if entry.get("id") == verification_id:
                        entry["feedback"] = is_correct
                        updated = True
                    temp_entries.append(entry)
            
            if updated:
                with open(cls.LOG_PATH, "w", encoding="utf-8") as f:
                    for entry in temp_entries:
                        f.write(json.dumps(entry, ensure_ascii=False) + "\n")
        except Exception as e:
            print(f"Error updating feedback in {cls.LOG_PATH}: {str(e)}")
            return False
            
        return updated

    @classmethod
    def get_metrics(cls) -> dict:
        """Đọc toàn bộ file log và tổng hợp các chỉ số chất lượng sinh trắc học."""
        cls.ensure_log_file_exists()
        
        total_verifications = 0
        total_registrations = 0
        spoof_detected = 0
        quality_failed = 0
        
        # Thống kê lý do không đạt chất lượng
        quality_reasons = {
            "blur": 0,
            "brightness_low": 0,
            "brightness_high": 0,
            "size_too_small": 0,
            "occluded": 0,
            "pose_roll": 0,
            "pose_yaw": 0,
            "pose_pitch": 0
        }
        
        # Biometric error rate tracking (FAR/FRR)
        genuine_trials = 0
        impostor_trials = 0
        false_accepts = 0
        false_rejects = 0
        
        # Thống kê theo ngày
        daily_stats = {}

        try:
            with open(cls.LOG_PATH, "r", encoding="utf-8") as f:
                for line in f:
                    if not line.strip():
                        continue
                    entry = json.loads(line)
                    action = entry.get("action")
                    date_str = entry.get("timestamp", "")[:10] # Lấy YYYY-MM-DD
                    
                    if date_str not in daily_stats:
                        daily_stats[date_str] = {
                            "verifications": 0,
                            "registrations": 0,
                            "spoofs": 0,
                            "quality_fails": 0,
                            "verified": 0
                        }
                    
                    if action == "register":
                        total_registrations += 1
                        daily_stats[date_str]["registrations"] += 1
                    elif action == "verify":
                        total_verifications += 1
                        daily_stats[date_str]["verifications"] += 1
                        if entry.get("verified") is True:
                            daily_stats[date_str]["verified"] += 1

                    # Đếm spoofing
                    if entry.get("face_detected") and entry.get("liveness_passed") is False:
                        spoof_detected += 1
                        daily_stats[date_str]["spoofs"] += 1

                    # Đếm lỗi chất lượng ảnh
                    if entry.get("face_detected") and not entry.get("quality_passed"):
                        quality_failed += 1
                        daily_stats[date_str]["quality_fails"] += 1
                        details = entry.get("quality_details") or {}
                        for key, failed in details.items():
                            if failed and key in quality_reasons:
                                quality_reasons[key] += 1

                    # Đếm FAR/FRR nếu có feedback
                    feedback = entry.get("feedback")
                    if action == "verify" and feedback is not None:
                        verified = entry.get("verified")
                        # feedback=True nghĩa là mô hình trả về đúng (verified đúng hoặc rejected đúng)
                        # feedback=False nghĩa là mô hình trả về sai (chấp nhận sai FAR hoặc từ chối sai FRR)
                        if verified is True:
                            if feedback is True:
                                genuine_trials += 1 # True Accept
                            else:
                                impostor_trials += 1 # False Accept (người ngoài nhưng nhận diện nhầm thành nhân viên)
                                false_accepts += 1
                        else: # verified is False
                            if feedback is True:
                                impostor_trials += 1 # True Reject (từ chối đúng người giả mạo/người ngoài)
                            else:
                                genuine_trials += 1 # False Reject (nhân viên thật nhưng bị từ chối)
                                false_rejects += 1
        except Exception as e:
            print(f"Error reading metrics from {cls.LOG_PATH}: {str(e)}")

        # Tính toán tỷ lệ
        spoof_rate = spoof_detected / max(1, total_verifications)
        quality_fail_rate = quality_failed / max(1, total_verifications + total_registrations)
        
        far = false_accepts / max(1, impostor_trials)
        frr = false_rejects / max(1, genuine_trials)

        return {
            "summary": {
                "total_verifications": total_verifications,
                "total_registrations": total_registrations,
                "spoof_attempts_detected": spoof_detected,
                "spoof_rate": round(spoof_rate, 4),
                "quality_failures": quality_failed,
                "quality_fail_rate": round(quality_fail_rate, 4)
            },
            "quality_reasons_breakdown": quality_reasons,
            "biometrics": {
                "feedback_count": genuine_trials + impostor_trials,
                "genuine_trials": genuine_trials,
                "impostor_trials": impostor_trials,
                "false_accepts_count": false_accepts,
                "false_rejects_count": false_rejects,
                "far": round(far, 4), # Tỷ lệ chấp nhận sai
                "frr": round(frr, 4)  # Tỷ lệ từ chối sai
            },
            "daily_statistics": daily_stats
        }
