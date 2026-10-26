import json, threading, time
import websocket

class RobotSocket:
    def __init__(self, url, on_message):
        self.url = url
        self.on_message = on_message      # called with the parsed server reply
        self.ws = None
        self.connected = False

    def start(self):
        threading.Thread(target=self._loop, daemon=True).start()

    def _loop(self):
        while True:
            self.ws = websocket.WebSocketApp(
                self.url,
                on_open=lambda ws: setattr(self, "connected", True),
                on_message=self._handle,
                on_close=lambda ws, *a: setattr(self, "connected", False),
                on_error=lambda ws, e: None,
            )
            self.ws.run_forever(ping_interval=20, ping_timeout=10)
            self.connected = False
            time.sleep(3)                 # wait, then reconnect

    def _handle(self, ws, message):
        try:
            self.on_message(json.loads(message))
        except Exception:
            pass

    def send(self, payload):
        if not self.connected:
            return False
        try:
            self.ws.send(json.dumps(payload))
            return True
        except Exception:
            return False