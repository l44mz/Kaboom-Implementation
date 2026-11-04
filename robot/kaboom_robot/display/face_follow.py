"""Sparky's face follows the camera: FOCUSED, DISTRACTED and AWAY show the matching face.

The camera and MediaPipe run in a background thread; the face is drawn and sent to the
screen in the main loop, so a slow camera frame never freezes the animation.
While Sparky learns your normal pose (the first seconds), it shows the happy face.

Run from the robot folder, in the mp-test environment:
    python -m kaboom_robot.display.face_follow              # on the screen
    python -m kaboom_robot.display.face_follow --preview    # in a window (Pi over VNC)
Ctrl+C (or q in the preview window) stops it.
"""

import argparse
import threading
import time
import traceback

from kaboom_robot.config import load_config
from kaboom_robot.display.face import FaceAnimator
from kaboom_robot.display.face_demo import open_output
from kaboom_robot.vision.calibration import Baseline
from kaboom_robot.vision.camera import Camera
from kaboom_robot.vision.face_tracker import FaceTracker
from kaboom_robot.vision.state_machine import FocusState, FocusStateMachine

FACE_FOR_STATE = {
    FocusState.FOCUSED: "focused",
    FocusState.DISTRACTED: "distracted",
    FocusState.AWAY: "away",
}


class VisionThread(threading.Thread):
    """Reads the camera and keeps `expression` up to date. Stops on stop()."""

    def __init__(self, cfg):
        super().__init__(daemon=True)
        self.cfg = cfg
        self.expression = "happy"  # shown while calibrating
        self.error = None
        self._stop_event = threading.Event()

    def stop(self) -> None:
        self._stop_event.set()

    def run(self) -> None:
        try:
            self._watch()
        except Exception as e:  # show the problem in the main loop instead of dying silently
            traceback.print_exc()
            self.error = str(e)

    def _watch(self) -> None:
        cfg = self.cfg
        baseline = Baseline(seconds=cfg.vision.calibration_s)
        machine = FocusStateMachine(cfg.vision.thresholds)
        start = time.monotonic()
        last_state = None

        with Camera(**vars(cfg.camera)) as camera, FaceTracker(cfg.vision.model_path) as tracker:
            print("Look at the screen for a few seconds so Sparky learns your normal pose...")
            while not self._stop_event.is_set():
                frame = camera.read()
                if frame is None:
                    self.error = "Camera stopped."
                    return
                now = time.monotonic()
                obs = tracker.process(frame, int((now - start) * 1000))

                if not baseline.ready:
                    baseline.add(now, obs)
                    if baseline.ready:
                        print("Calibrated. Watching...")
                    continue

                state = machine.update(now, baseline.apply(obs))
                self.expression = FACE_FOR_STATE[state]
                if state != last_state:
                    reason = ", ".join(machine.reasons) or "all good"
                    print(f"{now - start:7.1f}s  {state.value:<10} ({reason})")
                    last_state = state


def main() -> None:
    parser = argparse.ArgumentParser(description="Sparky's face follows the camera")
    parser.add_argument("--preview", action="store_true", help="window instead of the screen")
    parser.add_argument("--fps", type=float, default=15.0, help="face frames per second")
    parser.add_argument("--smooth", type=int, default=1,
                        help="1 = faster (default, leaves CPU for the camera), 2 = smoother edges")
    parser.add_argument("--rotation", default="landscape",
                        help="landscape, landscape_flipped, portrait or portrait_flipped")
    parser.add_argument("--swap-rb", action="store_true", help="use if red and blue look swapped")
    parser.add_argument("--invert", action="store_true", help="use if black shows as white")
    parser.add_argument("--rs-invert", action="store_true", help="RS high = command")
    parser.add_argument("--delay", type=int, default=1, help="slow the screen bus down (2, 3...)")
    args = parser.parse_args()

    cfg = load_config()
    vision = VisionThread(cfg)
    animator = FaceAnimator("happy", supersample=args.smooth)
    show, close = open_output(args)

    vision.start()
    print("Sparky is watching. Ctrl+C to stop.")
    try:
        while True:
            now = time.monotonic()
            if vision.error:
                print(f"Vision stopped: {vision.error}")
                break
            animator.set_expression(vision.expression)
            if show(animator.frame(now)) == "q":
                break
            time.sleep(max(0.0, 1 / args.fps - (time.monotonic() - now)))
    except KeyboardInterrupt:
        pass
    finally:
        vision.stop()
        vision.join(timeout=3)
        close()


if __name__ == "__main__":
    main()
