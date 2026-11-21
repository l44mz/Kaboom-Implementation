import 'package:get/get.dart';

import '../../core/network/api_client.dart';
import '../../core/storage/token_storage.dart';
import '../models/user.dart';
import '../services/user_service.dart';

/// The logged-in user, shared by every screen so the coin balance is the same everywhere.
/// Registered once in main.dart: Get.put(UserController(), permanent: true).
class UserController extends GetxController {
  UserController({UserService? service})
      : _service = service ?? UserService(ApiClient(TokenStorage()));

  final UserService _service;


  final user = Rxn<User>();

  /// Loads the user from GET /users/me. Throws if it fails, so the screen can show the error.
  Future<void> load() async {
    user.value = await _service.getMe();
  }

  /// Reloads the user after something changed the coins (for example a finished session).
  /// If it fails, the old balance stays on screen.
  /// Named reload, not refresh: GetxController already has a refresh() method.
  Future<void> reload() async {
    try {
      await load();
    } catch (_) {}
  }

  /// Forgets the user on log out.
  void clear() => user.value = null;
}