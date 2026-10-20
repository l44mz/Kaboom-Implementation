/// A focus session, as returned by POST /sessions.
class FocusSession {
  const FocusSession({
    required this.id,
    required this.plannedDurationSeconds,
    required this.startTime,
    required this.status,
    this.robotId,
  });

  final String id;
  final String? robotId;
  final int plannedDurationSeconds;
  final DateTime startTime;
  final String status;

  factory FocusSession.fromJson(Map<String, dynamic> json) => FocusSession(
        id: json['id'] as String,
        robotId: json['robotId'] as String?,
        plannedDurationSeconds: json['plannedDurationSeconds'] as int,
        startTime: DateTime.parse(json['startTime'] as String),
        status: json['status'] as String,
      );
}
