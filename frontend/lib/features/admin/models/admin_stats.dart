/// The four numbers at the top of the admin dashboard.
/// GET /api/v1/admin/dashboard/stats
class AdminStats {
  const AdminStats({
    required this.totalUsers,
    required this.sessionsToday,
    required this.robotsOnline,
    required this.storeProducts,
  });

  final int totalUsers;
  final int sessionsToday;
  final int robotsOnline;
  final int storeProducts;

  factory AdminStats.fromJson(Map<String, dynamic> json) => AdminStats(
        totalUsers: (json['totalUsers'] as num).toInt(),
        sessionsToday: (json['sessionsToday'] as num).toInt(),
        robotsOnline: (json['robotsOnline'] as num).toInt(),
        storeProducts: (json['storeProducts'] as num).toInt(),
      );
}