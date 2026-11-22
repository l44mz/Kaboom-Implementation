import math

import numpy as np
import pytest

from kaboom_robot.vision import metrics


def _rotation(yaw, pitch, roll):
    """Build a 4x4 matrix from angles in degrees (roll, then yaw, then pitch)."""
    y, p, r = (math.radians(a) for a in (yaw, pitch, roll))
    rx = np.array([[1, 0, 0], [0, math.cos(p), -math.sin(p)], [0, math.sin(p), math.cos(p)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(r), -math.sin(r), 0], [math.sin(r), math.cos(r), 0], [0, 0, 1]])
    m = np.eye(4)
    m[:3, :3] = rz @ ry @ rx
    return m


def _eye(opening):
    """Six eye points: corners 30 px apart, lids `opening` px apart."""
    return np.array(
        [
            [0, 0],
            [30, 0],
            [10, -opening / 2],
            [10, opening / 2],
            [20, -opening / 2],
            [20, opening / 2],
        ],
        dtype=float,
    )


def test_ear_open_eye_is_bigger_than_closed_eye():
    assert metrics.eye_aspect_ratio(_eye(9)) == pytest.approx(0.30)
    assert metrics.eye_aspect_ratio(_eye(2)) < 0.15


def test_ear_returns_none_for_a_collapsed_eye():
    assert metrics.eye_aspect_ratio(np.zeros((6, 2))) is None


def test_mean_ear_uses_both_eyes():
    pixels = np.zeros((478, 2))
    pixels[list(metrics.LEFT_EYE)] = _eye(9)
    pixels[list(metrics.RIGHT_EYE)] = _eye(3) + 100
    assert metrics.mean_ear(pixels) == pytest.approx((0.30 + 0.10) / 2)


def test_gaze_is_zero_when_iris_is_centered_and_grows_when_it_moves():
    pixels = np.zeros((478, 2))
    pixels[list(metrics.LEFT_EYE)] = _eye(9)
    pixels[list(metrics.RIGHT_EYE)] = _eye(9)
    pixels[metrics.LEFT_IRIS] = pixels[metrics.RIGHT_IRIS] = [15, 0]
    assert metrics.mean_gaze(pixels) == pytest.approx(0.0)
    pixels[metrics.LEFT_IRIS] = pixels[metrics.RIGHT_IRIS] = [24, 0]
    assert metrics.mean_gaze(pixels) == pytest.approx(0.30)


def test_head_angles_are_zero_when_facing_the_camera():
    assert metrics.head_angles(np.eye(4)) == pytest.approx((0.0, 0.0, 0.0))


@pytest.mark.parametrize("yaw, pitch, roll", [(35, 0, 0), (0, -25, 0), (0, 0, 15), (20, 10, -5)])
def test_head_angles_read_back_the_angles_used_to_build_the_matrix(yaw, pitch, roll):
    assert metrics.head_angles(_rotation(yaw, pitch, roll)) == pytest.approx((yaw, pitch, roll))


def test_to_pixels_scales_by_frame_size():
    class Lm:
        def __init__(self, x, y):
            self.x, self.y = x, y

    assert metrics.to_pixels([Lm(0.5, 0.25)], 640, 480).tolist() == [[320.0, 120.0]]
