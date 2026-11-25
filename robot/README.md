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

## How the detection decides

- **Calibration (first 3 s):** learns the user's normal working pose (head yaw, pitch and
  tilt), where their eyes normally look, and how open their eyes normally are.
- **Per-person eyes:** after calibration, "eyes closed" means under 60% of the user's own
  open eyes (`ear_closed_ratio`), so narrow eyes or glasses are not mistaken for sleep.
- **Gaze direction:** the eyes are measured left/right and up/down from where the user
  normally looks, not just "how far from the middle".
- **Smoothing:** each number is averaged over about 0.15 s (`smoothing_s`), so small jitter
  near a limit does not flip the state.
- **Slow re-calibration:** while the user is focused, the working pose follows slow changes
  (leaning back, moving the chair) over about a minute. Distractions are never learned.
- **Closest face:** if two people are in view, the biggest (closest) face is tracked.

## Accuracy test

Measures how often the detection is right, with a real person. A helper watches the user
and presses a key whenever the user's **real** state changes; the robot detects on its own.

```bash
python -m kaboom_robot.vision.accuracy --user "User 1"
```

Keys in the window: `f` really focused, `d` really distracted, `a` really away, `q` stop.
At the end it prints the score and saves a CSV and a Markdown report in `recordings/`.
The robot is allowed its normal waiting time after each change (5 s for distracted, 3 s for
away), so that time is not counted as a mistake, and a glance shorter than that is not a miss.

Score several recordings together (for example, all 5 test users):

```bash
python -m kaboom_robot.vision.accuracy --report "recordings/*.csv"
```

A suggested 5-minute script per user: work normally 1 min, look at your phone 1 min, work
1 min, leave the desk 1 min, come back and work 1 min. Copy the final report into `docs/qa/`.

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
