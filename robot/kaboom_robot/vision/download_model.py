"""Downloads the Face Landmarker model file into robot/models/.

Run from the robot folder: python -m kaboom_robot.vision.download_model
"""

import urllib.request
from pathlib import Path

MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/"
    "face_landmarker/face_landmarker/float16/1/face_landmarker.task"
)
MODEL_PATH = Path("models/face_landmarker.task")


def main() -> None:
    if MODEL_PATH.is_file():
        print(f"Already downloaded: {MODEL_PATH}")
        return
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading {MODEL_URL}")
    urllib.request.urlretrieve(MODEL_URL, MODEL_PATH)
    print(f"Saved {MODEL_PATH} ({MODEL_PATH.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
