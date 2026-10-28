/// Countdown math for a focus session, with pause and resume.
///
/// Time is passed in (`now`), so tests can use fixed times instead of waiting.
/// Mirrors the backend rule: paused time does not count.
class SessionClock {
  SessionClock({required this.startTime, required this.plannedSeconds});

  final DateTime startTime;
  final int plannedSeconds;

  Duration _pausedTotal = Duration.zero;
  DateTime? _pausedAt;

  bool get isPaused => _pausedAt != null;

  void pause(DateTime now) {
    if (!isPaused) _pausedAt = now;
  }

  void resume(DateTime now) {
    if (!isPaused) return;
    _pausedTotal += now.difference(_pausedAt!);
    _pausedAt = null;
  }

  /// Seconds of the session that have really run (not counting pauses).
  int activeSeconds(DateTime now) {
    final end = _pausedAt ?? now;
    final seconds = (end.difference(startTime) - _pausedTotal).inSeconds;
    return seconds.clamp(0, plannedSeconds);
  }

  int remainingSeconds(DateTime now) => plannedSeconds - activeSeconds(now);

  /// 1.0 at the start, 0.0 when the time is up. Drives the ring.
  double remainingFraction(DateTime now) =>
      plannedSeconds == 0 ? 0 : remainingSeconds(now) / plannedSeconds;

  bool isFinished(DateTime now) => remainingSeconds(now) == 0;

  /// "mm:ss", for example 30:22. A 60 minute session starts at "60:00".
  static String format(int seconds) {
    final m = (seconds ~/ 60).toString().padLeft(2, '0');
    final s = (seconds % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }
}
