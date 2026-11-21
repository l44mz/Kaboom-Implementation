import 'package:dio/dio.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../models/focus_session.dart';
import '../models/focus_state.dart';

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

  /// GET /api/v1/focus-sessions/{id}: the robot's latest state for this session,
  /// or null if the robot hasn't sent one yet.
  Future<FocusState?> getCurrentState(String sessionId) async {
    final response = await _api.dio.get('/focus-sessions/$sessionId');
    final data = response.data as Map<String, dynamic>;
    final state = data['currentState'];
    return state is String ? FocusState.fromApi(state) : null;
  }

  /// The robot's states during a session. Asks the backend every [every] and
  /// gives a new value only when the state changes. Stops when the listener cancels.
  Stream<FocusState> focusStates(
    String sessionId, {
    Duration every = const Duration(seconds: 2),
  }) async* {
    FocusState? last;
    while (true) {
      try {
        final state = await getCurrentState(sessionId);
        if (state != null && state != last) {
          last = state;
          yield state;
        }
      } catch (_) {
        // Network hiccup: keep showing the last state and try again next time.
      }
      await Future<void>.delayed(every);
    }
  }

  /// PUT /api/v1/focus-sessions/{id}/pause
  Future<void> pauseSession(String sessionId) async {
    await _api.dio.put('/focus-sessions/$sessionId/pause');
  }

  /// PUT /api/v1/focus-sessions/{id}/resume
  Future<void> resumeSession(String sessionId) async {
    await _api.dio.put('/focus-sessions/$sessionId/resume');
  }

  /// PUT /api/v1/focus-sessions/{id}/end. Returns the coins earned (rewardEarned).
  Future<int> endSession(String sessionId) async {
    final response = await _api.dio.put('/focus-sessions/$sessionId/end');
    final data = response.data as Map<String, dynamic>;
    return (data['rewardEarned'] as num?)?.toInt() ?? 0;
  }
}

/// The message to show when starting a session fails.
String startSessionErrorMessage(Object error) {
  if (error is DioException && error.response?.statusCode == 409) {
    return 'You already have a session running.';
  }
  return apiErrorMessage(error);
}
