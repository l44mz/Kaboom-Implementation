import '../../../core/network/api_client.dart';
import '../models/focus_session.dart';
import '../models/focus_state.dart';
import 'session_service.dart';

/// API calls for camera mode: the phone's camera watches the user, not the robot.
/// The backend keeps these sessions apart (/camera-sessions, source CAMERA),
/// but they count for coins and history like robot sessions.
///
/// It extends SessionService, so the Focus Session screen can use either one
/// for pause, resume and end without knowing which mode is running.
class CameraSessionService extends SessionService {
  // super.api passes api to SessionService; it can still be used below.
  CameraSessionService(super.api) : _cameraApi = api;

  // SessionService's _api is private to its own file, so this class keeps its own.
  final ApiClient _cameraApi;

  /// POST /api/v1/camera-sessions/start. A camera session has no robot,
  /// so robotId is ignored. The backend needs at least 600 seconds (10 minutes).
  @override
  Future<FocusSession> startSession({
    required int plannedDurationSeconds,
    String? robotId,
  }) async {
    final response = await _cameraApi.dio.post(
      '/camera-sessions/start',
      data: {'plannedDurationSeconds': plannedDurationSeconds},
    );
    return FocusSession.fromCameraJson(response.data as Map<String, dynamic>);
  }

  /// PUT /api/v1/camera-sessions/{id}/pause
  @override
  Future<void> pauseSession(String sessionId) async {
    await _cameraApi.dio.put('/camera-sessions/$sessionId/pause');
  }

  /// PUT /api/v1/camera-sessions/{id}/resume
  @override
  Future<void> resumeSession(String sessionId) async {
    await _cameraApi.dio.put('/camera-sessions/$sessionId/resume');
  }

  /// PUT /api/v1/camera-sessions/{id}/end. Returns the coins earned (rewardEarned).
  @override
  Future<int> endSession(String sessionId) async {
    final response = await _cameraApi.dio.put('/camera-sessions/$sessionId/end');
    final data = response.data as Map<String, dynamic>;
    return (data['rewardEarned'] as num?)?.toInt() ?? 0;
  }

  /// POST /api/v1/camera-sessions/{id}/state: what the phone's camera sees.
  /// [heartbeat] is a "still here" message sent every few seconds when nothing changed.
  /// [sequence] goes up by one each time, so the backend ignores old or repeated messages.
  /// Returns true when the backend says Sparky explodes.
  Future<bool> sendState(
    String sessionId, {
    required FocusState state,
    required int sequence,
    bool heartbeat = false,
  }) async {
    final response = await _cameraApi.dio.post(
      '/camera-sessions/$sessionId/state',
      data: {
        'sessionId': sessionId,
        'state': state.name.toUpperCase(), // FOCUSED, DISTRACTED or AWAY
        'timestamp': DateTime.now().millisecondsSinceEpoch,
        'sequenceNumber': sequence,
        'eventType': heartbeat ? 'HEARTBEAT' : 'STATE_CHANGE',
      },
    );
    final data = response.data as Map<String, dynamic>;
    return data['explosionTriggered'] == true;
  }
}