import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/app.dart';

void main() {
  testWidgets('app starts on the sign up screen', (tester) async {
    await tester.pumpWidget(const KaboomApp());
    await tester.pumpAndSettle();
    expect(find.text('Sign Up'), findsOneWidget);
  });
}
