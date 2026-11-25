import 'dart:async';
import 'dart:js_interop';
import 'dart:ui_web' as ui_web;

import 'package:camera/camera.dart' show CameraException;
import 'package:flutter/widgets.dart';
import 'package:web/web.dart' as web;

import '../../session/models/focus_state.dart';
import '../focus/face_observation.dart';
import '../focus/focus_rules.dart';
import 'focus_detector.dart';

/// Used by [FocusDetector] on the website.
FocusDetector createFocusDetector() => WebFocusDetector();

// --- web/kaboom_face.js (MediaPipe in the browser) -------------------------------------

@JS('kaboomFace')
external _KaboomFace? get _kaboomFace;

extension type _KaboomFace._(JSObject _) implements JSObject {
  external JSPromise<JSAny?> load();
  external _FaceResult? detect(web.HTMLVideoElement video);
}

extension type _FaceResult._(JSObject _) implements JSObject {
  external bool get present;
  external double? get yaw;
  external double? get pitch;
  external double? get roll;
  external double? get eyeOpen;
}

// ---------------------------------------------------------------------------------------

/// Camera mode on the website: the computer's (or phone browser's) front camera,
/// with MediaPipe finding the face. Same steps and rules as [PhoneFocusDetector]:
///
/// 1. The browser camera plays in a <video> (the browser asks "Allow camera?" once).
/// 2. About 5 times a second, kaboom_face.js finds the face: head angles and open eyes.
/// 3. FocusRules (the same rules as Sparky) turns that into a state.
///
/// Everything runs in the browser. No picture leaves the computer.
class WebFocusDetector implements FocusDetector {
  WebFocusDetector({FocusRules? rules}) : rules = rules ?? FocusRules();

  final FocusRules rules;

  static int _nextId = 0;
  final String _viewType = 'kaboom-camera-${_nextId++}';
  bool _viewRegistered = false;

  final _readings = StreamController<FocusReading>.broadcast();
  final _clock = Stopwatch();

  web.HTMLVideoElement? _video;
  web.MediaStream? _stream;
  Timer? _timer;

  /// Check a frame at most this often (5 a second is plenty for a 1-second rule).
  static const _frameEveryMs = 200;

  @override
  final problem = ValueNotifier<String?>(null);

  @override
  int framesReceived = 0;

  @override
  Stream<FocusReading> get readings => _readings.stream;

  @override
  Stream<FocusState> get states => readings.map((r) => r.state).distinct();

  @override
  bool get isRunning => _stream != null;

  @override
  double get previewAspectRatio {
    final video = _video;
    if (video == null || video.videoWidth == 0 || video.videoHeight == 0) return 4 / 3;
    return video.videoWidth / video.videoHeight;
  }

  @override
  Widget buildPreview() => _video == null ? const SizedBox.shrink() : HtmlElementView(viewType: _viewType);

  @override
  Future<void> start() async {
    final face = _kaboomFace;
    if (face == null) {
      throw CameraException('noFaceDetector', 'kaboom_face.js is not loaded.');
    }

    // 1. The camera. The browser asks the user the first time.
    final web.MediaStream stream;
    try {
      stream = await web.window.navigator.mediaDevices
          .getUserMedia(web.MediaStreamConstraints(
            video: {
              'facingMode': 'user',
              'width': {'ideal': 640},
              'height': {'ideal': 480},
            }.jsify()!,
            audio: false.toJS,
          ))
          .toDart;
    } catch (e) {
      final text = e.toString();
      if (text.contains('NotAllowedError') || text.contains('Permission')) {
        throw CameraException('CameraAccessDenied', text);
      }
      if (text.contains('NotFoundError') || text.contains('OverconstrainedError')) {
        throw CameraException('noCamera', 'No camera found.');
      }
      throw CameraException('cameraError', text);
    }

    // 2. The face detector (downloads MediaPipe and the model the first time).
    try {
      await face.load().toDart;
    } catch (e) {
      for (final track in stream.getTracks().toDart) {
        track.stop();
      }
      throw CameraException('noFaceDetector', 'Could not load face detection: $e');
    }

    final video = (_video ??= _makeVideo());
    video.srcObject = stream;
    _stream = stream;
    try {
      await video.play().toDart;
    } catch (_) {
      // Autoplay of a muted video is allowed everywhere; ignore odd browsers.
    }

    _clock.start();
    _timer = Timer.periodic(const Duration(milliseconds: _frameEveryMs), (_) => _check(face));
  }

  web.HTMLVideoElement _makeVideo() {
    final video = web.HTMLVideoElement()
      ..autoplay = true
      ..muted = true
      ..playsInline = true;
    video.style
      ..width = '100%'
      ..height = '100%'
      ..objectFit = 'cover'
      ..transform = 'scaleX(-1)'; // like a mirror, as on the phone
    if (!_viewRegistered) {
      ui_web.platformViewRegistry.registerViewFactory(_viewType, (int _) => _video!);
      _viewRegistered = true;
    }
    return video;
  }

  void _check(_KaboomFace face) {
    final video = _video;
    if (video == null || _readings.isClosed) return;
    framesReceived++;
    try {
      final result = face.detect(video);
      if (result == null) return; // the video has no picture yet
      final observation = result.present
          ? FaceObservation(
              facePresent: true,
              yaw: result.yaw,
              pitch: result.pitch,
              roll: result.roll,
              eyeOpen: result.eyeOpen,
            )
          : const FaceObservation.noFace();
      final state = rules.update(_clock.elapsedMilliseconds / 1000, observation);
      problem.value = null;
      _readings.add(FocusReading(
        state: state,
        observation: rules.lastObservation,
        reasons: rules.reasons,
        calibrated: rules.calibrated,
      ));
    } catch (e) {
      debugPrint('Face detection failed on one frame: $e');
      problem.value = 'Face detection failed: $e';
    }
  }

  @override
  Future<void> stop() async {
    _timer?.cancel();
    _timer = null;
    final stream = _stream;
    _stream = null;
    if (stream != null) {
      for (final track in stream.getTracks().toDart) {
        track.stop(); // turns the camera light off
      }
    }
    _video?.srcObject = null;
  }

  @override
  Future<void> dispose() async {
    await stop();
    await _readings.close();
    problem.dispose();
  }
}
