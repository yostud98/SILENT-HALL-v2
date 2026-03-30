"""
UMAT AI Quiz System — AI Proctoring Engine
==========================================
Face detection, eye tracking, phone detection, cheating scoring.
Runs as a service that sends cheating events to the backend.

Requirements:
    pip install opencv-python-headless ultralytics requests numpy

Usage:
    python proctor.py --attempt-id 123 --student-ref UMAT/CS/21/0042
"""

import cv2
import numpy as np
import time
import json
import requests
import argparse
import threading
from datetime import datetime
from pathlib import Path


# ── CONFIG ────────────────────────────────────────────────────
API_BASE = "http://localhost:8080/api"
TOKEN = ""  # Set after login
CHEAT_SCORE = 0
ATTEMPT_ID = None

# Cheating event severities
SEVERITY = {
    "NO_FACE_DETECTED":    5,
    "MULTIPLE_FACES":     20,
    "LOOKING_AWAY":        3,
    "PHONE_DETECTED":     15,
    "SCREENSHOT_DETECTED": 25,
}

# Cheating score accumulator
class CheatingTracker:
    def __init__(self):
        self.score = 0
        self.events = []
        self.last_event_time = {}
        self.cooldown = 3  # seconds between same-type events

    def add_event(self, event_type: str, description: str = ""):
        now = time.time()
        last = self.last_event_time.get(event_type, 0)
        if now - last < self.cooldown:
            return  # Cooldown active
        self.last_event_time[event_type] = now
        severity = SEVERITY.get(event_type, 5)
        self.score = min(100, self.score + severity)
        event = {
            "type": event_type,
            "description": description,
            "timestamp": datetime.now().isoformat(),
            "severity": severity,
            "cumulative_score": self.score
        }
        self.events.append(event)
        print(f"[CHEAT EVENT] {event_type}: {description} | Score: {self.score}/100")
        send_cheat_event(event_type, description)
        return event

    def get_json(self):
        return json.dumps({
            "cheating_score": self.score,
            "events": self.events
        }, indent=2)


tracker = CheatingTracker()


# ── BACKEND COMMUNICATION ─────────────────────────────────────
def send_cheat_event(event_type: str, description: str):
    if not ATTEMPT_ID or not TOKEN:
        return
    try:
        requests.post(
            f"{API_BASE}/quiz/cheat-event",
            json={"attemptId": ATTEMPT_ID, "eventType": event_type, "description": description},
            headers={"Authorization": f"Bearer {TOKEN}"},
            timeout=3
        )
    except Exception as e:
        print(f"[API] Failed to send event: {e}")


