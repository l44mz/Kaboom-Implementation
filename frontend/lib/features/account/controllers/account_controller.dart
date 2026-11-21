import 'package:dio/dio.dart';
import 'package:get/get.dart';

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
class AccountController extends GetxController {
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

 
  User? get user => users.user.value;

  final robot = Rxn<RobotInfo>();

  final sessionCount = RxnInt();
  final focusSeconds = RxnInt();

  final isLoading = false.obs;
  final errorMessage = RxnString();


  final notificationsOn = true.obs;

  // ---------- lifecycle ----------

  @override
  void onInit() {
    super.onInit();
    load();
  }

  // ---------- actions ----------

  /// Loads the profile, the robot and the all-time stats at the same time.
  Future<void> load() async {
    isLoading.value = true;
    errorMessage.value = null;
    try {
      final results = await Future.wait<Object?>([
        users.load(),
        _account.getMyRobot(),
        _history.getHistory(HistoryRange.all),
      ]);
      robot.value = results[1] as RobotInfo?;
      final allTime = results[2] as HistoryData;
      sessionCount.value = allTime.sessionCount;
      focusSeconds.value = allTime.totalFocusSeconds;
    } on DioException catch (e) {
      errorMessage.value = apiErrorMessage(e);
    } finally {
      isLoading.value = false;
    }
  }

  /// Saves a new name and email. Returns null on success, or a message.
  Future<String?> updateProfile(String name, String email) async {
    try {
      final updated =
          await _account.updateProfile(name: name.trim(), email: email.trim());
      // With a new email the old login token stops working, so only reload when it is the same.
      if (updated.email == users.user.value?.email) await users.reload();
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

  void setNotifications(bool value) => notificationsOn.value = value;

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