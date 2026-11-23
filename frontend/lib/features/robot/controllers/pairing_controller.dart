import 'package:dio/dio.dart';
import 'package:get/get.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../../account/models/robot_info.dart';
import '../services/robot_service.dart';

/// Logic and state for the Connect Robot screen.
class PairingController extends GetxController {
  PairingController({RobotService? service})
      : _service = service ?? RobotService(ApiClient(TokenStorage()));

  final RobotService _service;

  /// The code is always 6 digits (the robot shows it on its screen).
  static const codeLength = 6;

  // ---------- state (observable) ----------

  final isPairing = false.obs;
  final errorMessage = RxnString();

  /// Set once pairing worked: the screen then shows "Sparky is yours!".
  final pairedRobot = Rxn<RobotInfo>();

  // ---------- validation ----------

  static String? validateCode(String code) {
    if (code.length != codeLength || !RegExp(r'^\d{6}$').hasMatch(code)) {
      return 'Type the 6 digits from Sparky\'s screen';
    }
    return null;
  }

  // ---------- actions ----------

  /// Returns true if the robot is now linked to the user.
  Future<bool> pair(String code) async {
    final invalid = validateCode(code);
    if (invalid != null) {
      errorMessage.value = invalid;
      return false;
    }

    isPairing.value = true;
    errorMessage.value = null;
    try {
      pairedRobot.value = await _service.pair(code);
      return true;
    } on DioException catch (e) {
      errorMessage.value = _pairError(e);
      return false;
    } finally {
      isPairing.value = false;
    }
  }

  /// Clears the error while the user types again.
  void clearError() => errorMessage.value = null;

  /// The backend's answers for POST /robots/pair, in words the user understands.
  String _pairError(DioException e) {
    switch (e.response?.statusCode) {
      case 404:
        return 'That code is wrong or expired. Check the code on Sparky\'s screen.';
      case 409:
        return 'Your account already has a different robot.';
      default:
        return apiErrorMessage(e);
    }
  }
}