# Kaboom robot

Python code that runs on the Raspberry Pi inside the robot. It watches the user with the
camera and decides whether they are **FOCUSED**, **DISTRACTED**, or **AWAY**.

## Folder layout

```text
robot/
├── config.example.yaml        settings; copy to config.yaml to change them
├── requirements.txt
├── pyproject.toml             ruff and pytest settings
├── kaboom_robot/
│   ├── config.py              loads config.yaml
│   ├── main.py                robot program (later: pairing, sessions, display)
│   ├── buffer.py              offline buffer (later)
│   ├── api/                   backend calls (later)
│   ├── display/               robot faces (later)
│   └── vision/
│       ├── camera.py          laptop / USB webcam or Pi Camera Module
│       ├── face_tracker.py    MediaPipe Face Landmarker, one frame in, numbers out
│       ├── metrics.py         eye openness (EAR), gaze, head angles
│       ├── observation.py     FaceObservation: one frame's numbers
│       ├── calibration.py     learns the user's normal working pose
│       ├── state_machine.py   FOCUSED / DISTRACTED / AWAY over time
│       ├── download_model.py  downloads the model file
│       └── demo.py            live test window
└── tests/                     pytest, no camera needed
```

## Run it on a laptop (Mac)

```bash
cd robot
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python -m kaboom_robot.vision.download_model
python -m kaboom_robot.vision.demo
```

The first time, macOS asks to let the Terminal use the camera. Allow it, then run the demo again.

## Run it on the Raspberry Pi

Needs **64-bit** Raspberry Pi OS (`uname -m` prints `aarch64`).

```bash
sudo apt update
sudo apt install -y python3-venv python3-picamera2 libgl1
cd robot
python3 -m venv --system-site-packages .venv   # lets the venv see picamera2 from apt
source .venv/bin/activate
pip install -r requirements.txt
python -m kaboom_robot.vision.download_model
cp config.example.yaml config.yaml             # then set camera.source to picamera2 for the ribbon camera
python -m kaboom_robot.vision.demo --no-window # over SSH; drop --no-window with a screen attached
```

## Checks

```bash
ruff check .
ruff format --check .
pytest
```

## Credits

- Detection ideas (eye aspect ratio, gaze score, time-based thresholds) are adapted from
  [e-candeloro/Driver-State-Detection](https://github.com/e-candeloro/Driver-State-Detection),
  MIT License, Copyright (c) 2021 Ettore Candeloro.
- The camera and Face Landmarker setup follows Google's
  [Face Landmarker Raspberry Pi sample](https://github.com/google-ai-edge/mediapipe-samples/tree/main/examples/face_landmarker/raspberry_pi),
  Apache License 2.0.
