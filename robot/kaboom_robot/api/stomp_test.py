import time
from robot_socket import RobotSocket

DEVICE_TOKEN = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIwYzE0ZGEzNS0zZWZmLTRjOTItYWU5Ny02Yjk4ZDBmNjU3YzUiLCJyb2JvdElkIjoiMGMxNGRhMzUtM2VmZi00YzkyLWFlOTctNmI5OGQwZjY1N2M1IiwidHlwZSI6ImRldmljZV9hY2Nlc3MiLCJpYXQiOjE3OTQzMDMxNTQsImV4cCI6MTc5NDMwMzc1NH0.XZ0Ps-wKfSCjMdmuNUe7s0oRMGihQw7woSQhdEX2CfiQPCPw2Vib41S1pJ41R23g"          # device access token (type device_access, valid 10 min)
SESSION_ID = "66c5b8c4-5517-4405-a66d-c371be61ccf2"

def on_msg(destination, data):
    print(destination, "->", data)

sock = RobotSocket("ws://localhost:8080/ws", lambda: DEVICE_TOKEN, on_msg)
sock.start()
while not sock.connected:
    time.sleep(0.2)

for seq, state in enumerate(["FOCUSED", "FOCUSED", "DISTRACTED", "AWAY"], start=1):
    sock.send({"sessionId": SESSION_ID, "state": state,
               "timestamp": int(time.time() * 1000),
               "sequenceNumber": seq, "eventType": "STATE_CHANGE"})
    time.sleep(2)
time.sleep(1)