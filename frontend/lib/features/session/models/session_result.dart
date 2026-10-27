import 'dart:math' as math;

/// What the Session Complete screen shows about a finished session.
class SessionResult {
  const SessionResult({
    required this.focusedSeconds,
    required this.coinsEarned,
    required this.endedEarly,
  });

  /// Built in the app until PUT /focus-sessions/{id}/end works, using the
  /// server's coin rule so the numbers match later.
  factory SessionResult.local({
    required int focusedSeconds,
    required bool endedEarly,
  }) =>
      SessionResult(
        focusedSeconds: focusedSeconds,
        coinsEarned: coinsFor(focusedSeconds),
        endedEarly: endedEarly,
      );

  final int focusedSeconds;
  final int coinsEarned;

  /// True when the user tapped End Session before the timer finished (US-09).
  final bool endedEarly;

  /// Server rule (FocusSessionService.endSession): one coin per focused minute, max 30.
  static int coinsFor(int focusedSeconds) => math.min(30, focusedSeconds ~/ 60);

  /// "60 minutes", "1 minute", or "45 seconds" under a minute.
  String get timeFocusedLabel {
    final minutes = focusedSeconds ~/ 60;
    if (minutes == 0) return '$focusedSeconds seconds';
    return minutes == 1 ? '1 minute' : '$minutes minutes';
  }
}
