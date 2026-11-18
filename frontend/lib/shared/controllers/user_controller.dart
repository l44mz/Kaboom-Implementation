import 'package:flutter/foundation.dart';

import '../../core/network/api_client.dart';
import '../../core/storage/token_storage.dart';
import '../models/user.dart';
import '../services/user_service.dart';

/// The logged-in user, shared by every screen so the coin balance is the same everywhere.
class UserController extends ChangeNotifier {
  UserController({UserService? service})
      : _service = service ?? UserService(ApiClient(TokenStorage()));

  final UserService _service;

  User? user;

  /// Loads the user from GET /users/me. Throws if it fails, so the screen can show the error.
  Future<void> load() async {
    user = await _service.getMe();
    notifyListeners();
  }

  /// Reloads the user after something changed the coins (for example a finished session).
  /// If it fails, the old balance stays on screen.
  Future<void> refresh() async {
    try {
      await load();
    } catch (_) {}
  }

  /// Forgets the user on log out.
  void clear() {
    user = null;
    notifyListeners();
  }
}
