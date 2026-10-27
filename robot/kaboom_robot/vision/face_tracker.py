"""Runs MediaPipe Face Landmarker on a frame and returns a FaceObservation.

Based on Google's Face Landmarker Raspberry Pi sample
(google-ai-edge/mediapipe-samples, Apache License 2.0), updated for MediaPipe 1.x,
which removed the old `mp.solutions` drawing helpers the sample used.
"""

from pathlib import Path

import cv2
import mediapipe as mp
from mediapipe.tasks.python import BaseOptions, vision

from kaboom_robot.vision import metrics
from kaboom_robot.vision.observation import NO_FACE, FaceObservation


class FaceTracker:
    """Wraps the Face Landmarker model. Use it in a `with` block so it closes."""

    def __init__(
        self,
        model_path: Path,
        min_detection_confidence: float = 0.5,
        min_presence_confidence: float = 0.5,
        min_tracking_confidence: float = 0.5,
    ):
        if not Path(model_path).is_file():
            raise FileNotFoundError(
                f"Model not found at {model_path}. "
                "Run: python -m kaboom_robot.vision.download_model"
            )
        options = vision.FaceLandmarkerOptions(
            base_options=BaseOptions(model_asset_path=str(model_path)),
            # VIDEO mode: we call it once per frame and wait for the answer.
            # Simpler than the sample's LIVE_STREAM callback, and fast enough on a Pi 4.
            running_mode=vision.RunningMode.VIDEO,
            num_faces=1,
            min_face_detection_confidence=min_detection_confidence,
            min_face_presence_confidence=min_presence_confidence,
            min_tracking_confidence=min_tracking_confidence,
            # Gives a 4x4 head pose matrix, so we get yaw/pitch/roll without extra math.
            output_facial_transformation_matrixes=True,
        )
        self._landmarker = vision.FaceLandmarker.create_from_options(options)
        self._last_timestamp_ms = -1

    def process(self, frame_bgr, timestamp_ms: int) -> FaceObservation:
        """Measure the face in one OpenCV (BGR) frame."""
        # VIDEO mode rejects timestamps that do not increase.
        timestamp_ms = max(timestamp_ms, self._last_timestamp_ms + 1)
        self._last_timestamp_ms = timestamp_ms

        height, width = frame_bgr.shape[:2]
        rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
        image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
        result = self._landmarker.detect_for_video(image, timestamp_ms)

        if not result.face_landmarks:
            return NO_FACE

        pixels = metrics.to_pixels(result.face_landmarks[0], width, height)
        yaw = pitch = roll = None
        if result.facial_transformation_matrixes:
            yaw, pitch, roll = metrics.head_angles(result.facial_transformation_matrixes[0])

        return FaceObservation(
            face_present=True,
            ear=metrics.mean_ear(pixels),
            gaze=metrics.mean_gaze(pixels),
            yaw=yaw,
            pitch=pitch,
            roll=roll,
        )

    def close(self) -> None:
        self._landmarker.close()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
