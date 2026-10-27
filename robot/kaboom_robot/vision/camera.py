"""Reads frames from a USB webcam / laptop camera (OpenCV) or the Pi Camera Module (picamera2)."""

import cv2


class Camera:
    """Gives BGR frames (OpenCV's color order) from either camera type.

    source="opencv": laptop camera or USB webcam (works on Mac and on the Pi).
    source="picamera2": the ribbon-cable Pi Camera Module, which OpenCV cannot open
    on current Raspberry Pi OS.
    """

    def __init__(self, source: str = "opencv", index: int = 0, width: int = 640, height: int = 480):
        self.source = source
        if source == "picamera2":
            from picamera2 import Picamera2  # only installed on the Pi

            self._cam = Picamera2()
            # "RGB888" in picamera2 already gives OpenCV's BGR byte order.
            config = self._cam.create_video_configuration(
                main={"size": (width, height), "format": "RGB888"}
            )
            self._cam.configure(config)
            self._cam.start()
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
        ok, frame = self._cam.read()
        return frame if ok else None

    def close(self) -> None:
        if self.source == "picamera2":
            self._cam.stop()
        else:
            self._cam.release()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
