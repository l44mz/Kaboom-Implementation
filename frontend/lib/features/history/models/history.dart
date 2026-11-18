/// The History tabs, as the backend names them (GET /focus-sessions/history?range=...).
enum HistoryRange {
  day('day', 'Day', 'Today'),
  week('week', 'Week', 'This week'),
  month('month', 'Month', 'This month'),
  all('all', 'All time', 'Last 6 months');

  const HistoryRange(this.apiValue, this.label, this.chartCaption);

  final String apiValue;

  /// Text on the tab.
  final String label;

  /// Small grey text at the top right of the chart.
  final String chartCaption;
}

/// Everything the History screen shows for one range.
class HistoryData {
  const HistoryData({
    required this.totalFocusSeconds,
    required this.sessionCount,
    required this.avgFocusPercentage,
    required this.bars,
    required this.sessions,
  });

  final int totalFocusSeconds;
  final int sessionCount;
  final double avgFocusPercentage;
  final List<HistoryBar> bars;
  final List<PastSession> sessions;

  factory HistoryData.fromJson(Map<String, dynamic> json) => HistoryData(
        totalFocusSeconds: (json['totalFocusSeconds'] as num).toInt(),
        sessionCount: (json['sessionCount'] as num).toInt(),
        avgFocusPercentage: (json['avgFocusPercentage'] as num).toDouble(),
        bars: (json['bars'] as List)
            .map((bar) => HistoryBar.fromJson(bar as Map<String, dynamic>))
            .toList(),
        sessions: (json['sessions'] as List)
            .map((session) => PastSession.fromJson(session as Map<String, dynamic>))
            .toList(),
      );
}

/// One bar of the "Focus minutes" chart.
class HistoryBar {
  const HistoryBar({
    required this.label,
    required this.focusMinutes,
    required this.current,
  });

  final String label;
  final int focusMinutes;

  /// True for today / this week / this month (drawn brighter).
  final bool current;

  factory HistoryBar.fromJson(Map<String, dynamic> json) => HistoryBar(
        label: json['label'] as String,
        focusMinutes: (json['focusMinutes'] as num).toInt(),
        current: (json['current'] as bool?) ?? false,
      );
}

/// One finished session in "Past sessions".
class PastSession {
  const PastSession({
    required this.id,
    required this.startTime,
    required this.activeSeconds,
    required this.focusedPercentage,
    required this.distractions,
    required this.rewardEarned,
  });

  final String id;
  final DateTime startTime;

  /// Session length without pauses.
  final int activeSeconds;
  final double focusedPercentage;
  final int distractions;
  final int rewardEarned;

  factory PastSession.fromJson(Map<String, dynamic> json) => PastSession(
        id: json['id'] as String,
        startTime: DateTime.parse(json['startTime'] as String),
        activeSeconds: (json['activeSeconds'] as num).toInt(),
        focusedPercentage: (json['focusedPercentage'] as num).toDouble(),
        distractions: (json['distractions'] as num).toInt(),
        rewardEarned: (json['rewardEarned'] as num).toInt(),
      );

  /// "50 min · 1 distraction"
  String get summary {
    final minutes = (activeSeconds / 60).round();
    final length = minutes == 0 ? '$activeSeconds sec' : '$minutes min';
    final word = distractions == 1 ? 'distraction' : 'distractions';
    return '$length · $distractions $word';
  }
}

/// Small date and time texts for History, without extra packages.
class HistoryFormat {
  static const _weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  static const _months = [
    'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
  ];

  /// 18000 → "5h 00m", 2700 → "45m", 0 → "0m"
  static String duration(int seconds) {
    final hours = seconds ~/ 3600;
    final minutes = (seconds % 3600) ~/ 60;
    if (hours == 0) return '${minutes}m';
    return '${hours}h ${minutes.toString().padLeft(2, '0')}m';
  }

  /// "Today · 4:30 PM", "Yesterday · 8:15 PM", "Fri, Oct 2 · 6:00 PM"
  static String sessionTitle(DateTime start, {DateTime? now}) {
    final today = _dateOnly(now ?? DateTime.now());
    final day = _dateOnly(start);
    final difference = today.difference(day).inDays;

    final String dayText;
    if (difference == 0) {
      dayText = 'Today';
    } else if (difference == 1) {
      dayText = 'Yesterday';
    } else {
      dayText = '${_weekdays[start.weekday - 1]}, ${_months[start.month - 1]} ${start.day}';
    }
    return '$dayText · ${time(start)}';
  }

  /// 16:30 → "4:30 PM"
  static String time(DateTime value) {
    final hour = value.hour % 12 == 0 ? 12 : value.hour % 12;
    final minute = value.minute.toString().padLeft(2, '0');
    return '$hour:$minute ${value.hour < 12 ? 'AM' : 'PM'}';
  }

  static DateTime _dateOnly(DateTime value) => DateTime(value.year, value.month, value.day);
}
