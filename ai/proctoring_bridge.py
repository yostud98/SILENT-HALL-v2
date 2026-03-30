"""
UMAT AI — Proctoring HTTP Bridge
==================================
Runs a lightweight local HTTP server that:
 1. Receives video frames from the frontend (base64 encoded)
 2. Runs AI analysis (face detection, phone detection)
 3. Returns cheating events as JSON

This lets the browser-based frontend call the Python AI in real-time.

Usage:
    python proctoring_bridge.py --port 5555

Frontend calls:
    POST http://localhost:5555/analyze  {frame: "<base64_image>", attempt_id: 1}
    GET  http://localhost:5555/status
"""

import base64
import json
import time
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO
from datetime import datetime
import argparse

try:
    import cv2
    import numpy as np
    CV2_AVAILABLE = True
except ImportError:
    CV2_AVAILABLE = False
    print("[WARN] OpenCV not installed. Running in stub mode.")


# ── State ─────────────────────────────────────────────────────
state = {
    "cheating_score": 0,
    "face_count": 0,
    "phone_detected": False,
    "looking_away": False,
    "last_analysis": None,
    "events": []
}

# Face detector (lazy-loaded)
face_cascade = None
phone_model = None


def get_face_cascade():
    global face_cascade
    if face_cascade is None and CV2_AVAILABLE:
        path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        face_cascade = cv2.CascadeClassifier(path)
    return face_cascade


def get_phone_model():
    global phone_model
    if phone_model is None:
        try:
            from ultralytics import YOLO
            phone_model = YOLO("yolov8n.pt")
        except Exception:
            pass
    return phone_model


# ── Analysis ──────────────────────────────────────────────────
def analyze_frame(frame_b64: str) -> dict:
    events = []

    if not CV2_AVAILABLE:
        return {"events": [], "face_count": 1, "phone": False, "score": 0}

    # Decode base64 frame
    try:
        img_data = base64.b64decode(frame_b64)
        nparr = np.frombuffer(img_data, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if frame is None:
            return {"error": "Invalid frame", "events": []}
    except Exception as e:
        return {"error": str(e), "events": []}

    result = {
        "face_count": 0,
        "phone_detected": False,
        "looking_away": False,
        "events": [],
        "timestamp": datetime.now().isoformat()
    }

    # Face detection
    cascade = get_face_cascade()
    if cascade:
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = cascade.detectMultiScale(gray, 1.1, 5, minSize=(60, 60))
        result["face_count"] = len(faces)

        if len(faces) == 0:
            state["cheating_score"] = min(100, state["cheating_score"] + 2)
            events.append({"type": "NO_FACE_DETECTED", "severity": 2, "desc": "No face in frame"})

        elif len(faces) > 1:
            state["cheating_score"] = min(100, state["cheating_score"] + 10)
            events.append({"type": "MULTIPLE_FACES", "severity": 10,
                           "desc": f"{len(faces)} faces detected"})

        else:
            # Check if looking away (face off-center)
            h, w = frame.shape[:2]
            x, y, fw, fh = faces[0]
            cx = x + fw // 2
            deviation = abs(cx - w // 2) / (w // 2)
            if deviation > 0.45:
                result["looking_away"] = True
                state["cheating_score"] = min(100, state["cheating_score"] + 1)
                events.append({"type": "LOOKING_AWAY", "severity": 1,
                               "desc": f"Face deviation: {deviation:.0%}"})

    # Phone detection (YOLOv8)
    model = get_phone_model()
    if model:
        try:
            results = model(frame, verbose=False, conf=0.5)
            for r in results:
                for box in r.boxes:
                    label = r.names[int(box.cls[0])].lower()
                    if "phone" in label or "cell" in label:
                        conf = float(box.conf[0])
                        result["phone_detected"] = True
                        state["cheating_score"] = min(100, state["cheating_score"] + 8)
                        events.append({"type": "PHONE_DETECTED", "severity": 8,
                                       "desc": f"Phone detected ({conf:.0%} confidence)"})
        except Exception:
            pass

    result["events"] = events
    result["cheating_score"] = state["cheating_score"]

    # Store recent events
    state["events"] = (state["events"] + events)[-50:]  # Keep last 50
    state["face_count"] = result["face_count"]
    state["phone_detected"] = result["phone_detected"]
    state["looking_away"] = result.get("looking_away", False)
    state["last_analysis"] = result["timestamp"]

    return result


# ── HTTP Handler ──────────────────────────────────────────────
class ProctoringHandler(BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        pass  # Suppress default logs

    def send_json(self, data, status=200):
        body = json.dumps(data).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Content-Length", len(body))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        if self.path == "/status":
            self.send_json({
                "status": "running",
                "cv2_available": CV2_AVAILABLE,
                "cheating_score": state["cheating_score"],
                "face_count": state["face_count"],
                "phone_detected": state["phone_detected"],
                "looking_away": state["looking_away"],
                "last_analysis": state["last_analysis"],
                "recent_events": state["events"][-10:]
            })
        elif self.path == "/reset":
            state["cheating_score"] = 0
            state["events"] = []
            self.send_json({"status": "reset"})
        else:
            self.send_json({"error": "Not found"}, 404)

    def do_POST(self):
        if self.path == "/analyze":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length)
            try:
                data = json.loads(body)
                frame_b64 = data.get("frame", "")
                if not frame_b64:
                    self.send_json({"error": "No frame provided"}, 400)
                    return
                result = analyze_frame(frame_b64)
                self.send_json(result)
            except Exception as e:
                self.send_json({"error": str(e)}, 500)
        else:
            self.send_json({"error": "Not found"}, 404)


def run_bridge(port=5555):
    server = HTTPServer(("127.0.0.1", port), ProctoringHandler)
    print(f"[PROCTOR BRIDGE] Running on http://127.0.0.1:{port}")
    print(f"  POST /analyze  — Send base64 frame for analysis")
    print(f"  GET  /status   — Get current cheating state")
    print(f"  GET  /reset    — Reset cheating score")
    print(f"  OpenCV: {'✓' if CV2_AVAILABLE else '✗ (stub mode)'}")
    print(f"\n  Press Ctrl+C to stop\n")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[PROCTOR BRIDGE] Stopped.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="UMAT Proctoring Bridge Server")
    parser.add_argument("--port", type=int, default=5555, help="Port (default: 5555)")
    args = parser.parse_args()
    run_bridge(args.port)
