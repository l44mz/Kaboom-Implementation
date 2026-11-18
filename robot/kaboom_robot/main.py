"""The robot program: pairs with the backend the first time, then waits for a focus session
started in the app, watches the user with the camera while it runs, reports
FOCUSED / DISTRACTED / AWAY to the backend, and shows Sparky's matching face on the
robot's screen.

Run from the robot folder:
    python -m kaboom_robot.main            # face on the screen, text in the terminal
    python -m kaboom_robot.main --window   # also shows the live camera (Pi desktop / VNC)
    python -m kaboom_robot.main --no-face  # without the screen
Press Ctrl+C (or q in the window) to stop.
"""

import argparse
import socket
import time

import cv2

from kaboom_robot.api.backend import BackendClient, BackendError, NotPairedError
from kaboom_robot.api.robot_socket import RobotSocket, ws_url_from
from kaboom_robot.config import RobotConfig, load_config
from kaboom_robot.display.face_screen import start_face_screen
from kaboom_robot.vision.calibration import Baseline
from kaboom_robot.vision.camera import Camera
from kaboom_robot.vision.face_tracker import FaceTracker
from kaboom_robot.vision.state_machine import FocusState, FocusStateMachine

HEARTBEAT_S = 10  # resend the current state this often, so the backend keeps counting time
CHECK_SESSION_S = 3  # during a session, how often to check it is still running
RETRY_S = 2  # after a failed report, wait this long before trying again

WINDOW_TITLE = "Sparky (q to quit)"
COLORS = {  # BGR, same as the vision demo
    FocusState.FOCUSED: (80, 200, 80),
    FocusState.DISTRACTED: (40, 170, 240),
    FocusState.AWAY: (60, 60, 230),
}


FACE_FOR_STATE = {
    FocusState.FOCUSED: "focused",
    FocusState.DISTRACTED: "distracted",
    FocusState.AWAY: "away",
}


def set_face(face, expression: str) -> None:
    """Changes Sparky's face, if there is a screen."""
    if face is not None:
        face.set(expression)


class QuitRequested(Exception):
    """The user pressed q in the camera window."""


def wait_for_active_session(backend: BackendClient, poll_s: float) -> dict:
    """Ask the backend every poll_s seconds until a session is active, then return it."""
    said_waiting = False
    while True:
        try:
            session = backend.get_active_session()
        except NotPairedError:
            raise  # the tokens are no good: the caller pairs again
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
    except NotPairedError:
        raise
    except BackendError:
        return True
    return (
            current is not None
            and current.get("id") == session_id
            and current.get("status") == "active"
    )


def show_frame(frame, status: str, color, sent: str, obs) -> None:
    """Draw the state on the frame and show it. Raises QuitRequested when q is pressed."""
    lines = [
        status,
        f"face: {'yes' if obs.face_present else 'no'}",
        f"sent to backend: {sent}",
    ]
    for i, text in enumerate(lines):
        cv2.putText(
            frame,
            text,
            (16, 36 + i * 30),
            cv2.FONT_HERSHEY_SIMPLEX,
            1.0 if i == 0 else 0.6,
            color if i == 0 else (255, 255, 255),
            2,
            cv2.LINE_AA,
        )
    cv2.imshow(WINDOW_TITLE, frame)
    if cv2.waitKey(1) & 0xFF == ord("q"):
        raise QuitRequested


