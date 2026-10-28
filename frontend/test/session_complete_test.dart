import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/session/models/session_result.dart';
import 'package:kaboom_app/features/session/screens/session_complete_screen.dart';

void main() {
  group('SessionResult', () {
    test('coins follow the server rule: 1 per focused minute, max 30', () {
      expect(SessionResult.coinsFor(59), 0);
      expect(SessionResult.coinsFor(26 * 60 + 30), 26);
      expect(SessionResult.coinsFor(60 * 60), 30);
    });

    test('time focused reads naturally', () {
      String label(int s) =>
          SessionResult.local(focusedSeconds: s, endedEarly: false).timeFocusedLabel;
      expect(label(45), '45 seconds');
      expect(label(60), '1 minute');
      expect(label(60 * 60), '60 minutes');
    });
  });

  Future<void> pumpScreen(
    WidgetTester tester,
    SessionResult? result, {
    Size screen = const Size(402, 874), // the Figma frame
  }) async {
    tester.view.physicalSize = screen;
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      MaterialApp(home: SessionCompleteScreen(result: result)),
    );
  }

  testWidgets('shows time focused, coins and both buttons', (tester) async {
    await pumpScreen(
      tester,
      SessionResult.local(focusedSeconds: 20 * 60, endedEarly: false),
    );
    expect(find.text('Good Job!'), findsOneWidget);
    expect(find.text('Focus Session Summary'), findsOneWidget);
    expect(find.text('20 minutes'), findsOneWidget);
    expect(find.text('20'), findsOneWidget);
    expect(find.text('Shop'), findsOneWidget);
    expect(find.text('Back Home'), findsOneWidget);
  });

  testWidgets('says when the session ended early (US-09)', (tester) async {
    await pumpScreen(
      tester,
      SessionResult.local(focusedSeconds: 90, endedEarly: true),
    );
    expect(find.text('Session ended early'), findsOneWidget);
  });

  testWidgets('fits a small 360 px phone without overflow', (tester) async {
    await pumpScreen(
      tester,
      SessionResult.local(focusedSeconds: 60 * 60, endedEarly: false),
      screen: const Size(360, 780),
    );
    expect(tester.takeException(), isNull);
  });
}
