"""The robot program: waits for a focus session started in the app, watches the user
with the camera while it runs, and reports FOCUSED / DISTRACTED / AWAY to the backend.

Run from the robot folder:
    python -m kaboom_robot.main
Press Ctrl+C to stop.
"""

import time

from kaboom_robot.api.backend import BackendClient, BackendError
from kaboom_robot.config import RobotConfig, load_config
from kaboom_robot.vision.calibration import Baseline
from kaboom_robot.vision.camera import Camera
from kaboom_robot.vision.face_tracker import FaceTracker
from kaboom_robot.vision.state_machine import FocusStateMachine

HEARTBEAT_S = 10  # resend the current state this often, so the backend keeps counting time
CHECK_SESSION_S = 3  # during a session, how often to check it is still running
RETRY_S = 2  # after a failed report, wait this long before trying again


def wait_for_active_session(backend: BackendClient, poll_s: float) -> dict:
    """Ask the backend every poll_s seconds until a session is active, then return it."""
    said_waiting = False
    while True:
        try:
            session = backend.get_active_session()
        except BackendError as e:
            print(f"Backend not reachable, retrying: {e}")
            session = None

        if session and session.get("status") == "active":
            return session

        if not said_waiting:
            print("Waiting for a session... (press Start Focus in the app)")
            said_waiting = True
        time.sleep(poll_s)


def session_still_active(backend: BackendClient, session_id: str) -> bool:
    """False once the session is paused, ended, or replaced. Network errors count as active."""
    try:
        current = backend.get_active_session()
    except BackendError:
        return True
    return (
        current is not None
        and current.get("id") == session_id
        and current.get("status") == "active"
    )


def watch_session(cfg: RobotConfig, backend: BackendClient, session_id: str) -> None:
    """Run the camera and report states until the session is paused or ended."""
    print(f"Session {session_id} is running. Look at your screen for a few seconds...")
    baseline = Baseline(seconds=cfg.vision.calibration_s)
    machine = FocusStateMachine(cfg.vision.thresholds)

    start = time.monotonic()
    last_sent_state = None
    last_sent_at = 0.0
    retry_at = 0.0
    last_check = start

    with Camera(**vars(cfg.camera)) as camera, FaceTracker(cfg.vision.model_path) as tracker:
        while True:
            frame = camera.read()
            if frame is None:
                print("Camera stopped.")
                return
            now = time.monotonic()

            obs = tracker.process(frame, int((now - start) * 1000))
            if not baseline.ready:
                baseline.add(now, obs)
                if baseline.ready:
                    print("Calibrated. Watching...")
            else:
                state = machine.update(now, baseline.apply(obs))
                changed = state != last_sent_state
                due = changed or now - last_sent_at >= HEARTBEAT_S
                if due and now >= retry_at:
                    event_type = "STATE_CHANGE" if changed else "HEARTBEAT"
                    try:
                        reply = backend.report_state(session_id, state.value, event_type)
                        if changed:
                            reason = ", ".join(machine.reasons) or "all good"
                            print(
                                f"{now - start:7.1f}s  {state.value:<10} ({reason})"
                                f"  -> backend: {reply.get('message', 'ok')}"
                            )
                        last_sent_state = state
                        last_sent_at = now
                    except BackendError as e:
                        print(f"Could not send {state.value}: {e}")
                        retry_at = now + RETRY_S

            if now - last_check >= CHECK_SESSION_S:
                last_check = now
                if not session_still_active(backend, session_id):
                    print("Session paused or ended. Camera off.")
                    return


def main() -> None:
    cfg = load_config()
    if not cfg.api.robot_id:
        print("Set api.robot_id in config.yaml first (the robot's id from the robots table).")
        return

    backend = BackendClient(cfg.api.base_url, cfg.api.robot_id)
    print(f"Robot {cfg.api.robot_id} using backend {cfg.api.base_url}")

    try:
        while True:
            session = wait_for_active_session(backend, cfg.api.poll_s)
            watch_session(cfg, backend, session["id"])
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
