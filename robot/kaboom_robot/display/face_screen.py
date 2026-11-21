"""Runs Sparky's animated face on the screen in a background thread.

Other code sets `face.expression` ("focused", "distracted", "away", "happy" or "exploded");
the thread keeps drawing and sending frames, so the face stays alive (blinking,
looking around) even while the camera or the network is busy.
"""

import threading
import time

from kaboom_robot.display.face import EXPRESSIONS, FaceAnimator


class FaceScreen(threading.Thread):
    def __init__(
        self,
        fps: float = 12.0,
        smooth: int = 1,
        rotation: str = "landscape",
        swap_red_blue: bool = False,
        invert_colors: bool = False,
        rs_invert: bool = False,
        delay: int = 1,
    ):
        super().__init__(daemon=True)
        # Imported here so the robot still runs on a computer without the screen library.
        from kaboom_robot.display.ili9486 import Ili9486

        self._screen = Ili9486(
            rotation=rotation,
            swap_red_blue=swap_red_blue,
            invert_colors=invert_colors,
            rs_invert=rs_invert,
            delay=delay,
        )
        self._animator = FaceAnimator("happy", supersample=smooth)
        self._fps = fps
        self._stop_event = threading.Event()
        self.expression = "happy"

    def set(self, expression: str) -> None:
        if expression in EXPRESSIONS:
            self.expression = expression

    def run(self) -> None:
        try:
            while not self._stop_event.is_set():
                now = time.monotonic()
                self._animator.set_expression(self.expression)
                self._screen.show(self._animator.frame(now))
                time.sleep(max(0.0, 1 / self._fps - (time.monotonic() - now)))
        except Exception as e:
            print(f"Face screen stopped: {e}")

    def stop(self) -> None:
        self._stop_event.set()
        self.join(timeout=2)
        self._screen.close()


def start_face_screen(**options):
    """Starts the face, or returns None (with a message) if the screen can't be used."""
    try:
        face = FaceScreen(**options)
    except Exception as e:
        print(f"No face on the screen ({e}). The robot keeps running without it.")
        return None
    face.start()
    return face
