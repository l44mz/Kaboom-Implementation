"""Learns the user's normal working pose at the start of a session.

The robot sits beside the laptop, so a focused user is NOT facing the camera:
their head is already turned a bit. We record that "working pose" for a few
seconds and then measure every frame relative to it.

It also learns how open this user's eyes normally are, so "eyes closed" is judged
against their own eyes, and it keeps adjusting slowly while the user is focused,
so leaning back or moving the chair later does not count as a distraction.
"""

import math
from dataclasses import replace
from statistics import median

from kaboom_robot.vision.observation import FaceObservation


class Baseline:
    def __init__(self, seconds: float = 3.0, min_samples: int = 10, adapt_s: float = 60.0):
        self.seconds = seconds
        self.min_samples = min_samples
        # How slowly the pose follows the user while focused: after about adapt_s
        # seconds in a new position, most of the change has been learned.
        self.adapt_s = adapt_s
        self._start = None
        self._last_adapt = None
        self._samples: list[FaceObservation] = []
        self.yaw = self.pitch = self.roll = self.gaze = 0.0
        self.gaze_x = self.gaze_y = 0.0
        self.ear_open: float | None = None  # this user's normal EAR with open eyes
        self.ready = False

    def add(self, t: float, obs: FaceObservation) -> None:
        """Feed frames until ready. Frames without a face are skipped."""
        if self.ready or not obs.face_present or obs.yaw is None:
            return
        if self._start is None:
            self._start = t
        self._samples.append(obs)
        if t - self._start >= self.seconds and len(self._samples) >= self.min_samples:
            s = self._samples
            self.yaw = median(o.yaw for o in s)
            self.pitch = median(o.pitch for o in s)
            self.roll = _median_of(s, "roll")
            self.gaze = _median_of(s, "gaze")
            self.gaze_x = _median_of(s, "gaze_x")
            self.gaze_y = _median_of(s, "gaze_y")
            ears = [o.ear for o in s if o.ear is not None]
            # The median skips blinks during calibration.
            self.ear_open = median(ears) if ears else None
            self._last_adapt = t
            self.ready = True

    def apply(self, obs: FaceObservation) -> FaceObservation:
        """Return the same frame measured from the working pose."""
        if not obs.face_present:
            return obs
        if obs.gaze_x is not None and obs.gaze_y is not None:
            dx, dy = obs.gaze_x - self.gaze_x, obs.gaze_y - self.gaze_y
            gaze = math.hypot(dx, dy)  # how far the eyes moved from where they normally look
        else:
            dx = dy = None
            gaze = None if obs.gaze is None else obs.gaze - self.gaze
        ear_ratio = None
        if obs.ear is not None and self.ear_open:
            ear_ratio = obs.ear / self.ear_open
        return replace(
            obs,
            yaw=_minus(obs.yaw, self.yaw),
            pitch=_minus(obs.pitch, self.pitch),
            roll=_minus(obs.roll, self.roll),
            gaze=gaze,
            gaze_x=dx,
            gaze_y=dy,
            ear_ratio=ear_ratio,
        )

    def adapt(self, t: float, obs: FaceObservation, focused: bool, max_change_deg: float = 15.0):
        """Follow slow posture changes while the user is focused.

        `obs` is the raw frame (before apply). Only frames close to the current pose
        are used, so a real distraction is never learned as the new normal.
        """
        if not self.ready or self._last_adapt is None:
            return
        dt = max(t - self._last_adapt, 0.0)
        self._last_adapt = t
        if not focused or not obs.face_present or obs.yaw is None or obs.pitch is None:
            return
        if abs(obs.yaw - self.yaw) > max_change_deg or abs(obs.pitch - self.pitch) > max_change_deg:
            return
        a = 1.0 - math.exp(-dt / self.adapt_s) if self.adapt_s > 0 else 0.0
        self.yaw += a * (obs.yaw - self.yaw)
        self.pitch += a * (obs.pitch - self.pitch)
        if obs.roll is not None:
            self.roll += a * (obs.roll - self.roll)
        if obs.gaze_x is not None and obs.gaze_y is not None:
            self.gaze_x += a * (obs.gaze_x - self.gaze_x)
            self.gaze_y += a * (obs.gaze_y - self.gaze_y)


def _median_of(samples: list[FaceObservation], name: str) -> float:
    values = [getattr(o, name) for o in samples if getattr(o, name) is not None]
    return median(values) if values else 0.0


def _minus(value: float | None, base: float) -> float | None:
    return None if value is None else value - base
