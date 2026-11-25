"""What the camera saw in one frame, as plain numbers."""

from dataclasses import dataclass


@dataclass(frozen=True)
class FaceObservation:
    """One frame's measurements. None means "could not measure"."""

    face_present: bool
    ear: float | None = None
    gaze: float | None = None
    yaw: float | None = None
    pitch: float | None = None
    roll: float | None = None
    # Where the irises point, split into left/right (x) and up/down (y), in eye widths.
    # `gaze` above is the length of this arrow.
    gaze_x: float | None = None
    gaze_y: float | None = None
    # Eye openness compared with this user's own open eyes (1.0 = normal).
    # Set by the Baseline, so people with narrower eyes are judged fairly.
    ear_ratio: float | None = None


NO_FACE = FaceObservation(face_present=False)
