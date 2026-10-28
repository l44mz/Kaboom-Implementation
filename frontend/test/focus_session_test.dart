import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/session/models/focus_session.dart';
import 'package:kaboom_app/features/session/models/focus_state.dart';
import 'package:kaboom_app/features/session/models/session_clock.dart';
import 'package:kaboom_app/features/session/screens/focus_session_screen.dart';

void main() {
  final start = DateTime(2026, 10, 28, 14);

  group('SessionClock', () {
    test('counts down from the planned duration', () {
      final clock = SessionClock(startTime: start, plannedSeconds: 900);
      final later = start.add(const Duration(minutes: 1, seconds: 30));
      expect(clock.remainingSeconds(start), 900);
      expect(clock.remainingSeconds(later), 810);
      expect(SessionClock.format(clock.remainingSeconds(later)), '13:30');
    });

    test('paused time does not count', () {
      final clock = SessionClock(startTime: start, plannedSeconds: 900);
      clock.pause(start.add(const Duration(minutes: 1)));
      expect(clock.remainingSeconds(start.add(const Duration(minutes: 5))), 840);
      clock.resume(start.add(const Duration(minutes: 5)));
      expect(clock.remainingSeconds(start.add(const Duration(minutes: 6))), 780);
    });

    test('stops at zero and reports finished', () {
      final clock = SessionClock(startTime: start, plannedSeconds: 600);
      final after = start.add(const Duration(minutes: 20));
      expect(clock.remainingSeconds(after), 0);
      expect(clock.remainingFraction(after), 0);
      expect(clock.isFinished(after), isTrue);
    });

    test('format pads minutes and seconds', () {
      expect(SessionClock.format(3600), '60:00');
      expect(SessionClock.format(65), '01:05');
    });
  });

  test('FocusState.fromApi reads the backend values', () {
    expect(FocusState.fromApi('DISTRACTED'), FocusState.distracted);
    expect(FocusState.fromApi('away'), FocusState.away);
    expect(FocusState.fromApi('something else'), FocusState.focused);
  });

  testWidgets('shows the timer, state chip and state message', (tester) async {
    // A phone-sized screen, like the Figma frames (402 x 874).
    tester.view.physicalSize = const Size(402, 874);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    final session = FocusSession(
      id: 'abc',
      plannedDurationSeconds: 900,
      startTime: DateTime.now(),
      status: 'active',
    );
    await tester.pumpWidget(MaterialApp(
      home: FocusSessionScreen(
        session: session,
        states: Stream.value(FocusState.distracted),
      ),
    ));
    await tester.pump();

    expect(find.textContaining(RegExp(r'^1[45]:\d\d$')), findsOneWidget);
    expect(find.text('Distracted'), findsOneWidget);
    expect(find.text(FocusState.distracted.message), findsOneWidget);
    expect(find.text('End Session'), findsOneWidget);

    await tester.tap(find.byTooltip('Pause'));
    await tester.pump();
    expect(find.text('Session paused'), findsOneWidget);

    // Remove the screen so its 1 second timer stops before the test ends.
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('fits a small 360 px phone without overflow', (tester) async {
    tester.view.physicalSize = const Size(360, 780);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(MaterialApp(
      home: FocusSessionScreen(
        session: FocusSession(
          id: 'abc',
          plannedDurationSeconds: 3600,
          startTime: DateTime.now(),
          status: 'active',
        ),
        states: Stream.value(FocusState.distracted),
      ),
    ));
    await tester.pump();
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('without a session it offers a way back to Home', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: FocusSessionScreen()));
    expect(find.text('No session is running.'), findsOneWidget);
    expect(find.text('Back to Home'), findsOneWidget);
  });
}
