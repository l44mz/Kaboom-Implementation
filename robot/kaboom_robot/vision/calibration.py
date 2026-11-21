"""Learns the user's normal working pose at the start of a session.

The robot sits beside the laptop, so a focused user is NOT facing the camera:
their head is already turned a bit. We record that "working pose" for a few
seconds and then measure every frame relative to it.
"""

from dataclasses import replace
from statistics import median

from kaboom_robot.vision.observation import FaceObservation


class Baseline:
    def __init__(self, seconds: float = 3.0, min_samples: int = 10):
        self.seconds = seconds
        self.min_samples = min_samples
        self._start = None
        self._samples: list[FaceObservation] = []
        self.yaw = self.pitch = self.gaze = 0.0
        self.ready = False

    def add(self, t: float, obs: FaceObservation) -> None:
        """Feed frames until ready. Frames without a face are skipped."""
        if self.ready or not obs.face_present or obs.yaw is None:
            return
        if self._start is None:
            self._start = t
        self._samples.append(obs)
        if t - self._start >= self.seconds and len(self._samples) >= self.min_samples:
            self.yaw = median(s.yaw for s in self._samples)
            self.pitch = median(s.pitch for s in self._samples)
            gazes = [s.gaze for s in self._samples if s.gaze is not None]
            self.gaze = median(gazes) if gazes else 0.0
            self.ready = True

    def apply(self, obs: FaceObservation) -> FaceObservation:
        """Return the same frame with yaw, pitch and gaze measured from the working pose."""
        if not obs.face_present:
            return obs
        return replace(
            obs,
            yaw=None if obs.yaw is None else obs.yaw - self.yaw,
            pitch=None if obs.pitch is None else obs.pitch - self.pitch,
            gaze=None if obs.gaze is None else obs.gaze - self.gaze,
        )
