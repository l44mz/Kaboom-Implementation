import '../../../core/network/api_client.dart';
import '../models/login_response.dart';

/// API calls for authentication. No UI and no state here.
class AuthService {
  AuthService(this._api);

  final ApiClient _api;

  /// POST /api/v1/auth/register
  Future<void> register({
    required String name,
    required String email,
    required String password,
  }) async {
    await _api.dio.post('/auth/register', data: {
      'name': name,
      'email': email,
      'password': password,
    });
  }

  /// POST /api/v1/auth/login
  Future<LoginResponse> login({
    required String email,
    required String password,
  }) async {
    final response = await _api.dio.post('/auth/login', data: {
      'email': email,
      'password': password,
    });
    return LoginResponse.fromJson(response.data as Map<String, dynamic>);
  }
}