import 'package:dio/dio.dart';

import '../config/env.dart';
import '../storage/token_storage.dart';

/// Dio set up for the Kaboom API:
/// - adds the access token to every request
/// - on 401, gets a new access token with the refresh token and retries once
class ApiClient {
  ApiClient(this._tokens) : dio = Dio(_options()) {
    dio.interceptors.add(
      InterceptorsWrapper(onRequest: _addToken, onError: _refreshAndRetry),
    );
  }

  final TokenStorage _tokens;
  final Dio dio;

  // Used only for /auth/refresh. It has no interceptors, so a failed
  // refresh can't trigger another refresh.
  final Dio _refreshDio = Dio(_options());

  // If several requests get 401 at the same time, they share one refresh.
  Future<String?>? _refreshing;

  static BaseOptions _options() => BaseOptions(
        baseUrl: Env.apiBaseUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        headers: {'Content-Type': 'application/json'},
      );

  Future<void> _addToken(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await _tokens.accessToken;
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  Future<void> _refreshAndRetry(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final request = error.requestOptions;
    final shouldRefresh = error.response?.statusCode == 401 &&
        !request.path.startsWith('/auth/') && // login/register 401s are real
        request.extra['retried'] != true; // only retry once
    if (!shouldRefresh) return handler.next(error);

    final newToken = await (_refreshing ??=
        _refreshAccessToken().whenComplete(() => _refreshing = null));
    if (newToken == null) return handler.next(error);

    request.headers['Authorization'] = 'Bearer $newToken';
    request.extra['retried'] = true;
    try {
      handler.resolve(await dio.fetch(request));
    } on DioException catch (e) {
      handler.next(e);
    }
  }

  /// Returns a new access token, or null if there is no valid refresh token.
  Future<String?> _refreshAccessToken() async {
    final refresh = await _tokens.refreshToken;
    if (refresh == null) return null;
    try {
      final response = await _refreshDio.post(
        '/auth/refresh',
        data: {'refreshToken': refresh},
      );
      final access =
          (response.data as Map<String, dynamic>)['accessToken'] as String;
      await _tokens.saveAccess(access);
      return access;
    } on DioException {
      await _tokens.clear(); // refresh token expired or invalid: log out
      return null;
    }
  }
}