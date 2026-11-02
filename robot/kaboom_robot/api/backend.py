"""Talks to the Kaboom backend over HTTP (standard library only, nothing to install).

The robot makes two calls:
    GET  /robots/me/active-session?robotId=...   -> the session to watch, or 204 when there is none
    POST /robots/{robotId}/focus-states          -> report FOCUSED / DISTRACTED / AWAY
"""

import json
import time
import urllib.error
import urllib.request


class BackendError(Exception):
    """The backend could not be reached, or it answered with an error."""


class BackendClient:
    def __init__(self, base_url: str, robot_id: str, timeout_s: float = 5.0):
        self.base_url = base_url.rstrip("/")
        self.robot_id = robot_id
        self.timeout_s = timeout_s
        self._last_sequence = 0

    def get_active_session(self) -> dict | None:
        """The robot's active or paused session, or None when there is no session."""
        url = f"{self.base_url}/robots/me/active-session?robotId={self.robot_id}"
        status, body = self._request("GET", url)
        if status == 204 or not body:
            return None
        return body

    def report_state(self, session_id: str, state: str, event_type: str) -> dict:
        """Send one state. event_type is "STATE_CHANGE" or "HEARTBEAT"."""
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
        url = f"{self.base_url}/robots/{self.robot_id}/focus-states"
        _, body = self._request("POST", url, payload)
        return body or {}

    def _request(self, method: str, url: str, payload: dict | None = None):
        data = None if payload is None else json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(
            url,
            data=data,
            method=method,
            headers={"Content-Type": "application/json", "Accept": "application/json"},
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_s) as response:
                raw = response.read()
                return response.status, (json.loads(raw) if raw else None)
        except urllib.error.HTTPError as e:  # the backend answered with 4xx / 5xx
            detail = e.read().decode("utf-8", errors="replace")[:200]
            raise BackendError(f"HTTP {e.code} from {method} {url}: {detail}") from e
        except (urllib.error.URLError, TimeoutError, OSError) as e:  # no answer at all
            raise BackendError(f"cannot reach {url}: {e}") from e
