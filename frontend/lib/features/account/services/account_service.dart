import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/models/user.dart';
import '../models/robot_info.dart';

/// HTTP calls for My Account. No UI and no state here.
class AccountService {
  AccountService(this._api);

  final ApiClient _api;

  /// PUT /api/v1/users/me: changes the name and email.
  Future<User> updateProfile({required String name, required String email}) async {
    final response = await _api.dio.put(
      '/users/me',
      data: {'name': name, 'email': email},
    );
    return User.fromJson(response.data as Map<String, dynamic>);
  }

  /// PUT /api/v1/users/me/password
  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    await _api.dio.put(
      '/users/me/password',
      data: {'currentPassword': currentPassword, 'newPassword': newPassword},
    );
  }

  /// GET /api/v1/robots/me: the user's robot, or null if they don't have one (404).
  Future<RobotInfo?> getMyRobot() async {
    try {
      final response = await _api.dio.get('/robots/me');
      return RobotInfo.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) return null;
      rethrow;
    }
  }
}
