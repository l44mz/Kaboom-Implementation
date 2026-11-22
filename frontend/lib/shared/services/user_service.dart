import '../../core/network/api_client.dart';
import '../models/user.dart';

/// API calls about the logged-in user.
class UserService {
  UserService(this._api);

  final ApiClient _api;

  /// GET /api/v1/users/me: the logged-in user's profile and coins.
  Future<User> getMe() async {
    final response = await _api.dio.get('/users/me');
    return User.fromJson(response.data as Map<String, dynamic>);
  }
}
