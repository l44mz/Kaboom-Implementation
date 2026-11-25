import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:get/get.dart';

/// Who watches the user during a session.
enum FocusMode {
  /// Sparky, the robot on the desk, with its own camera.
  robot,

  /// The phone's front camera. The phone shows Sparky's face.
  phone;

  /// Reads the saved name. Anything unknown means robot.
  static FocusMode fromName(String? name) => FocusMode.values.firstWhere(
        (m) => m.name == name,
        orElse: () => FocusMode.robot,
      );
}

/// The user's choice between the robot and the phone camera.
/// Shared by Home (which session to start) and the Focus Mode screen (where it is chosen).
/// The choice is saved on the phone, so it is still there next time the app opens.
class FocusModeController extends GetxController {
  FocusModeController({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  static const _key = 'focus_mode';
  final FlutterSecureStorage _storage;

  /// The current choice. Robot until the user picks the phone.
  final mode = FocusMode.robot.obs;

  /// The shared controller. Registers it the first time, so screens and tests
  /// that run without main() still work.
  static FocusModeController get to => Get.isRegistered<FocusModeController>()
      ? Get.find<FocusModeController>()
      : Get.put(FocusModeController(), permanent: true);

  /// Phone mode needs the camera and ML Kit, which only work in the Android and iOS apps.
  static bool get phoneSupported =>
      !kIsWeb &&
      (defaultTargetPlatform == TargetPlatform.android ||
          defaultTargetPlatform == TargetPlatform.iOS);

  bool get usesPhone => mode.value == FocusMode.phone;

  /// Reads the saved choice. Called once when the app starts.
  Future<void> load() async {
    try {
      final saved = FocusMode.fromName(await _storage.read(key: _key));
      mode.value = saved == FocusMode.phone && !phoneSupported ? FocusMode.robot : saved;
    } catch (_) {
      // No storage (for example in tests): keep the robot.
    }
  }

  /// Saves a new choice. The phone is refused where it cannot work (web).
  Future<void> choose(FocusMode value) async {
    if (value == FocusMode.phone && !phoneSupported) return;
    mode.value = value;
    try {
      await _storage.write(key: _key, value: value.name);
    } catch (_) {
      // Not saved, but the choice still works until the app closes.
    }
  }
}