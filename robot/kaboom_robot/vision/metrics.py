"""Pure math for focus detection: eye openness, gaze, and head angles.

No camera and no MediaPipe here, only numpy, so every function is easy to test.

The eye aspect ratio (EAR) and gaze score are adapted from
e-candeloro/Driver-State-Detection (MIT License, Copyright (c) 2021 Ettore Candeloro).
"""

import math

import numpy as np

# MediaPipe Face Landmarker point numbers for each eye, in this order:
# outer corner, inner corner, upper lid 1, lower lid 1, upper lid 2, lower lid 2.
LEFT_EYE = (33, 133, 160, 144, 158, 153)
RIGHT_EYE = (362, 263, 385, 380, 387, 373)

# Center of each iris (the model returns 478 points; 468 to 477 are the irises).
LEFT_IRIS = 468
RIGHT_IRIS = 473


def to_pixels(landmarks, width: int, height: int) -> np.ndarray:
    """Turn MediaPipe's 0..1 landmark coordinates into an (N, 2) array of pixels.

    Pixels matter: in 0..1 units a 640x480 frame squashes distances differently
    on x and y, which would bend the eye measurements.
    """
    return np.array([[lm.x * width, lm.y * height] for lm in landmarks], dtype=float)


def eye_aspect_ratio(points: np.ndarray) -> float | None:
    """EAR for one eye: lid opening divided by eye width.

    About 0.25 to 0.35 when the eye is open, under about 0.15 when it is closed.
    Returns None if the eye has no width (a broken detection).
    """
    width = np.linalg.norm(points[0] - points[1])
    if width <= 1e-6:
        return None
    opening = np.linalg.norm(points[2] - points[3]) + np.linalg.norm(points[4] - points[5])
    return float(opening / (2 * width))


def mean_ear(pixels: np.ndarray) -> float | None:
    """Average EAR of both eyes, skipping an eye that could not be measured."""
    scores = [eye_aspect_ratio(pixels[list(eye)]) for eye in (LEFT_EYE, RIGHT_EYE)]
    scores = [s for s in scores if s is not None]
    return float(np.mean(scores)) if scores else None


def gaze_score(pixels: np.ndarray, eye: tuple, iris: int) -> float | None:
    """How far the iris sits from the eye's center, divided by the eye width.

    Near 0 when looking straight ahead, bigger when the eyes look to the side.
    """
    points = pixels[list(eye)]
    x_min, y_min = points.min(axis=0)
    x_max, y_max = points.max(axis=0)
    width = x_max - x_min
    if width <= 1e-6:
        return None
    center = np.array([(x_min + x_max) / 2, (y_min + y_max) / 2])
    return float(np.linalg.norm(pixels[iris] - center) / width)


def mean_gaze(pixels: np.ndarray) -> float | None:
    """Average gaze score of both eyes."""
    scores = [
        gaze_score(pixels, LEFT_EYE, LEFT_IRIS),
        gaze_score(pixels, RIGHT_EYE, RIGHT_IRIS),
    ]
    scores = [s for s in scores if s is not None]
    return float(np.mean(scores)) if scores else None


def head_angles(matrix) -> tuple[float, float, float]:
    """Read (yaw, pitch, roll) in degrees from MediaPipe's 4x4 face transformation matrix.

    yaw: turning left or right. pitch: nodding up or down. roll: tilting to a shoulder.
    All three are 0 when the face points straight at the camera.
    """
    r = np.asarray(matrix, dtype=float).reshape(4, 4)[:3, :3]
    pitch = math.atan2(r[2, 1], r[2, 2])
    yaw = math.atan2(-r[2, 0], math.hypot(r[0, 0], r[1, 0]))
    roll = math.atan2(r[1, 0], r[0, 0])
    return math.degrees(yaw), math.degrees(pitch), math.degrees(roll)
