"""STOMP-over-WebSocket client for the backend. Runs in a background thread,
reconnects by itself, and puts the server's replies in a queue for the main loop."""

import json
import queue
import threading
import time
from urllib.parse import urlparse

import websocket


def ws_url_from(base_url: str) -> str:
    """http://host:8080/api/v1  ->  ws://host:8080/ws"""
    parsed = urlparse(base_url)
    scheme = "wss" if parsed.scheme == "https" else "ws"
    return f"{scheme}://{parsed.netloc}/ws"


def _frame(command, headers=None, body=""):
    head = "".join(f"{k}:{v}\n" for k, v in (headers or {}).items())
    return f"{command}\n{head}\n{body}\x00"


def _parse(raw):
    raw = raw.lstrip("\n").rstrip("\x00")
    if not raw.strip():
        return None, {}, ""  # heartbeat
    head, _, body = raw.partition("\n\n")
    lines = head.split("\n")
    headers = dict(line.split(":", 1) for line in lines[1:] if ":" in line)
    return lines[0], headers, body


class RobotSocket:
    def __init__(self, url, token_provider):
        self.url = url
        self.token_provider = token_provider  # returns a valid device access token
        self.replies = queue.SimpleQueue()
        self.connected = False  # True only after the server's CONNECTED frame
        self._ws = None
        self._stopped = False
        self._last_seq = 0

    def start(self):
        threading.Thread(target=self._loop, daemon=True).start()

    def stop(self):
        self._stopped = True
        if self._ws is not None:
            self._ws.close()

    def poll_reply(self):
        """Next reply from the backend (a dict), or None if there is none."""
        try:
            return self.replies.get_nowait()
        except queue.Empty:
            return None

    def send_state(self, session_id, state, event_type):
        """Returns True if the message was written to the socket."""
        if not self.connected:
            return False
            # Milliseconds since 1970 and always rising: stays valid across pauses and Pi
        # restarts, because the backend remembers the last number of each session.
        now_ms = int(time.time() * 1000)
        self._last_seq = max(self._last_seq + 1, now_ms)
        payload = {
            "sessionId": session_id,
            "state": state,
            "timestamp": now_ms,
            "sequenceNumber": self._last_seq,
            "eventType": event_type,
        }

        try:
            self._ws.send(_frame(
                "SEND",
                {"destination": "/app/focus-states", "content-type": "application/json"},
                json.dumps(payload)))
            return True
        except Exception:
            return False

    # ---- internals ----

    def _loop(self):
        while not self._stopped:
            self.connected = False
            self._ws = websocket.WebSocketApp(
                self.url,
                on_open=self._on_open,
                on_message=self._on_frame,
                on_close=lambda ws, *a: setattr(self, "connected", False),
                on_error=lambda ws, e: print(f"Socket error: {e}"),
            )
            self._ws.run_forever(ping_interval=20, ping_timeout=10)
            if not self._stopped:
                time.sleep(3)  # then reconnect with a fresh token

    def _on_open(self, ws):
        try:
            token = self.token_provider()
        except Exception as e:
            print(f"Could not get a device token: {e}")
            ws.close()
            return
        ws.send(_frame("CONNECT", {
            "accept-version": "1.2",
            "host": "kaboom",
            "heart-beat": "0,0",
            "Authorization": f"Bearer {token}",
        }))

    def _on_frame(self, ws, raw):
        command, headers, body = _parse(raw)
        if command == "CONNECTED":
            ws.send(_frame("SUBSCRIBE", {"id": "sub-0", "destination": "/user/queue/expression"}))
            ws.send(_frame("SUBSCRIBE", {"id": "sub-1", "destination": "/user/queue/errors"}))
            self.connected = True
        elif command == "MESSAGE":
            try:
                self.replies.put(json.loads(body))  # errors arrive as {"error": "..."}
            except Exception:
                pass
        elif command == "ERROR":
            print(f"STOMP error: {headers.get('message')}")
            ws.close()