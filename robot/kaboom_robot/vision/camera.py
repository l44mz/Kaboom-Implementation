"""Reads frames from a USB webcam / laptop camera (OpenCV), the Pi Camera Module (picamera2),
or the Pi Camera Module through the rpicam-vid command (rpicam)."""

import subprocess

import cv2
import numpy as np


class Camera:
    """Gives BGR frames (OpenCV's color order) from any of the three camera types.

    source="opencv": laptop camera or USB webcam (works on Mac and on the Pi).
    source="picamera2": the ribbon-cable Pi Camera Module through picamera2 (needs the
    system Python, because picamera2 is installed with apt).
    source="rpicam": the ribbon-cable Pi Camera Module through the rpicam-vid command.
    Works with any Python version, e.g. a Python 3.12 venv for MediaPipe 0.10 on a Pi 4.
    Keep width a multiple of 64 (640 is fine) so the frames have no padding.
    """

    def __init__(self, source: str = "opencv", index: int = 0, width: int = 640, height: int = 480):
        self.source = source
        self.width = width
        self.height = height

        if source == "picamera2":
            from picamera2 import Picamera2  # only installed on the Pi

            self._cam = Picamera2()
            # "RGB888" in picamera2 already gives OpenCV's BGR byte order.
            config = self._cam.create_video_configuration(
                main={"size": (width, height), "format": "RGB888"}
            )
            self._cam.configure(config)
            self._cam.start()
        elif source == "rpicam":
            # One YUV420 frame = full-size Y plane + quarter-size U and V planes.
            self._frame_bytes = width * height * 3 // 2
            self._cam = subprocess.Popen(
                [
                    "rpicam-vid",
                    "-t", "0",              # run until we stop it
                    "-n",                   # no preview window
                    "--codec", "yuv420",    # raw frames, no compression
                    "--width", str(width),
                    "--height", str(height),
                    "--framerate", "15",
                    "-o", "-",              # write frames to stdout
                ],
                stdout=subprocess.PIPE,
                stderr=subprocess.DEVNULL,
                bufsize=self._frame_bytes,
            )
        elif source == "opencv":
            self._cam = cv2.VideoCapture(index)
            self._cam.set(cv2.CAP_PROP_FRAME_WIDTH, width)
            self._cam.set(cv2.CAP_PROP_FRAME_HEIGHT, height)
            if not self._cam.isOpened():
                raise RuntimeError(f"Cannot open camera {index}")
        else:
            raise ValueError(f"Unknown camera source: {source}")

    def read(self):
        """Return the next frame, or None if the camera stopped."""
        if self.source == "picamera2":
            return self._cam.capture_array()
        if self.source == "rpicam":
            return self._read_rpicam_frame()
        ok, frame = self._cam.read()
        return frame if ok else None

    def _read_rpicam_frame(self):
        """Read exactly one YUV420 frame from rpicam-vid and convert it to BGR."""
        data = bytearray()
        while len(data) < self._frame_bytes:
            chunk = self._cam.stdout.read(self._frame_bytes - len(data))
            if not chunk:  # rpicam-vid exited
                return None
            data.extend(chunk)
        yuv = np.frombuffer(bytes(data), dtype=np.uint8).reshape(
            self.height * 3 // 2, self.width
        )
        return cv2.cvtColor(yuv, cv2.COLOR_YUV2BGR_I420)

    def close(self) -> None:
        if self.source == "picamera2":
            self._cam.stop()
        elif self.source == "rpicam":
            self._cam.terminate()
            try:
                self._cam.wait(timeout=2)
            except subprocess.TimeoutExpired:
                self._cam.kill()
        else:
            self._cam.release()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
