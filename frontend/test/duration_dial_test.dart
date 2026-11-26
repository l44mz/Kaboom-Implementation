import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/home/widgets/duration_dial.dart';

/// A point on the ring at [minutes] of the clock (60 = 12 o'clock).
Offset onRing(double minutes) {
  const c = DurationDial.size / 2;
  const r = c - 40;
  final a = 2 * math.pi * minutes / 60 - math.pi / 2;
  return Offset(c + r * math.cos(a), c + r * math.sin(a));
}

/// The dial alone, keeping the chosen minutes like the Home screen does.
class _Host extends StatefulWidget {
  const _Host({this.enabled = true});
  final bool enabled;
  @override
  State<_Host> createState() => _HostState();
}

class _HostState extends State<_Host> {
  int minutes = 15;
  @override
  Widget build(BuildContext context) => MaterialApp(
        home: Scaffold(
          body: Center(
            child: DurationDial(
              minutes: minutes,
              enabled: widget.enabled,
              onChanged: (m) => setState(() => minutes = m),
            ),
          ),
        ),
      );
}

void main() {
  group('minutesAt', () {
    test('the clock positions give 15, 30 and 45 minutes', () {
      expect(DurationDial.minutesAt(onRing(15), previous: 5), 15);
      expect(DurationDial.minutesAt(onRing(30), previous: 5), 30);
      expect(DurationDial.minutesAt(onRing(45), previous: 5), 45);
    });

    test('snaps to the nearest 5 minutes', () {
      expect(DurationDial.minutesAt(onRing(22), previous: 5), 20);
      expect(DurationDial.minutesAt(onRing(23), previous: 5), 25);
    });

    test('12 o\'clock is 60 coming from a long time, 5 from a short one', () {
      expect(DurationDial.minutesAt(onRing(0), previous: 50), 60);
      expect(DurationDial.minutesAt(onRing(0), previous: 10), 5);
    });

    test('dragging past 12 o\'clock stops at the end instead of wrapping', () {
      expect(DurationDial.minutesAt(onRing(5), previous: 55, dragging: true), 60);
      expect(DurationDial.minutesAt(onRing(55), previous: 5, dragging: true), 5);
    });

    test('a tap can still jump across the clock', () {
      expect(DurationDial.minutesAt(onRing(5), previous: 55), 5);
    });

    test('a touch in the middle changes nothing', () {
      const c = DurationDial.size / 2;
      expect(DurationDial.minutesAt(const Offset(c + 5, c), previous: 15), isNull);
    });
  });

  testWidgets('shows the chosen time in the middle', (tester) async {
    await tester.pumpWidget(const _Host());
    expect(find.text('15:00'), findsOneWidget);
  });

  testWidgets('tapping the ring at 6 o\'clock picks 30 minutes', (tester) async {
    await tester.pumpWidget(const _Host());
    final topLeft = tester.getTopLeft(find.byType(DurationDial));
    await tester.tapAt(topLeft + onRing(30));
    await tester.pump();
    expect(find.text('30:00'), findsOneWidget);
  });

  testWidgets('dragging the knob around the clock changes the time', (tester) async {
    await tester.pumpWidget(const _Host());
    final topLeft = tester.getTopLeft(find.byType(DurationDial));
    final gesture = await tester.startGesture(topLeft + onRing(15));
    for (var m = 16.0; m <= 45; m += 1) {
      await gesture.moveTo(topLeft + onRing(m));
    }
    await gesture.up();
    await tester.pump();
    expect(find.text('45:00'), findsOneWidget);
  });

  testWidgets('cannot be changed while a session is starting', (tester) async {
    await tester.pumpWidget(const _Host(enabled: false));
    final topLeft = tester.getTopLeft(find.byType(DurationDial));
    await tester.tapAt(topLeft + onRing(30));
    await tester.pump();
    expect(find.text('15:00'), findsOneWidget);
  });
}
