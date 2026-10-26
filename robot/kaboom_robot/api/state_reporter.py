import time

class StateReporter:
    def __init__(self, socket, session_id, hold=3, heartbeat=10):
        self.socket, self.session_id = socket, session_id
        self.hold, self.heartbeat = hold, heartbeat
        self.current = None
        self.candidate, self.candidate_since = None, 0
        self.last_sent = 0
        self.seq = 0

    def update(self, detected):
        now = time.time()
        if detected != self.candidate:
            self.candidate, self.candidate_since = detected, now
        stable = now - self.candidate_since >= self.hold

        if stable and self.candidate != self.current:
            if self._send(self.candidate, "STATE_CHANGE"):
                self.current = self.candidate
        elif self.current and now - self.last_sent >= self.heartbeat:
            self._send(self.current, "HEARTBEAT")

    def _send(self, state, event_type):
        self.seq += 1
        ok = self.socket.send({
            "sessionId": self.session_id,
            "state": state,
            "timestamp": int(time.time() * 1000),
            "sequenceNumber": self.seq,
            "eventType": event_type,
        })
        if ok:
            self.last_sent = time.time()
        return ok