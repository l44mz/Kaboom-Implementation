import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/app.dart';

void main() {
  testWidgets('app starts on the splash screen, then goes to sign up', (tester) async {
    await tester.pumpWidget(const KaboomApp());
    // Sparky's face moves forever, so pumpAndSettle would never finish.
    // Pump a short time instead.
    await tester.pump(const Duration(milliseconds: 500));
    expect(find.text('KABOOM!'), findsOneWidget);
    expect(find.text('Sign Up'), findsNothing);

    // The splash stays 2.4 s, then the page change animates.
    await tester.pump(const Duration(milliseconds: 2000));
    await tester.pump(const Duration(milliseconds: 500));
    expect(find.text('KABOOM!'), findsNothing);
    expect(find.text('Sign Up'), findsOneWidget);
  });
}
