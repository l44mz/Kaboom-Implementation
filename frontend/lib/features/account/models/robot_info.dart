/// The user's robot, as returned by GET /robots/me.
class RobotInfo {
  const RobotInfo({
    required this.id,
    required this.name,
    required this.connected,
  });

  final String id;

  /// e.g. "FocusRobot - A4F2"
  final String name;

  /// True when the robot talked to the backend in the last 30 seconds.
  final bool connected;

  factory RobotInfo.fromJson(Map<String, dynamic> json) => RobotInfo(
        id: json['id'] as String,
        name: json['name'] as String,
        connected: (json['connected'] as bool?) ?? false,
      );
}
