import 'dart:io' show Platform;

import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

import '../../features/session/models/session_clock.dart';

/// The phone notifications for a focus session (Android only).
///
/// While a session runs, a notification that can't be swiped away shows the
/// time left counting down. Android counts it down by itself, so it keeps
/// going even when the app is closed or the phone is locked.
/// A second notification is booked for the end time: "Session complete!".
///
/// On web, Windows and in tests every method does nothing.
class SessionNotifications {
  SessionNotifications._();

  /// One shared object for the whole app.
  static final SessionNotifications instance = SessionNotifications._();

  static const _runningId = 1; // the countdown (or "Paused")
  static const _finishedId = 2; // "Session complete!"

  static const _timerChannel = AndroidNotificationDetails(
    'session_timer',
    'Session timer',
    channelDescription: 'Shows the time left while a focus session runs.',
    importance: Importance.low, // no sound, no pop-up, just in the bar
    priority: Priority.low,
  );

  static const _finishedDetails = NotificationDetails(
    android: AndroidNotificationDetails(
      'session_finished',
      'Session finished',
      channelDescription: 'Tells you when a focus session is over.',
      importance: Importance.high, // sound + pop-up
      priority: Priority.high,
      category: AndroidNotificationCategory.alarm,
    ),
  );

  final _plugin = FlutterLocalNotificationsPlugin();
  bool _ready = false;

  bool get _supported => !kIsWeb && Platform.isAndroid;

  AndroidFlutterLocalNotificationsPlugin? get _android =>
      _plugin.resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin>();

  /// Call once in main(), before runApp.
  Future<void> init() async {
    if (!_supported || _ready) return;
    try {
      tz_data.initializeTimeZones();
      await _plugin.initialize(
        settings: const InitializationSettings(
          android: AndroidInitializationSettings('@mipmap/ic_launcher'),
        ),
      );
      _ready = true;
    } catch (e) {
      debugPrint('Notifications are off: $e');
    }
  }

  /// Android 13+ asks the user "Allow Kaboom to send notifications?".
  /// Android only shows the question once; after that it returns the answer.
  Future<void> askPermission() => _safe(() async {
        await _android?.requestNotificationsPermission();
      });

  /// The session is running and ends at [endTime].
  /// Shows the countdown and books the "Session complete!" notification.
  Future<void> showRunning(DateTime endTime) => _safe(() async {
        final left = endTime.difference(DateTime.now());
        if (left <= Duration.zero) return;

        await _plugin.show(
          id: _runningId,
          title: 'Focus session running',
          body: 'Stay focused, Sparky is watching!',
          notificationDetails: NotificationDetails(
            android: AndroidNotificationDetails(
              _timerChannel.channelId,
              _timerChannel.channelName,
              channelDescription: _timerChannel.channelDescription,
              importance: _timerChannel.importance,
              priority: _timerChannel.priority,
              ongoing: true, // can't be swiped away
              autoCancel: false,
              onlyAlertOnce: true,
              showWhen: true,
              when: endTime.millisecondsSinceEpoch, // count down to this time
              usesChronometer: true,
              chronometerCountDown: true,
              timeoutAfter: left.inMilliseconds, // removed at 00:00
              category: AndroidNotificationCategory.progress,
            ),
          ),
        );

        // Exact time if the phone allows it; otherwise Android may be a little late.
        final exact = await _android?.canScheduleExactNotifications() ?? false;
        await _plugin.zonedSchedule(
          id: _finishedId,
          scheduledDate: tz.TZDateTime.from(endTime, tz.UTC),
          notificationDetails: _finishedDetails,
          androidScheduleMode: exact
              ? AndroidScheduleMode.exactAllowWhileIdle
              : AndroidScheduleMode.inexactAllowWhileIdle,
          title: 'Session complete!',
          body: 'Great focus! Open Kaboom to see your coins.',
        );
      });

  /// The session is paused: stop the countdown and un-book the end.
  Future<void> showPaused(int secondsLeft) => _safe(() async {
        await _plugin.cancel(id: _finishedId);
        await _plugin.show(
          id: _runningId,
          title: 'Focus session paused',
          body: '${SessionClock.format(secondsLeft)} left. Open Kaboom to resume.',
          notificationDetails: NotificationDetails(
            android: AndroidNotificationDetails(
              _timerChannel.channelId,
              _timerChannel.channelName,
              channelDescription: _timerChannel.channelDescription,
              importance: _timerChannel.importance,
              priority: _timerChannel.priority,
              ongoing: true,
              autoCancel: false,
              onlyAlertOnce: true,
              showWhen: false,
            ),
          ),
        );
      });

  /// The session is over. Removes the countdown.
  /// If the time ran out, shows "Session complete!" now (replaces the booked one).
  /// If the user ended it early, shows nothing.
  Future<void> sessionEnded({required bool endedEarly}) => _safe(() async {
        await _plugin.cancel(id: _runningId);
        await _plugin.cancel(id: _finishedId);
        if (endedEarly) return;
        await _plugin.show(
          id: _finishedId,
          title: 'Session complete!',
          body: 'Great focus! Open Kaboom to see your coins.',
          notificationDetails: _finishedDetails,
        );
      });

  /// A notification problem must never break the session itself.
  Future<void> _safe(Future<void> Function() action) async {
    if (!_ready) return;
    try {
      await action();
    } catch (e) {
      debugPrint('Notification failed: $e');
    }
  }
}
