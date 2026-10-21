import '../../../core/network/api_client.dart';
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