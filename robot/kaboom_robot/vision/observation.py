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


NO_FACE = FaceObservation(face_present=False)
