import '../../core/network/api_client.dart';

class AuthService {
  AuthService(this._api);

  final ApiClient _api;

  /// POST /api/v1/auth/register
  /// Throws a DioException if the server returns an error.
  Future<void> register({
    required String name,
    required String email,
    required String password,
  }) async {
    await _api.dio.post(
      '/auth/register',
      data: {
        'name': name,
        'email': email,
        'password': password,
      },
    );
  }
}