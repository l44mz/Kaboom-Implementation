"""Talks to the Kaboom backend over HTTP (standard library only, nothing to install).

The robot makes these calls:
    POST /robots/pairing-codes          -> ask for a pairing code to show to the user
    POST /robots/pairing-status         -> collect the device tokens once the code was entered
    POST /auth/refresh                  -> new access token when the old one expires
    GET  /robots/me/active-session      -> the session to watch, or 204 when there is none
    POST /robots/{robotId}/focus-states -> report a state over HTTP (normally the STOMP
                                           socket in robot_socket.py is used instead)
"""

import json
import os
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

DEFAULT_TOKEN_FILE = Path.home() / ".kaboom_robot" / "device_tokens.json"


class BackendError(Exception):
    """The backend could not be reached, or it answered with an error."""

    def __init__(self, message: str, status: int | None = None):
        super().__init__(message)
        self.status = status  # HTTP status, or None when there was no answer at all


class NotPairedError(BackendError):
    """The robot has no valid device tokens: it has to be paired (again)."""


class BackendClient:
    def __init__(
            self,
            base_url: str,
            robot_id: str | None = None,
            serial_number: str = "",
            token_file: Path = DEFAULT_TOKEN_FILE,
            timeout_s: float = 5.0,
    ):
        self.base_url = base_url.rstrip("/")
        self.robot_id = robot_id  # replaced by the id saved at pairing, if there is one
        self.serial_number = serial_number
        self.token_file = Path(token_file)
        self.timeout_s = timeout_s
        self._last_sequence = 0
        self._lock = threading.RLock()  # the socket thread and the main loop both use tokens
        self._access_token: str | None = None
        self._refresh_token: str | None = None
        self._expires_at = 0.0
        self._load_tokens()

    # ---------- pairing and tokens ----------

    def is_paired(self) -> bool:
        return self._refresh_token is not None

    def pair(self, on_code=print) -> None:
        """Ask for a pairing code, hand it to on_code, and wait until the user has entered
        it in the app. Asks for a new code when the old one expires."""
        while True:
            _, reply = self._call("POST", "/robots/pairing-codes", {"serialNumber": self.serial_number})
            secret = reply["pairingSecret"]  # stays in memory, never printed
            on_code(reply["pairingCode"])

            while True:
                time.sleep(3)
                try:
                    _, status = self._call("POST", "/robots/pairing-status", {"pairingSecret": secret})
                except BackendError as e:
                    if e.status in (404, 410):  # the code expired: ask for a new one
                        break
                    print(f"Pairing: {e}")
                    continue
                if status.get("status") == "paired":
                    self.robot_id = status["robotId"]
                    self._store(
                        status["deviceAccessToken"],
                        status["expiresInSeconds"],
                        refresh=status["deviceRefreshToken"],
                    )
                    return

    def get_device_token(self) -> str:
        """A valid device access token. Refreshes it shortly before it expires."""
        with self._lock:
            if not self._refresh_token:
                raise NotPairedError("robot is not paired")
            if time.time() >= self._expires_at - 30:
                self._refresh()
            return self._access_token

    def _refresh(self) -> None:
        with self._lock:
            try:
                _, body = self._call("POST", "/auth/refresh", {"refreshToken": self._refresh_token})
            except BackendError as e:
                if e.status in (400, 401, 403):  # the tokens are no good any more
                    self._forget_tokens()
                    raise NotPairedError("pairing is no longer valid, pair again") from e
                raise
            self._store(body["accessToken"], body["expiresInSeconds"])

    # ---------- the calls the robot makes ----------

    def get_active_session(self) -> dict | None:
        """The robot's active or paused session, or None when there is no session."""
        path = "/robots/me/active-session"
        if self.robot_id:
            path += f"?robotId={self.robot_id}"  # older backends need it, newer ones ignore it
        status, body = self._authed("GET", path)
        if status == 204 or not body:
            return None
        return body

    def report_state(self, session_id: str, state: str, event_type: str) -> dict:
        """Send one state over HTTP. event_type is "STATE_CHANGE" or "HEARTBEAT"."""
        now_ms = int(time.time() * 1000)
        # Must always go up, even across pauses and restarts, or the backend ignores the report.
        sequence = max(self._last_sequence + 1, now_ms)
        self._last_sequence = sequence

        payload = {
            "sessionId": session_id,
            "state": state,
            "timestamp": now_ms,
            "sequenceNumber": sequence,
            "eventType": event_type,
        }
        _, body = self._authed("POST", f"/robots/{self.robot_id}/focus-states", payload)
        return body or {}

    # ---------- internals ----------

    def _authed(self, method: str, path: str, payload: dict | None = None):
        """A call with the device token. If the backend rejects the token, refresh and retry once."""
        try:
            return self._call(method, path, payload, self.get_device_token())
        except BackendError as e:
            if e.status != 401:
                raise
        self._refresh()
        return self._call(method, path, payload, self._access_token)

    def _call(self, method: str, path: str, payload: dict | None = None, token: str | None = None):
        url = self.base_url + path
        data = None if payload is None else json.dumps(payload).encode("utf-8")
        headers = {"Content-Type": "application/json", "Accept": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        request = urllib.request.Request(url, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_s) as response:
                raw = response.read()
                return response.status, (json.loads(raw) if raw else None)
        except urllib.error.HTTPError as e:  # the backend answered with 4xx / 5xx
            detail = e.read().decode("utf-8", errors="replace")[:200]
            raise BackendError(f"HTTP {e.code} from {method} {url}: {detail}", status=e.code) from e
        except (urllib.error.URLError, TimeoutError, OSError) as e:  # no answer at all
            raise BackendError(f"cannot reach {url}: {e}") from e

    def _store(self, access: str, expires_in: int, refresh: str | None = None) -> None:
        with self._lock:
            self._access_token = access
            self._expires_at = time.time() + expires_in
            if refresh:
                self._refresh_token = refresh
            self._save_tokens()

    def _save_tokens(self) -> None:
        self.token_file.parent.mkdir(parents=True, exist_ok=True)
        self.token_file.write_text(
            json.dumps(
                {
                    "robotId": self.robot_id,
                    "accessToken": self._access_token,
                    "refreshToken": self._refresh_token,
                    "expiresAt": self._expires_at,
                }
            )
        )
        try:
            os.chmod(self.token_file, 0o600)  # only this user may read it
        except OSError:
            pass

    def _load_tokens(self) -> None:
        try:
            data = json.loads(self.token_file.read_text())
        except (OSError, ValueError):
            return
        self.robot_id = data.get("robotId") or self.robot_id
        self._access_token = data.get("accessToken")
        self._refresh_token = data.get("refreshToken")
        self._expires_at = float(data.get("expiresAt") or 0)

    def _forget_tokens(self) -> None:
        self._access_token = self._refresh_token = None
        self._expires_at = 0.0
        try:
            self.token_file.unlink()
        except OSError:
            pass