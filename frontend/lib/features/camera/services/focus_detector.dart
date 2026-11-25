import 'package:flutter/widgets.dart';

import '../../session/models/focus_state.dart';
import '../focus/face_observation.dart';
// The phone version (ML Kit) in the Android and iOS apps, the browser version
// (MediaPipe) on the website. Dart picks one when the app is built.
import 'phone_focus_detector.dart' if (dart.library.js_interop) 'web_focus_detector.dart'
    as platform;

/// One result from the camera: the state, and the numbers behind it (for the test screen).
class FocusReading {
  const FocusReading({
    required this.state,
    required this.observation,
    required this.reasons,
    required this.calibrated,
  });

  final FocusState state;
  final FaceObservation observation;
  final List<String> reasons;
  final bool calibrated;
}

/// Watches the user with the front camera and decides FOCUSED, DISTRACTED or AWAY.
/// The screens only use this, so they work the same on a phone and on the website.
abstract class FocusDetector {
  /// The detector for where the app is running (phone or website).
  factory FocusDetector() => platform.createFocusDetector();

  /// A new reading after every checked frame.
  Stream<FocusReading> get readings;

  /// Only the state, and only when it changes.
  Stream<FocusState> get states;

  /// What went wrong, if frames can't be checked (shown on the test screen).
  /// Null while everything works.
  ValueNotifier<String?> get problem;

  /// Frames the camera has sent so far (to tell "no frames" from "frames rejected").
  int get framesReceived;

  /// True while the camera is open and its preview can be shown.
  bool get isRunning;

  /// Width / height of the preview as it is shown (portrait on a phone).
  double get previewAspectRatio;

  /// The live camera picture.
  Widget buildPreview();

  /// Opens the front camera and starts checking frames. The phone or browser asks
  /// for the camera permission the first time. Throws a CameraException
  /// ('CameraAccessDenied', 'noCamera', ...) if it can't.
  Future<void> start();

  /// Stops the camera (for example when the session is paused or ends).
  Future<void> stop();

  /// Stops everything. The detector can't be used again after this.
  Future<void> dispose();
}
