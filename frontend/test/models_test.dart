import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/session/models/focus_session.dart';
import 'package:kaboom_app/shared/models/user.dart';

void main() {
  test('User.fromJson reads the backend UserDto', () {
    final user = User.fromJson({
      'id': '123e4567-e89b-12d3-a456-426614174000',
      'name': 'Shahd',
      'email': 'shahd@example.com',
      'coins': 340,
      'role': 'user',
    });

    expect(user.name, 'Shahd');
    expect(user.coins, 340);
    expect(user.isAdmin, isFalse);
  });

  test('admin role makes isAdmin true', () {
    final admin = User.fromJson({
      'id': '1',
      'name': 'Admin',
      'email': 'admin@example.com',
      'coins': 0,
      'role': 'admin',
    });

    expect(admin.isAdmin, isTrue);
  });

  test('FocusSession.fromJson reads the backend SessionResponse', () {
    final session = FocusSession.fromJson({
      'id': 'abc',
      'robotId': null,
      'plannedDurationSeconds': 900,
      'startTime': '2026-10-20T14:00:00',
      'status': 'active',
    });

    expect(session.plannedDurationSeconds, 900);
    expect(session.robotId, isNull);
    expect(session.startTime.hour, 14);
    expect(session.status, 'active');
  });
}
