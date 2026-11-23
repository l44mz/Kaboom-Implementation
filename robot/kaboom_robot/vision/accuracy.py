"""Measures how often the focus detection is right, with a real person.

A helper watches the user and presses a key whenever the user's REAL state changes,
while the robot keeps detecting on its own. At the end it prints a score and saves
a CSV (every frame) and a Markdown report (for docs/qa) in recordings/.

Run from the robot folder:
    python -m kaboom_robot.vision.accuracy --user "User 1"
Keys in the window:
    f = really focused   d = really distracted   a = really away   q = stop
Score saved recordings again (for example all 5 users together):
    python -m kaboom_robot.vision.accuracy --report recordings/*.csv
"""

import argparse
import glob
import time
from datetime import datetime
from pathlib import Path

import cv2

from kaboom_robot.config import load_config
from kaboom_robot.vision import scoring
from kaboom_robot.vision.calibration import Baseline
from kaboom_robot.vision.camera import Camera
from kaboom_robot.vision.face_tracker import FaceTracker
from kaboom_robot.vision.state_machine import FocusState, FocusStateMachine

KEYS = {ord("f"): "FOCUSED", ord("d"): "DISTRACTED", ord("a"): "AWAY"}
COLORS = {  # BGR
    "FOCUSED": (80, 200, 80),
    "DISTRACTED": (40, 170, 240),
    "AWAY": (60, 60, 230),
}


def _num(value):
    return "" if value is None else f"{value:.4f}"


def record(user: str, out_dir: Path) -> None:
    cfg = load_config()
    baseline = Baseline(seconds=cfg.vision.calibration_s)
    machine = FocusStateMachine(cfg.vision.thresholds)
    rows: list[dict] = []
    truth = "FOCUSED"  # sessions start with the user working
    start = time.monotonic()

    print(f"Recording {user}. The user sits as they normally work.")
    print("Press f / d / a when the user REALLY becomes focused / distracted / away. q stops.")
    with Camera(**vars(cfg.camera)) as camera, FaceTracker(cfg.vision.model_path) as tracker:
        try:
            while True:
                frame = camera.read()
                if frame is None:
                    print("Camera stopped.")
                    break
                now = time.monotonic()
                t = now - start
                obs = tracker.process(frame, int(t * 1000))
                if not baseline.ready:
                    baseline.add(now, obs)
                    predicted = "CALIBRATING"
                else:
                    adjusted = baseline.apply(obs)
                    state = machine.update(now, adjusted)
                    baseline.adapt(now, obs, focused=state == FocusState.FOCUSED)
                    predicted = state.value
                    obs = adjusted
                rows.append(
                    {
                        "t": f"{t:.3f}",
                        "truth": truth,
                        "predicted": predicted,
                        "ear": _num(obs.ear),
                        "gaze": _num(obs.gaze),
                        "yaw": _num(obs.yaw),
                        "pitch": _num(obs.pitch),
                        "roll": _num(obs.roll),
                    }
                )

                agree = predicted == truth
                lines = [
                    (f"Real: {truth}", COLORS[truth]),
                    (f"Robot: {predicted}", COLORS.get(predicted, (200, 200, 200))),
                    (
                        "agree" if agree else "different (the robot may still be waiting)",
                        (255, 255, 255),
                    ),
                    ("f focused   d distracted   a away   q stop", (200, 200, 200)),
                ]
                for i, (text, color) in enumerate(lines):
                    cv2.putText(
                        frame,
                        text,
                        (16, 36 + i * 32),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.8 if i < 2 else 0.55,
                        color,
                        2,
                        cv2.LINE_AA,
                    )
                cv2.imshow("Kaboom accuracy test", frame)
                key = cv2.waitKey(1) & 0xFF
                if key == ord("q"):
                    break
                if key in KEYS and KEYS[key] != truth:
                    truth = KEYS[key]
                    print(f"{t:7.1f}s  real state: {truth}")
        except KeyboardInterrupt:
            pass
        finally:
            cv2.destroyAllWindows()

    if not rows:
        print("Nothing recorded.")
        return
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    name = "".join(c if c.isalnum() else "-" for c in user).strip("-").lower() or "user"
    csv_path = out_dir / f"accuracy-{name}-{stamp}.csv"
    scoring.write_csv(csv_path, rows)
    _report([csv_path], cfg.vision.thresholds, title=f"Focus detection accuracy: {user}")


def _report(paths: list[Path], thresholds, title: str) -> None:
    recordings = [scoring.read_csv(p) for p in paths]
    report = scoring.score(recordings, scoring.allowed_delays(thresholds))
    text = scoring.to_markdown(report, title)
    md_path = paths[0].with_suffix(".md") if len(paths) == 1 else paths[0].parent / "accuracy.md"
    md_path.write_text(text, encoding="utf-8")
    print()
    print(text)
    print(f"Saved: {md_path}" + (f" and {paths[0]}" if len(paths) == 1 else ""))


def main() -> None:
    parser = argparse.ArgumentParser(description="Kaboom focus detection accuracy test")
    parser.add_argument("--user", default="User", help="name or number of the test user")
    parser.add_argument("--out", default="recordings", help="folder for the CSV and report")
    parser.add_argument(
        "--report",
        nargs="+",
        metavar="CSV",
        help="score saved recordings instead of recording a new one",
    )
    args = parser.parse_args()

    if args.report:
        paths = [Path(p) for pattern in args.report for p in sorted(glob.glob(pattern))]
        if not paths:
            print("No CSV files found.")
            return
        title = "Focus detection accuracy"
        if len(paths) > 1:
            title += f" ({len(paths)} recordings)"
        _report(paths, load_config().vision.thresholds, title=title)
    else:
        record(args.user, Path(args.out))


if __name__ == "__main__":
    main()
