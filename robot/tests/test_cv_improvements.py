"""Tests for the detection improvements: gaze direction, largest face, per-person eyes,
smoothing, roll calibration and slow re-calibration. Fake numbers, no camera.
"""

import numpy as np
import pytest

from kaboom_robot.vision import metrics
from kaboom_robot.vision.calibration import Baseline
from kaboom_robot.vision.observation import NO_FACE, FaceObservation
from kaboom_robot.vision.state_machine import FocusState, FocusStateMachine, Thresholds

FPS = 10


def run(machine, frames, start=0.0):
    """Feed a list of observations at FPS; return the last state."""
    t = start
    for obs in frames:
        t += 1 / FPS
        machine.update(t, obs)
    return machine.state, t


def _eye_points():
    return np.array([[0, 0], [30, 0], [10, -4], [10, 4], [20, -4], [20, 4]], dtype=float)


# --- metrics ---------------------------------------------------------------------------


def test_gaze_vector_keeps_the_direction():
    pixels = np.zeros((478, 2))
    pixels[list(metrics.LEFT_EYE)] = _eye_points()
    pixels[list(metrics.RIGHT_EYE)] = _eye_points()
    pixels[metrics.LEFT_IRIS] = pixels[metrics.RIGHT_IRIS] = [24, 0]  # looking right
    x, y = metrics.mean_gaze_vector(pixels)
    assert x == pytest.approx(0.30)
    assert y == pytest.approx(0.0)
    pixels[metrics.LEFT_IRIS] = pixels[metrics.RIGHT_IRIS] = [6, 0]  # looking left
    x, _ = metrics.mean_gaze_vector(pixels)
    assert x == pytest.approx(-0.30)


def test_largest_face_picks_the_closest_person():
    far = np.array([[100, 100], [130, 140]], dtype=float)  # small, behind the user
    near = np.array([[200, 50], [400, 330]], dtype=float)  # big, the user
    assert metrics.largest_face([far, near]) == 1
    assert metrics.largest_face([near, far]) == 0
    assert metrics.largest_face([]) is None


# --- calibration -----------------------------------------------------------------------


def _calibrated(**normal):
    """A Baseline that learned `normal` (yaw, pitch, roll, ear, gaze_x, gaze_y)."""
    values = dict(yaw=20.0, pitch=-10.0, roll=8.0, ear=0.18, gaze_x=0.1, gaze_y=0.0)
    values.update(normal)
    b = Baseline(seconds=3, min_samples=10)
    for i in range(40):
        b.add(i / 10, FaceObservation(True, gaze=0.1, **values))
    assert b.ready
    return b, values


def test_baseline_learns_roll_gaze_direction_and_eye_openness():
    b, normal = _calibrated()
    adjusted = b.apply(FaceObservation(True, gaze=0.1, **normal))
    assert adjusted.roll == pytest.approx(0.0)
    assert adjusted.gaze == pytest.approx(0.0)
    assert adjusted.ear_ratio == pytest.approx(1.0)


def test_gaze_is_measured_from_where_the_user_normally_looks():
    b, normal = _calibrated(gaze_x=0.1)
    # The user normally looks a bit right (toward the laptop): that is 0, not "looking away".
    looking_further = dict(normal, gaze_x=0.4)
    adjusted = b.apply(FaceObservation(True, gaze=0.4, **looking_further))
    assert adjusted.gaze == pytest.approx(0.3)


def test_tilted_head_user_is_not_distracted_after_calibration():
    b, normal = _calibrated(roll=35.0)  # works with the head tilted more than roll_deg
    m = FocusStateMachine()
    obs = b.apply(FaceObservation(True, gaze=0.1, **normal))
    state, _ = run(m, [obs] * 100)
    assert state == FocusState.FOCUSED


def test_slow_posture_change_is_learned_while_focused():
    b, normal = _calibrated(yaw=20.0)
    t = 4.0
    for _ in range(600):  # 60 s leaning 10 degrees further, still working
        t += 0.1
        b.adapt(t, FaceObservation(True, **dict(normal, yaw=30.0)), focused=True)
    assert b.yaw == pytest.approx(30.0, abs=4.0)


def test_distraction_is_not_learned_as_the_new_normal():
    b, normal = _calibrated(yaw=20.0)
    t = 4.0
    for _ in range(600):
        t += 0.1
        b.adapt(t, FaceObservation(True, **dict(normal, yaw=30.0)), focused=False)
        b.adapt(t, FaceObservation(True, **dict(normal, yaw=70.0)), focused=True)  # big jump
    assert b.yaw == pytest.approx(20.0, abs=0.5)


# --- state machine ---------------------------------------------------------------------


def test_narrow_eyes_are_not_mistaken_for_closed_eyes():
    b, normal = _calibrated(ear=0.14)  # under the old fixed 0.15 limit, with eyes open
    m = FocusStateMachine()
    obs = b.apply(FaceObservation(True, gaze=0.1, **normal))
    state, _ = run(m, [obs] * 60)
    assert state == FocusState.FOCUSED


def test_closing_narrow_eyes_is_still_noticed():
    b, normal = _calibrated(ear=0.14)
    m = FocusStateMachine()
    closed = b.apply(FaceObservation(True, gaze=0.1, **dict(normal, ear=0.04)))
    state, _ = run(m, [closed] * 35)
    assert state == FocusState.DISTRACTED
    assert "eyes closed" in m.reasons


def _jittery_head(frames):
    """Head at 27 degrees, but the measurement jumps to 31 every third frame."""
    return [
        FaceObservation(
            True, ear=0.3, gaze=0.0, yaw=31.0 if i % 3 == 0 else 27.0, pitch=0.0, roll=0.0
        )
        for i in range(frames)
    ]


def test_without_smoothing_jitter_near_the_limit_looks_like_a_distraction():
    m = FocusStateMachine(Thresholds(smoothing_s=0))
    state, _ = run(m, _jittery_head(80))
    assert state == FocusState.DISTRACTED


def test_smoothing_ignores_jitter_near_the_limit():
    m = FocusStateMachine()
    state, _ = run(m, _jittery_head(80))
    assert state == FocusState.FOCUSED


def test_smoothing_restarts_after_the_face_is_lost():
    m = FocusStateMachine()
    turned = FaceObservation(True, ear=0.3, gaze=0.0, yaw=50.0, pitch=0.0, roll=0.0)
    fine = FaceObservation(True, ear=0.3, gaze=0.0, yaw=0.0, pitch=0.0, roll=0.0)
    _, t = run(m, [turned] * 20)
    _, t = run(m, [NO_FACE] * 5, t)
    # The first frame back is taken as it is, not averaged with the old turned head.
    smoothed = m._smoother.update(t + 0.1, fine)
    assert smoothed.yaw == pytest.approx(0.0)
