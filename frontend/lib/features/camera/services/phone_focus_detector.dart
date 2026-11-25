import 'dart:async';
import 'dart:io' show Platform;

import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';

import '../../session/models/focus_state.dart';
import '../focus/face_observation.dart';
import '../focus/focus_rules.dart';
import 'focus_detector.dart';

/// Used by [FocusDetector] in the Android and iOS apps.
FocusDetector createFocusDetector() => PhoneFocusDetector();

/// Watches the user with the phone's front camera and decides FOCUSED, DISTRACTED or AWAY.
///
/// 1. The camera sends small frames (low resolution is enough for a face).
/// 2. About 5 times a second, ML Kit finds the face: head angles and open eyes.
/// 3. FocusRules (the same rules as Sparky) turns that into a state.
///
/// Everything runs on the phone. No picture leaves it.
class PhoneFocusDetector implements FocusDetector {
  PhoneFocusDetector({FocusRules? rules}) : rules = rules ?? FocusRules();

  final FocusRules rules;

  final _faces = FaceDetector(
    options: FaceDetectorOptions(
      enableClassification: true, // gives the "eye open" probabilities
      performanceMode: FaceDetectorMode.fast,
      minFaceSize: 0.15, // ignore tiny faces in the background
    ),
  );

  final _readings = StreamController<FocusReading>.broadcast();
  final _clock = Stopwatch();

  CameraController? _camera;
  bool _busy = false; // ML Kit is still working on the last frame
  int _lastFrameMs = -1000;

  /// Check a frame at most this often (5 a second is plenty for a 1-second rule).
  static const _frameEveryMs = 200;

  /// The camera, for the preview on screen. Null until [start] finishes.
  CameraController? get camera => _camera;

  @override
  final problem = ValueNotifier<String?>(null);

  @override
  int framesReceived = 0;

  @override
  Stream<FocusReading> get readings => _readings.stream;

  @override
  Stream<FocusState> get states => readings.map((r) => r.state).distinct();

  @override
  bool get isRunning => _camera?.value.isInitialized ?? false;

  /// The camera's aspect ratio is for landscape; the phone is held upright.
  @override
  double get previewAspectRatio => 1 / (_camera?.value.aspectRatio ?? 4 / 3);

  @override
  Widget buildPreview() {
    final camera = _camera;
    return camera == null ? const SizedBox.shrink() : CameraPreview(camera);
  }

  /// Android asks the user for the camera permission the first time.
  @override
  Future<void> start() async {
    final cameras = await availableCameras();
    if (cameras.isEmpty) {
      throw CameraException('noCamera', 'This phone has no camera.');
    }
    final front = cameras.firstWhere(
      (c) => c.lensDirection == CameraLensDirection.front,
      orElse: () => cameras.first,
    );
    final controller = CameraController(
      front,
      ResolutionPreset.low,
      enableAudio: false,
      // The two formats ML Kit reads directly, without converting the picture.
      imageFormatGroup: Platform.isAndroid ? ImageFormatGroup.nv21 : ImageFormatGroup.bgra8888,
    );
    await controller.initialize();
    _camera = controller;
    _clock.start();
    await controller.startImageStream(_onFrame);
  }

  @override
  Future<void> stop() async {
    final controller = _camera;
    _camera = null;
    if (controller == null) return;
    if (controller.value.isStreamingImages) await controller.stopImageStream();
    await controller.dispose();
  }

  @override
  Future<void> dispose() async {
    await stop();
    await _faces.close();
    await _readings.close();
    problem.dispose();
  }

  void _onFrame(CameraImage image) {
    framesReceived++;
    final now = _clock.elapsedMilliseconds;
    if (_busy || now - _lastFrameMs < _frameEveryMs) return; // skip this frame
    _busy = true;
    _lastFrameMs = now;
    _check(image, now / 1000).whenComplete(() => _busy = false);
  }

  Future<void> _check(CameraImage image, double seconds) async {
    final input = _toInputImage(image);
    if (input == null) {
      problem.value = 'Camera picture not supported: format ${image.format.raw}, '
          '${image.planes.length} planes, ${image.width}x${image.height}.';
      return;
    }
    try {
      final faces = await _faces.processImage(input);
      final state = rules.update(seconds, _observe(faces));
      if (_readings.isClosed) return;
      problem.value = null;
      _readings.add(FocusReading(
        state: state,
        observation: rules.lastObservation,
        reasons: rules.reasons,
        calibrated: rules.calibrated,
      ));
    } catch (e) {
      debugPrint('Face detection failed on one frame: $e');
      if (!_readings.isClosed) problem.value = 'Face detection failed: $e';
    }
  }

