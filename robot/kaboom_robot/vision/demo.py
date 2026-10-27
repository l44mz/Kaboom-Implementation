"""Live test of the detection: shows the camera with the state and numbers on top.

Run from the robot folder:
    python -m kaboom_robot.vision.demo              # with a window (Mac, or Pi with a screen)
    python -m kaboom_robot.vision.demo --no-window  # print only (Pi over SSH)
Press q in the window, or Ctrl+C in the terminal, to stop.
"""

import argparse
import time

import cv2

from kaboom_robot.config import load_config
from kaboom_robot.vision.calibration import Baseline
from kaboom_robot.vision.camera import Camera
from kaboom_robot.vision.face_tracker import FaceTracker
from kaboom_robot.vision.state_machine import FocusState, FocusStateMachine

COLORS = {  # BGR
    FocusState.FOCUSED: (80, 200, 80),
    FocusState.DISTRACTED: (40, 170, 240),
    FocusState.AWAY: (60, 60, 230),
}


def _fmt(value, digits=2):
    return "--" if value is None else f"{value:.{digits}f}"


def main() -> None:
    parser = argparse.ArgumentParser(description="Kaboom focus detection demo")
    parser.add_argument("--no-window", action="store_true", help="print only, no video window")
    args = parser.parse_args()

    cfg = load_config()
    baseline = Baseline(seconds=cfg.vision.calibration_s)
    machine = FocusStateMachine(cfg.vision.thresholds)
    start = time.monotonic()
    last_state = None
    last_frame = start
    fps = 0.0

    print("Sit as you normally work and look at your screen for a few seconds...")
    with Camera(**vars(cfg.camera)) as camera, FaceTracker(cfg.vision.model_path) as tracker:
        try:
            while True:
                frame = camera.read()
                if frame is None:
                    print("Camera stopped.")
                    break
                now = time.monotonic()
                fps = 0.9 * fps + 0.1 / max(now - last_frame, 1e-6)
                last_frame = now

                obs = tracker.process(frame, int((now - start) * 1000))
                if not baseline.ready:
                    baseline.add(now, obs)
                    status = "Calibrating..."
                    color = (200, 200, 200)
                else:
                    state = machine.update(now, baseline.apply(obs))
                    if state != last_state:
                        reason = ", ".join(machine.reasons) or "all good"
                        print(f"{now - start:7.1f}s  {state.value:<10}  ({reason})")
                        last_state = state
                    status = state.value
                    color = COLORS[state]
                    obs = baseline.apply(obs)

                if args.no_window:
                    continue
                lines = [
                    status,
                    f"face: {'yes' if obs.face_present else 'no'}   fps: {fps:.0f}",
                    f"EAR: {_fmt(obs.ear)}   gaze: {_fmt(obs.gaze)}",
                    f"yaw: {_fmt(obs.yaw, 0)}  pitch: {_fmt(obs.pitch, 0)}"
                    f"  roll: {_fmt(obs.roll, 0)}",
                ]
                if machine.reasons:
                    lines.append("why: " + ", ".join(machine.reasons))
                for i, text in enumerate(lines):
                    scale = 1.0 if i == 0 else 0.6
                    cv2.putText(
                        frame,
                        text,
                        (16, 36 + i * 30),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        scale,
                        color if i == 0 else (255, 255, 255),
                        2,
                        cv2.LINE_AA,
                    )
                cv2.imshow("Kaboom focus detection (q to quit)", frame)
                if cv2.waitKey(1) & 0xFF == ord("q"):
                    break
        except KeyboardInterrupt:
            pass
        finally:
            cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