# ── FACE DETECTION ────────────────────────────────────────────
class FaceDetector:
    def __init__(self):
        # Haar cascade for fast face detection
        cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        self.face_cascade = cv2.CascadeClassifier(cascade_path)

        eye_cascade_path = cv2.data.haarcascades + "haarcascade_eye.xml"
        self.eye_cascade = cv2.CascadeClassifier(eye_cascade_path)

        self.no_face_frames = 0
        self.NO_FACE_THRESHOLD = 15  # ~1.5 seconds at 10fps

    def detect(self, frame):
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = self.face_cascade.detectMultiScale(
            gray, scaleFactor=1.1, minNeighbors=5, minSize=(80, 80)
        )
        result = {
            "face_count": len(faces),
            "faces": [],
            "eyes_detected": False,
            "looking_away": False
        }

        if len(faces) == 0:
            self.no_face_frames += 1
            if self.no_face_frames >= self.NO_FACE_THRESHOLD:
                tracker.add_event("NO_FACE_DETECTED", "No face visible for extended period")
                self.no_face_frames = 0
        else:
            self.no_face_frames = 0

        if len(faces) > 1:
            tracker.add_event("MULTIPLE_FACES", f"{len(faces)} faces detected in frame")

        for (x, y, w, h) in faces:
            result["faces"].append({"x": int(x), "y": int(y), "w": int(w), "h": int(h)})
            # Draw bounding box
            cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
            cv2.putText(frame, "STUDENT", (x, y-5), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

            # Eye detection within face ROI
            roi_gray = gray[y:y+h, x:x+w]
            eyes = self.eye_cascade.detectMultiScale(roi_gray, scaleFactor=1.1, minNeighbors=3)
            if len(eyes) >= 1:
                result["eyes_detected"] = True

            # Gaze estimation: check face position in frame
            frame_h, frame_w = frame.shape[:2]
            face_cx = x + w // 2
            face_cy = y + h // 2
            # If face center is far from frame center, student is looking away
            deviation_x = abs(face_cx - frame_w // 2) / (frame_w // 2)
            deviation_y = abs(face_cy - frame_h // 2) / (frame_h // 2)
            if deviation_x > 0.5 or deviation_y > 0.5:
                result["looking_away"] = True
                tracker.add_event("LOOKING_AWAY", "Face detected but looking away from screen")

        return result, frame


# ── PHONE DETECTION (YOLOv8) ─────────────────────────────────
class PhoneDetector:
    def __init__(self, use_yolo=True):
        self.enabled = False
        self.model = None

        if use_yolo:
            try:
                from ultralytics import YOLO
                # Download yolov8n.pt automatically if not present
                model_path = Path("yolov8n.pt")
                self.model = YOLO(str(model_path))
                self.enabled = True
                print("[PhoneDetector] YOLOv8 loaded successfully")
            except ImportError:
                print("[PhoneDetector] ultralytics not installed. Phone detection disabled.")
                print("  Install with: pip install ultralytics")
            except Exception as e:
                print(f"[PhoneDetector] Failed to load YOLO: {e}")

    def detect(self, frame):
        if not self.enabled or self.model is None:
            return frame, False

        results = self.model(frame, verbose=False, conf=0.45)
        phone_detected = False

        for result in results:
            for box in result.boxes:
                cls_id = int(box.cls[0])
                label = result.names[cls_id]
                conf = float(box.conf[0])

                # COCO class 67 = cell phone
                if label.lower() in ["cell phone", "phone", "mobile phone"] and conf > 0.45:
                    phone_detected = True
                    x1, y1, x2, y2 = map(int, box.xyxy[0])
                    cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 0, 255), 2)
                    cv2.putText(frame, f"PHONE {conf:.0%}", (x1, y1-5),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
                    tracker.add_event("PHONE_DETECTED", f"Phone detected with {conf:.0%} confidence")

        return frame, phone_detected


# ── OVERLAY / HUD ─────────────────────────────────────────────
def draw_hud(frame, face_result, cheat_score):
    h, w = frame.shape[:2]

    # Background panel
    overlay = frame.copy()
    cv2.rectangle(overlay, (0, 0), (280, 110), (15, 20, 40), -1)
    cv2.addWeighted(overlay, 0.8, frame, 0.2, 0, frame)

    # Title
    cv2.putText(frame, "UMAT AI PROCTOR", (10, 20),
                cv2.FONT_HERSHEY_SIMPLEX, 0.55, (100, 200, 255), 1)

    # Face status
    face_count = face_result.get("face_count", 0)
    face_color = (0, 255, 100) if face_count == 1 else ((0, 100, 255) if face_count == 0 else (0, 50, 255))
    face_text = f"Faces: {face_count}" + (" [OK]" if face_count == 1 else " [WARNING]")
    cv2.putText(frame, face_text, (10, 42), cv2.FONT_HERSHEY_SIMPLEX, 0.45, face_color, 1)

    # Gaze status
    gaze_text = "Gaze: AWAY" if face_result.get("looking_away") else "Gaze: OK"
    gaze_color = (0, 100, 255) if face_result.get("looking_away") else (0, 255, 100)
    cv2.putText(frame, gaze_text, (10, 62), cv2.FONT_HERSHEY_SIMPLEX, 0.45, gaze_color, 1)

    # Cheating score bar
    cv2.putText(frame, f"Cheat Score: {cheat_score}/100", (10, 82),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (200, 200, 255), 1)
    bar_color = (0, 200, 0) if cheat_score < 20 else ((0, 165, 255) if cheat_score < 50 else (0, 0, 255))
    cv2.rectangle(frame, (10, 90), (10 + int(cheat_score * 2), 100), bar_color, -1)
    cv2.rectangle(frame, (10, 90), (210, 100), (100, 100, 100), 1)

    # Timestamp
    ts = datetime.now().strftime("%H:%M:%S")
    cv2.putText(frame, ts, (w - 80, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (150, 150, 150), 1)

    return frame


# ── MAIN PROCTOR LOOP ─────────────────────────────────────────
def run_proctor(attempt_id=None, student_ref=None, show_window=True, camera_index=0):
    global ATTEMPT_ID
    ATTEMPT_ID = attempt_id

    print(f"\n{'='*50}")
    print("  UMAT AI Proctoring Engine")
    print(f"  Attempt ID : {attempt_id or 'demo'}")
    print(f"  Student    : {student_ref or 'demo'}")
    print(f"{'='*50}\n")

    cap = cv2.VideoCapture(camera_index)
    if not cap.isOpened():
        print("[ERROR] Cannot open camera")
        return

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    cap.set(cv2.CAP_PROP_FPS, 10)

    face_detector = FaceDetector()
    phone_detector = PhoneDetector(use_yolo=True)

    frame_count = 0
    print("[PROCTOR] Starting monitoring loop. Press Q to stop.")

    while True:
        ret, frame = cap.read()
        if not ret:
            print("[ERROR] Failed to read frame")
            break

        frame_count += 1

        # Run AI checks every 3rd frame (for performance)
        if frame_count % 3 == 0:
            face_result, frame = face_detector.detect(frame)
            frame, phone_found = phone_detector.detect(frame)
            frame = draw_hud(frame, face_result, tracker.score)

        if show_window:
            cv2.imshow("UMAT AI Proctor", frame)
            key = cv2.waitKey(1) & 0xFF
            if key == ord('q') or key == 27:  # Q or ESC
                break

    cap.release()
    if show_window:
        cv2.destroyAllWindows()

    # Save cheating report
    report_path = f"cheat_report_{attempt_id or 'demo'}_{int(time.time())}.json"
    with open(report_path, 'w') as f:
        f.write(tracker.get_json())
    print(f"\n[REPORT] Saved to {report_path}")
    print(f"[FINAL SCORE] Cheating Score: {tracker.score}/100")
    return tracker.score


# ── SNAPSHOT API ──────────────────────────────────────────────
def capture_snapshot(frame, attempt_id, event_type):
    """Save a snapshot when a violation occurs"""
    snapshots_dir = Path("snapshots")
    snapshots_dir.mkdir(exist_ok=True)
    filename = f"{attempt_id}_{event_type}_{int(time.time())}.jpg"
    path = snapshots_dir / filename
    cv2.imwrite(str(path), frame)
    return str(path)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="UMAT AI Proctoring Engine")
    parser.add_argument("--attempt-id", type=int, help="Quiz attempt ID")
    parser.add_argument("--student-ref", type=str, help="Student reference number")
    parser.add_argument("--token", type=str, help="JWT auth token", default="")
    parser.add_argument("--camera", type=int, default=0, help="Camera index (default: 0)")
    parser.add_argument("--headless", action="store_true", help="Run without GUI window")
    args = parser.parse_args()

    TOKEN = args.token
    run_proctor(
        attempt_id=args.attempt_id,
        student_ref=args.student_ref,
        show_window=not args.headless,
        camera_index=args.camera
    )
