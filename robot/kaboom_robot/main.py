from kaboom_robot.api.robot_socket import RobotSocket
from kaboom_robot.api.state_reporter import StateReporter
from kaboom_robot import detect_state          # your CV function

def on_server_message(msg):
    if "error" in msg:
        print("server error:", msg["error"])
    elif msg.get("robotExpression"):
        show_face(msg["robotExpression"])   # your screen code

socket = RobotSocket(f"ws://{HOST}:8080/api/v1/ws/robot?robotId={ROBOT_ID}", on_server_message)
socket.start()

# after the polling loop finds an active session:
reporter = StateReporter(socket, session_id)
while session_is_active():
    reporter.update(detect_state())      # returns "FOCUSED" / "DISTRACTED" / "AWAY"