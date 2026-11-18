import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../../../shared/models/user.dart';
import '../services/auth_service.dart';

/// Logic and state for the Sign Up and Login screens.
class AuthController extends ChangeNotifier {
  AuthController({TokenStorage? tokens})
      : _tokens = tokens ?? TokenStorage() {
    _authService = AuthService(ApiClient(_tokens));
  }

  final TokenStorage _tokens;
  late final AuthService _authService;

  bool isLoading = false;
  String? errorMessage;

  // ---------- validation (used by both screens) ----------

  static String? validateName(String? value) {
    final name = value?.trim() ?? '';
    if (name.isEmpty) return 'Please enter your name';
    if (name.length > 100) return 'Name must be 100 characters or less';
    return null;
  }

  static String? validateEmail(String? value) {
    final email = value?.trim() ?? '';
    if (email.isEmpty) return 'Please enter your email';
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email)) {
      return 'Please enter a valid email';
    }
    return null;
  }

  /// Sign up rules: same as the backend (8+ characters, at least one number).
  static String? validateNewPassword(String? value) {
    final password = value ?? '';
    if (password.isEmpty) return 'Please enter a password';
    if (password.length < 8) return 'Password must be at least 8 characters';
    if (!RegExp(r'\d').hasMatch(password)) return 'Password must contain at least one number';
    return null;
  }

  /// Login only checks that something was typed.
  static String? validateLoginPassword(String? value) =>
      (value ?? '').isEmpty ? 'Please enter your password' : null;

  // ---------- actions ----------

  /// Returns true if the account was created.
  Future<bool> signUp({
    required String name,
    required String email,
    required String password,
  }) {
    return _run(() => _authService.register(
          name: name.trim(),
          email: email.trim(),
          password: password,
        ), conflictMessage: 'This email is already registered.');
  }

  /// Returns the logged-in user, or null if it failed (see [errorMessage]).
  Future<User?> login({required String email, required String password}) async {
    User? user;
    final ok = await _run(() async {
      final result = await _authService.login(email: email.trim(), password: password);
      await _tokens.save(access: result.accessToken, refresh: result.refreshToken);
      user = result.user;
    }, unauthorizedMessage: 'Email or password is incorrect.');
    return ok ? user : null;
  }

  // Shared loading / error handling for every action.
  Future<bool> _run(
    Future<void> Function() action, {
    String? conflictMessage,
    String? unauthorizedMessage,
  }) async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();
    try {
      await action();
      return true;
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      if (status == 409 && conflictMessage != null) {
        errorMessage = conflictMessage;
      } else if (status == 401 && unauthorizedMessage != null) {
        errorMessage = unauthorizedMessage;
      } else {
        errorMessage = apiErrorMessage(e);
      }
      return false;
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}