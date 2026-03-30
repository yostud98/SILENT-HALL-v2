"""
UMAT AI — YOLOv8 Model Loader
==============================
Downloads yolov8n.pt if not present and validates it.
Run this once before starting the proctoring engine.
"""

import sys
from pathlib import Path


def download_yolov8():
    model_path = Path("yolov8n.pt")

    if model_path.exists():
        print(f"[OK] yolov8n.pt already exists ({model_path.stat().st_size // 1024} KB)")
        return True

    print("[INFO] Downloading YOLOv8n model (~6MB)...")
    try:
        from ultralytics import YOLO
        model = YOLO("yolov8n.pt")  # Auto-downloads
        print(f"[OK] YOLOv8n downloaded and ready")

        # Quick validation
        import numpy as np
        dummy = np.zeros((480, 640, 3), dtype=np.uint8)
        results = model(dummy, verbose=False)
        print(f"[OK] Model validated — {len(model.names)} classes detected")
        print(f"     Phone class: {[k for k,v in model.names.items() if 'phone' in v.lower() or 'cell' in v.lower()]}")
        return True

    except ImportError:
        print("[ERROR] ultralytics not installed.")
        print("  Run: pip install ultralytics")
        return False
    except Exception as e:
        print(f"[ERROR] Download failed: {e}")
        return False


def check_dependencies():
    deps = {
        "cv2": "opencv-python-headless",
        "numpy": "numpy",
        "ultralytics": "ultralytics",
        "requests": "requests",
    }
    all_ok = True
    for module, package in deps.items():
        try:
            __import__(module)
            print(f"  [✓] {package}")
        except ImportError:
            print(f"  [✗] {package}  ← pip install {package}")
            all_ok = False
    return all_ok


if __name__ == "__main__":
    print("\n" + "="*50)
    print("  UMAT AI Proctoring — Setup Check")
    print("="*50 + "\n")

    print("Checking Python dependencies:")
    ok = check_dependencies()

    if not ok:
        print("\n[ACTION] Install missing packages:")
        print("  pip install -r requirements.txt\n")
        sys.exit(1)

    print("\nDownloading/verifying YOLOv8n model:")
    download_yolov8()

    print("\n[READY] AI proctoring system is configured.")
    print("  Run with: python proctor.py --attempt-id 1 --student-ref UMAT/CS/21/0042\n")
