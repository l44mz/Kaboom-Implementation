import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../../../shared/controllers/user_controller.dart';
import '../../../shared/models/user.dart';
import '../../history/models/history.dart';
import '../../history/services/history_service.dart';
import '../models/robot_info.dart';
import '../services/account_service.dart';

/// Logic and state for My Account.
/// The profile and coins come from the shared [UserController], so they match the side menu.
class AccountController extends ChangeNotifier {
  AccountController({required this.users, TokenStorage? tokens})
      : _tokens = tokens ?? TokenStorage() {
    final api = ApiClient(_tokens);
    _account = AccountService(api);
    _history = HistoryService(api);
  }

  final UserController users;
  final TokenStorage _tokens;
  late final AccountService _account;
  late final HistoryService _history;

  User? get user => users.user;
  RobotInfo? robot;

  /// All-time numbers for the three boxes.
  int? sessionCount;
  int? focusSeconds;

  bool isLoading = false;
  String? errorMessage;

  /// Only on this device for now; there is no notifications backend yet.
  bool notificationsOn = true;

  /// Loads the profile, the robot and the all-time stats at the same time.
  Future<void> load() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();
    try {
      final results = await Future.wait<Object?>([
        users.load(),
        _account.getMyRobot(),
        _history.getHistory(HistoryRange.all),
      ]);
      robot = results[1] as RobotInfo?;
      final allTime = results[2] as HistoryData;
      sessionCount = allTime.sessionCount;
      focusSeconds = allTime.totalFocusSeconds;
    } on DioException catch (e) {
      errorMessage = apiErrorMessage(e);
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  /// Saves a new name and email. Returns null on success, or a message.
  Future<String?> updateProfile(String name, String email) async {
    try {
      final updated =
          await _account.updateProfile(name: name.trim(), email: email.trim());
      // With a new email the old login token stops working, so only reload when it is the same.
      if (updated.email == users.user?.email) await users.refresh();
      return null;
    } on DioException catch (e) {
      return switch (e.response?.statusCode) {
        409 => 'This email is already used by another account.',
        400 => 'Please check the name and email.',
        _ => apiErrorMessage(e),
      };
    }
  }

  /// Changes the password. Returns null on success, or a message.
  Future<String?> changePassword(String current, String newPassword) async {
    try {
      await _account.changePassword(currentPassword: current, newPassword: newPassword);
      return null;
    } on DioException catch (e) {
      return e.response?.statusCode == 400
          ? 'Your current password is wrong.'
          : apiErrorMessage(e);
    }
  }

  void setNotifications(bool value) {
    notificationsOn = value;
    notifyListeners();
  }

  Future<void> logout() async {
    await _tokens.clear();
    users.clear();
  }

  // ---------- validation ----------

  static String? validateName(String? value) {
    final name = value?.trim() ?? '';
    if (name.isEmpty) return 'Please enter your name';
    return null;
  }

  static String? validateEmail(String? value) {
    final email = value?.trim() ?? '';
    if (email.isEmpty) return 'Please enter your email';
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email)) {
      return 'Enter a valid email';
    }
    return null;
  }

  static String? validateNewPassword(String? value) {
    if ((value ?? '').length < 8) return 'Use at least 8 characters';
    return null;
  }
}
