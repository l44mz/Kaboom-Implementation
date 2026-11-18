"""Plays a scripted sequence of states through the real BackendClient + RobotSocket."""
import sys
import time

from kaboom_robot.api.backend import BackendClient
from kaboom_robot.api.robot_socket import RobotSocket, ws_url_from
from kaboom_robot.config import load_config

SCENARIO = [("FOCUSED", 25), ("DISTRACTED", 15), ("AWAY", 15)]  # (state, seconds)
HEARTBEAT_S = 10

cfg = load_config()
backend = BackendClient(cfg.api.base_url, cfg.api.robot_id or None,
                        serial_number=cfg.api.serial_number)
if not backend.is_paired():
    sys.exit("Pair first: python -m kaboom_robot.main")

link = RobotSocket(ws_url_from(cfg.api.base_url), token_provider=backend.get_device_token)
link.start()
while not link.connected:
    time.sleep(0.2)

session = backend.get_active_session()
if not session or session.get("status") != "active":
    sys.exit("Start a session in Swagger first")
sid = session["id"]
t0 = time.monotonic()


def log(text):
    print(f"{time.monotonic() - t0:6.1f}s  {text}")


def drain():
    reply = link.poll_reply()
    while reply is not None:
        log(f"reply: {reply}")
        reply = link.poll_reply()


for state, seconds in SCENARIO:
    end = time.monotonic() + seconds
    log(f"-> {state} STATE_CHANGE sent={link.send_state(sid, state, 'STATE_CHANGE')}")
    next_hb = time.monotonic() + HEARTBEAT_S
    while time.monotonic() < end:
        time.sleep(0.2)
        drain()
        if next_hb <= time.monotonic() < end:
            log(f"-> {state} HEARTBEAT sent={link.send_state(sid, state, 'HEARTBEAT')}")
            next_hb += HEARTBEAT_S

log("scenario finished: end the session in Swagger NOW")
time.sleep(5)
drain()
link.stop()