def watch_session(
        cfg: RobotConfig,
        backend: BackendClient,
        link: RobotSocket,
        session_id: str,
        window: bool = False,
        face=None,
        initial_explosion_triggered: bool = False,
) -> None:
    """Run the camera and report states until the session is paused or ended."""
    print(f"Session {session_id} is running. Look at your screen for a few seconds...")
    explosion_triggered = initial_explosion_triggered
    set_face(face, "exploded" if explosion_triggered else "happy")
    baseline = Baseline(seconds=cfg.vision.calibration_s)
    machine = FocusStateMachine(cfg.vision.thresholds)

    start = time.monotonic()
    last_sent_state = None
    last_sent_at = 0.0
    retry_at = 0.0
    last_check = start

    with Camera(**vars(cfg.camera)) as camera, FaceTracker(cfg.vision.model_path) as tracker:
        try:
            while True:
                frame = camera.read()
                if frame is None:
                    print("Camera stopped.")
                    return
                now = time.monotonic()

                # What the backend answered since the last frame (over the STOMP socket).
                reply = link.poll_reply()
                while reply is not None:
                    if "error" in reply:
                        print(f"Backend error: {reply['error']}")
                    elif reply.get("explosionTriggered"):
                        explosion_triggered = True
                        set_face(face, "exploded")
                    reply = link.poll_reply()

                obs = tracker.process(frame, int((now - start) * 1000))
                if not baseline.ready:
                    baseline.add(now, obs)
                    status, color = "Calibrating...", (200, 200, 200)
                    if baseline.ready:
                        print("Calibrated. Watching...")
                else:
                    state = machine.update(now, baseline.apply(obs))
                    status = "EXPLODED" if explosion_triggered else state.value
                    color = COLORS[state]

                    if not explosion_triggered:
                        set_face(face, FACE_FOR_STATE[state])
                    changed = state != last_sent_state
                    due = changed or now - last_sent_at >= HEARTBEAT_S
                    if due and now >= retry_at:
                        event_type = "STATE_CHANGE" if changed else "HEARTBEAT"
                        if link.send_state(session_id, state.value, event_type):
                            if changed:
                                reason = ", ".join(machine.reasons) or "all good"
                                print(
                                    f"{now - start:7.1f}s  {state.value:<10} ({reason})  -> sent"
                                )
                            last_sent_state = state
                            last_sent_at = now
                        else:
                            print(f"Could not send {state.value}: not connected")
                            retry_at = now + RETRY_S

                if window:
                    sent = last_sent_state.value if last_sent_state else "nothing yet"
                    show_frame(frame, status, color, sent, obs)

                if now - last_check >= CHECK_SESSION_S:
                    last_check = now
                    if not session_still_active(backend, session_id):
                        print("Session paused or ended. Camera off.")
                        return
        finally:
            if window:
                cv2.destroyAllWindows()


def main() -> None:
    parser = argparse.ArgumentParser(description="Kaboom robot")
    parser.add_argument(
        "--window", action="store_true", help="show the live camera (needs the Pi desktop / VNC)"
    )
    parser.add_argument("--no-face", action="store_true", help="don't use the robot's screen")
    parser.add_argument("--invert", action="store_true", help="screen: use if black shows as white")
    parser.add_argument("--swap-rb", action="store_true", help="screen: use if red and blue swap")
    parser.add_argument("--rotation", default="landscape", help="screen: landscape_flipped, ...")
    parser.add_argument(
        "--serial",
        help="the robot's serial number (default: api.serial_number from config, else the hostname)",
    )
    args = parser.parse_args()

    cfg = load_config()
    serial = args.serial or cfg.api.serial_number or socket.gethostname()
    backend = BackendClient(cfg.api.base_url, cfg.api.robot_id or None, serial_number=serial)

    def show_code(code: str) -> None:
        print(f"\nNot paired. In the app open Connect and enter this code: {code}\n")

    face = None
    if not args.no_face:
        face = start_face_screen(
            invert_colors=args.invert,
            swap_red_blue=args.swap_rb,
            rotation=args.rotation,
        )

    link = None
    try:
        if not backend.is_paired():
            backend.pair(on_code=show_code)
            print("Paired!")
        print(f"Robot {backend.robot_id} using backend {cfg.api.base_url}")

        # One STOMP connection for the whole program; it reconnects by itself and asks the
        # backend client for a fresh device token every time it connects.
        link = RobotSocket(ws_url_from(cfg.api.base_url), token_provider=backend.get_device_token)
        link.start()

        while True:
            try:
                set_face(face, "happy")  # waiting for a session
                session = wait_for_active_session(backend, cfg.api.poll_s)
                watch_session(
                    cfg,
                    backend,
                    link,
                    session["id"],
                    window=args.window,
                    face=face,
                    initial_explosion_triggered=bool(session.get("explosionTriggered", False)),
                )
            except NotPairedError:
                print("The backend no longer accepts this robot. Pairing again...")
                backend.pair(on_code=show_code)
                print("Paired!")
            time.sleep(1)
    except (KeyboardInterrupt, QuitRequested):
        print("\nStopped.")
    finally:
        if link is not None:
            link.stop()
        if face is not None:
            face.stop()


if __name__ == "__main__":
    main()