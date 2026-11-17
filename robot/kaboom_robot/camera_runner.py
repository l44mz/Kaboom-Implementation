
"""Standalone Camera Mode runner for Kaboom."""

import argparse
import json
import os
import time
import urllib.error
import urllib.request

import cv2

from kaboom_robot.config import load_config
from kaboom_robot.vision.calibration import Baseline
from kaboom_robot.vision.camera import Camera
from kaboom_robot.vision.face_tracker import FaceTracker
from kaboom_robot.vision.state_machine import FocusStateMachine


HEARTBEAT_INTERVAL = 10

COLORS = {
    "FOCUSED": (80, 200, 80),
    "DISTRACTED": (40, 170, 240),
    "AWAY": (60, 60, 230),
}


def api_request(method, url, token, payload=None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")

    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
    )

    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            content = response.read().decode("utf-8")
            return json.loads(content) if content else {}

    except urllib.error.HTTPError as error:
        details = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"Backend returned HTTP {error.code}: {details}"
        ) from error

    except urllib.error.URLError as error:
        raise RuntimeError(
            f"Cannot connect to backend: {error.reason}"
        ) from error


def send_state(base_url, session_id, token, state, sequence, event_type):
    payload = {
        "sessionId": session_id,
        "state": state,
        "timestamp": int(time.time() * 1000),
        "sequenceNumber": sequence,
        "eventType": event_type,
    }

    url = f"{base_url}/camera-sessions/{session_id}/state"

    response = api_request("POST", url, token, payload)

    print(
        f"[API] {event_type}: {state} "
        f"(sequence={sequence})"
    )

    return response


def end_session(base_url, session_id, token):
    url = f"{base_url}/camera-sessions/{session_id}/end"

    result = api_request("PUT", url, token)

    print("\nCamera session completed!")
    print(json.dumps(result, indent=2))

    return result


def main():
    parser = argparse.ArgumentParser(
        description="Kaboom Camera Mode"
    )

    parser.add_argument("--session-id", required=True)

    parser.add_argument(
        "--base-url",
        default="http://localhost:8080/api/v1",
    )

    args = parser.parse_args()

    token = os.environ.get("KABOOM_TOKEN")

    if not token:
        raise RuntimeError(
            "KABOOM_TOKEN environment variable is missing."
        )

    base_url = args.base_url.rstrip("/")
    session_id = args.session_id

    cfg = load_config()

    baseline = Baseline(seconds=cfg.vision.calibration_s)
    machine = FocusStateMachine(cfg.vision.thresholds)

    sequence = 0
    last_sent_state = None
    last_heartbeat = 0.0

    start = time.monotonic()

    print("Starting Kaboom Camera Mode...")
    print("Calibrating your normal working position.")
    print("Please look at your screen.")

    try:
        with Camera(**vars(cfg.camera)) as camera, \
                FaceTracker(cfg.vision.model_path) as tracker:

            while True:
                frame = camera.read()

                if frame is None:
                    print("Camera stopped.")
                    break

                now = time.monotonic()

                observation = tracker.process(
                    frame,
                    int((now - start) * 1000),
                )

                if not baseline.ready:
                    baseline.add(now, observation)
                    status = "Calibrating..."
                    color = (200, 200, 200)

                else:
                    adjusted_observation = baseline.apply(observation)

                    state = machine.update(
                        now,
                        adjusted_observation,
                    )

                    status = state.value
                    color = COLORS[status]

                    if state != last_sent_state:
                        sequence += 1

                        send_state(
                            base_url,
                            session_id,
                            token,
                            state.value,
                            sequence,
                            "STATE_CHANGE",
                        )

                        last_sent_state = state
                        last_heartbeat = now

                        reason = ", ".join(machine.reasons) or "all good"
                        print(f"[VISION] {status}: {reason}")

                    elif now - last_heartbeat >= HEARTBEAT_INTERVAL:
                        sequence += 1

                        send_state(
                            base_url,
                            session_id,
                            token,
                            state.value,
                            sequence,
                            "HEARTBEAT",
                        )

                        last_heartbeat = now

                cv2.putText(
                    frame,
                    status,
                    (16, 40),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    1,
                    color,
                    2,
                    cv2.LINE_AA,
                )

                cv2.putText(
                    frame,
                    "Press Q to finish session",
                    (16, 80),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    (255, 255, 255),
                    2,
                    cv2.LINE_AA,
                )

                cv2.imshow("Kaboom Camera Mode", frame)

                if cv2.waitKey(1) & 0xFF == ord("q"):
                    print("Stopping Camera Mode...")
                    break

    except KeyboardInterrupt:
        print("\nCamera Mode interrupted.")

    finally:
        cv2.destroyAllWindows()

    end_session(base_url, session_id, token)


if __name__ == "__main__":
    main()