  /// The biggest face is the user (someone walking behind is smaller).
  FaceObservation _observe(List<Face> faces) {
    if (faces.isEmpty) return const FaceObservation.noFace();
    final face = faces.reduce((a, b) => _area(a) >= _area(b) ? a : b);
    final left = face.leftEyeOpenProbability;
    final right = face.rightEyeOpenProbability;
    final eyeOpen = left != null && right != null ? (left + right) / 2 : left ?? right;
    return FaceObservation(
      facePresent: true,
      yaw: face.headEulerAngleY,
      pitch: face.headEulerAngleX,
      roll: face.headEulerAngleZ,
      eyeOpen: eyeOpen,
    );
  }

  static double _area(Face f) => f.boundingBox.width * f.boundingBox.height;

  static const _orientations = {
    DeviceOrientation.portraitUp: 0,
    DeviceOrientation.landscapeLeft: 90,
    DeviceOrientation.portraitDown: 180,
    DeviceOrientation.landscapeRight: 270,
  };

  /// Wraps a camera frame for ML Kit, with how much the picture is rotated.
  /// (From the google_mlkit_commons example.)
  InputImage? _toInputImage(CameraImage image) {
    final controller = _camera;
    if (controller == null) return null;
    final sensor = controller.description.sensorOrientation;

    InputImageRotation? rotation;
    if (Platform.isIOS) {
      rotation = InputImageRotationValue.fromRawValue(sensor);
    } else {
      final device = _orientations[controller.value.deviceOrientation];
      if (device == null) return null;
      final degrees = controller.description.lensDirection == CameraLensDirection.front
          ? (sensor + device) % 360 // the front camera is mirrored
          : (sensor - device + 360) % 360;
      rotation = InputImageRotationValue.fromRawValue(degrees);
    }
    if (rotation == null) return null;

    final size = Size(image.width.toDouble(), image.height.toDouble());
    final format = InputImageFormatValue.fromRawValue(image.format.raw as int);

    // iOS: one BGRA plane, ML Kit reads it as it is.
    if (Platform.isIOS) {
      if (format != InputImageFormat.bgra8888 || image.planes.length != 1) return null;
      return _input(image.planes.first.bytes, size, rotation, InputImageFormat.bgra8888,
          image.planes.first.bytesPerRow);
    }

    // Android, the easy case: the camera gave NV21 in one plane, as asked.
    if (format == InputImageFormat.nv21 && image.planes.length == 1) {
      return _input(image.planes.first.bytes, size, rotation, InputImageFormat.nv21,
          image.planes.first.bytesPerRow);
    }

    // Android, the common case: many phones ignore the NV21 request and send
    // YUV_420_888 in 3 planes (Y, U, V). ML Kit doesn't read that from bytes,
    // so join the planes into NV21 here.
    if (image.planes.length == 3) {
      return _input(_yuv420ToNv21(image), size, rotation, InputImageFormat.nv21, image.width);
    }
    return null;
  }

  static InputImage _input(
    Uint8List bytes,
    Size size,
    InputImageRotation rotation,
    InputImageFormat format,
    int bytesPerRow,
  ) =>
      InputImage.fromBytes(
        bytes: bytes,
        metadata: InputImageMetadata(
          size: size,
          rotation: rotation,
          format: format,
          bytesPerRow: bytesPerRow,
        ),
      );

  /// YUV_420_888 (3 planes) to NV21 (all Y bytes, then V and U taking turns).
  /// Each plane may have padding at the end of its rows (bytesPerRow), and U and V
  /// may skip bytes between pixels (bytesPerPixel), so copy pixel by pixel.
  static Uint8List _yuv420ToNv21(CameraImage image) {
    final width = image.width;
    final height = image.height;
    final yPlane = image.planes[0];
    final uPlane = image.planes[1];
    final vPlane = image.planes[2];
    final uvWidth = width ~/ 2;
    final uvHeight = height ~/ 2;

    final out = Uint8List(width * height + 2 * uvWidth * uvHeight);
    var i = 0;

    // Y: the brightness of every pixel, row by row (without the row padding).
    for (var row = 0; row < height; row++) {
      final start = row * yPlane.bytesPerRow;
      out.setRange(i, i + width, yPlane.bytes, start);
      i += width;
    }

    // V and U: the colour, one pair for each 2x2 block of pixels.
    final uStep = uPlane.bytesPerPixel ?? 1;
    final vStep = vPlane.bytesPerPixel ?? 1;
    for (var row = 0; row < uvHeight; row++) {
      for (var col = 0; col < uvWidth; col++) {
        out[i++] = vPlane.bytes[row * vPlane.bytesPerRow + col * vStep];
        out[i++] = uPlane.bytes[row * uPlane.bytesPerRow + col * uStep];
      }
    }
    return out;
  }
}