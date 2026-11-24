import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/session/models/focus_session.dart';
import 'package:kaboom_app/shared/controllers/focus_mode_controller.dart';

void main() {
  group('FocusSession', () {
    test('reads a camera session (the backend calls the id sessionId)', () {
      final session = FocusSession.fromCameraJson({
        'sessionId': 'abc-123',
        'plannedDurationSeconds': 900,
        'startTime': '2026-11-24T19:00:00',
        'status': 'active',
        'currentState': null,
        'focusedSeconds': 0,
      });

      expect(session.id, 'abc-123');
      expect(session.plannedDurationSeconds, 900);
      expect(session.isCamera, isTrue);
      expect(session.robotId, isNull);
    });

    test('a robot session is not a camera session', () {
      final session = FocusSession.fromJson({
        'id': 'r-1',
        'robotId': 'robot-1',
        'plannedDurationSeconds': 600,
        'startTime': '2026-11-24T19:00:00',
        'status': 'active',
      });

      expect(session.isCamera, isFalse);
      expect(session.source, FocusSession.robotSource);
    });
  });

  group('FocusMode', () {
    test('reads the saved name, and anything unknown is the robot', () {
      expect(FocusMode.fromName('phone'), FocusMode.phone);
      expect(FocusMode.fromName('robot'), FocusMode.robot);
      expect(FocusMode.fromName(null), FocusMode.robot);
      expect(FocusMode.fromName('something else'), FocusMode.robot);
    });
  });
}