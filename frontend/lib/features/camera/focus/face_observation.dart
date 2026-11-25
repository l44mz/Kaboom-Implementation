/// What the camera measured in one frame, after ML Kit found (or didn't find) a face.
///
/// The same idea as the robot's FaceObservation (robot/kaboom_robot/vision/observation.py),
/// but with the numbers ML Kit gives on the phone. ML Kit has no gaze direction,
/// so the phone judges the head and the eyes only.
class FaceObservation {
  const FaceObservation({
    required this.facePresent,
    this.yaw,
    this.pitch,
    this.roll,
    this.eyeOpen,
    this.eyeOpenRatio,
  });

  /// A frame where no face was found.
  const FaceObservation.noFace() : this(facePresent: false);

  final bool facePresent;

  /// Head turned left or right, in degrees (ML Kit headEulerAngleY).
  final double? yaw;

  /// Head up or down, in degrees (ML Kit headEulerAngleX). Looking down at a desk is negative.
  final double? pitch;

  /// Head tilted to a shoulder, in degrees (ML Kit headEulerAngleZ).
  final double? roll;

  /// How open the eyes are, 0 (closed) to 1 (open): the average of both eyes
  /// (ML Kit leftEyeOpenProbability and rightEyeOpenProbability).
  final double? eyeOpen;

  /// [eyeOpen] compared with this user's own open eyes from calibration
  /// (1.0 = as open as normal). Null before calibration.
  final double? eyeOpenRatio;
}