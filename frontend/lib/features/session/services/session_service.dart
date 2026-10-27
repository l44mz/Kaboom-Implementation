import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../models/focus_session.dart';

/// API calls for focus sessions.
class SessionService {
  SessionService(this._api);

  final ApiClient _api;

  /// POST /api/v1/focus-sessions/start: starts a session for the logged-in user.
  Future<FocusSession> startSession({
    required int plannedDurationSeconds,
    String? robotId,
  }) async {
    final response = await _api.dio.post(
      '/focus-sessions/start',
      data: {
        'plannedDurationSeconds': plannedDurationSeconds,
        'robotId': ?robotId,
      },
    );
    return FocusSession.fromJson(response.data as Map<String, dynamic>);
  }
}

/// The message to show when starting a session fails.
String startSessionErrorMessage(Object error) {
  if (error is DioException && error.response?.statusCode == 409) {
    return 'You already have a session running.';
  }
  return apiErrorMessage(error);
}
