/// The user's robot, as returned by GET /robots/me.
class RobotInfo {
  const RobotInfo({
    required this.id,
    required this.name,
    required this.connected,
  });

  final String id;

  
  final String name;

 
  final bool connected;

  factory RobotInfo.fromJson(Map<String, dynamic> json) => RobotInfo(
        id: json['id'] as String,
        name: json['name'] as String,
        connected: (json['connected'] as bool?) ?? false,
      );
}
