/// A focus session, as returned by POST /focus-sessions/start or POST /camera-sessions/start.
class FocusSession {
  const FocusSession({
    required this.id,
    required this.plannedDurationSeconds,
    required this.startTime,
    required this.status,
    this.robotId,
    this.source = robotSource,
  });

  /// Watched by Sparky (the robot).
  static const robotSource = 'ROBOT';

  /// Watched by the phone's front camera (camera mode).
  static const cameraSource = 'CAMERA';

  final String id;
  final String? robotId;
  final int plannedDurationSeconds;
  final DateTime startTime;
  final String status;

  /// ROBOT or CAMERA, like the backend's SessionSource.
  final String source;

  /// True when the phone's camera watches this session instead of the robot.
  bool get isCamera => source == cameraSource;

  /// From POST /focus-sessions/start (a robot session).
  factory FocusSession.fromJson(Map<String, dynamic> json) => FocusSession(
        id: json['id'] as String,
        robotId: json['robotId'] as String?,
        plannedDurationSeconds: json['plannedDurationSeconds'] as int,
        startTime: DateTime.parse(json['startTime'] as String),
        status: json['status'] as String,
      );

  /// From POST /camera-sessions/start. The backend calls the id "sessionId" here.
  factory FocusSession.fromCameraJson(Map<String, dynamic> json) => FocusSession(
        id: json['sessionId'] as String,
        plannedDurationSeconds: json['plannedDurationSeconds'] as int,
        startTime: DateTime.parse(json['startTime'] as String),
        status: json['status'] as String,
        source: cameraSource,
      );
}