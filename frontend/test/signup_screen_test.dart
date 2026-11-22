import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/auth/screens/signup_screen.dart';

void main() {
  testWidgets('shows errors when Sign Up is tapped with empty fields',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(home: SignUpScreen()));

    await tester.ensureVisible(find.text('Sign Up'));
    await tester.tap(find.text('Sign Up'));
    await tester.pump();

    expect(find.text('Please enter your name'), findsOneWidget);
    expect(find.text('Please enter your email'), findsOneWidget);
    expect(find.text('Please enter a password'), findsOneWidget);
  });
}