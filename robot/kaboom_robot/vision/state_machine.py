"""Turns frame-by-frame measurements into FOCUSED, DISTRACTED or AWAY.

A single frame is never trusted on its own: a blink, a quick glance, or one
frame where MediaPipe loses the face must not change the state. Each signal
has to stay true for a number of seconds first. The idea of time-based
thresholds comes from e-candeloro/Driver-State-Detection (MIT License).

Time is passed in by the caller, so tests can use fake times instead of waiting.
"""

import math
from dataclasses import dataclass, replace
from enum import Enum

from kaboom_robot.vision.observation import FaceObservation


class FocusState(str, Enum):
    FOCUSED = "FOCUSED"
    DISTRACTED = "DISTRACTED"
    AWAY = "AWAY"


@dataclass(frozen=True)
class Thresholds:
    """All numbers that decide the state. Tuned in Sprint 4 with real users."""

    ear_closed: float = 0.15  # EAR below this = eyes closed (used before calibration)
    # After calibration: eyes count as closed under this share of the user's own
    # open-eye EAR (0.6 = 60%), so narrower eyes or glasses are judged fairly.
    ear_closed_ratio: float = 0.6
    gaze_off: float = 0.20  # gaze change above this = eyes looking elsewhere
    smoothing_s: float = 0.15  # averages the numbers over about this long, so they don't flicker
    yaw_deg: float = 30.0  # head turned left/right more than this
    pitch_deg: float = 20.0  # head up/down (for example, looking at a phone)
    roll_deg: float = 30.0  # head tilted to a shoulder
    eyes_closed_after_s: float = 3.0
    distracted_after_s: float = 5.0
    away_after_s: float = 3.0
    recover_after_s: float = 1.0  # how long all must look fine before FOCUSED again
    grace_s: float = 0.5  # gaps shorter than this do not break a streak


class Streak:
    """Measures for how long a yes/no signal has been continuously true.

    Short "no" gaps (under grace_s) are ignored, so a flickering detection
    does not restart the clock.
    """

    def __init__(self, grace_s: float):
        self.grace_s = grace_s
        self._start = None
        self._last_true = None

    def update(self, t: float, value: bool | None) -> None:
        if value is None:  # unknown this frame: keep what we had
            return
        if value:
            if self._start is None:
                self._start = t
            self._last_true = t
        elif self._start is not None and t - self._last_true > self.grace_s:
            self.reset()

    def reset(self) -> None:
        self._start = None
        self._last_true = None

    def duration(self, t: float) -> float:
        return 0.0 if self._start is None else t - self._start


class Smoother:
    """Averages each number over the last moments (exponential moving average).

    Camera numbers jitter a little every frame; near a threshold that would make the
    head "turn" and "un-turn" many times a second. Smoothing keeps them steady.
    It restarts when the face is lost, so a returning user is measured fresh.
    """

    FIELDS = ("ear", "ear_ratio", "gaze", "gaze_x", "gaze_y", "yaw", "pitch", "roll")

    def __init__(self, time_constant_s: float):
        self.tau = time_constant_s
        self._values: dict[str, float] = {}
        self._last_t = None

    def update(self, t: float, obs: FaceObservation) -> FaceObservation:
        if not obs.face_present or self.tau <= 0:
            self.reset()
            return obs
        a = 1.0 if self._last_t is None else 1.0 - math.exp(-(t - self._last_t) / self.tau)
        self._last_t = t
        changes = {}
        for name in self.FIELDS:
            value = getattr(obs, name)
            if value is None:
                continue  # unknown this frame: leave it unknown
            old = self._values.get(name)
            new = value if old is None else old + a * (value - old)
            self._values[name] = new
            changes[name] = new
        return replace(obs, **changes)

    def reset(self) -> None:
        self._values.clear()
        self._last_t = None


class FocusStateMachine:
    def __init__(self, thresholds: Thresholds | None = None):
        self.th = thresholds or Thresholds()
        self._smoother = Smoother(self.th.smoothing_s)
        g = self.th.grace_s
        self._no_face = Streak(g)
        self._eyes_closed = Streak(g)
        self._head_turned = Streak(g)
        self._gaze_off = Streak(g)
        self._focused_candidate = Streak(0.0)
        self.state = FocusState.FOCUSED
        self.reasons: list[str] = []  # why we are not FOCUSED, for the debug window

    def update(self, t: float, obs: FaceObservation) -> FocusState:
        """Feed one frame (yaw/pitch/gaze already relative to the Baseline). Returns the state."""
        th = self.th
        obs = self._smoother.update(t, obs)
        self._no_face.update(t, not obs.face_present)

        if obs.face_present:
            self._eyes_closed.update(t, _eyes_closed(obs, th))
            self._gaze_off.update(t, _abs_above(obs.gaze, th.gaze_off))
            self._head_turned.update(t, _head_turned(obs, th))
        else:
            # No face means no eyes or head to judge; start those clocks fresh later.
            self._eyes_closed.reset()
            self._gaze_off.reset()
            self._head_turned.reset()

        candidate = self._candidate(t)

        if not obs.face_present:
            # We cannot see the user, so we cannot say they look fine again.
            # Keep the current state until the AWAY timer decides.
            self._focused_candidate.reset()
            if candidate == FocusState.AWAY:
                self.state = FocusState.AWAY
            return self.state

        self._focused_candidate.update(t, candidate == FocusState.FOCUSED)
        if candidate != FocusState.FOCUSED:
            self.state = candidate
        elif self._focused_candidate.duration(t) >= th.recover_after_s:
            self.state = FocusState.FOCUSED
        return self.state

    def _candidate(self, t: float) -> FocusState:
        th = self.th
        self.reasons = []
        if self._no_face.duration(t) >= th.away_after_s:
            self.reasons.append("no face")
            return FocusState.AWAY
        if self._eyes_closed.duration(t) >= th.eyes_closed_after_s:
            self.reasons.append("eyes closed")
        if self._head_turned.duration(t) >= th.distracted_after_s:
            self.reasons.append("head turned")
        if self._gaze_off.duration(t) >= th.distracted_after_s:
            self.reasons.append("looking away")
        return FocusState.DISTRACTED if self.reasons else FocusState.FOCUSED


def _below(value: float | None, limit: float) -> bool | None:
    return None if value is None else value < limit


def _eyes_closed(obs: FaceObservation, th: Thresholds) -> bool | None:
    # After calibration we know this user's open eyes, so compare with those.
    if obs.ear_ratio is not None:
        return obs.ear_ratio < th.ear_closed_ratio
    return _below(obs.ear, th.ear_closed)


def _abs_above(value: float | None, limit: float) -> bool | None:
    return None if value is None else abs(value) > limit


def _head_turned(obs: FaceObservation, th: Thresholds) -> bool | None:
    if obs.yaw is None or obs.pitch is None or obs.roll is None:
        return None
    return abs(obs.yaw) > th.yaw_deg or abs(obs.pitch) > th.pitch_deg or abs(obs.roll) > th.roll_deg
