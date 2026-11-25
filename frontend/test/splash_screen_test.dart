import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:kaboom_app/core/router/app_router.dart';
import 'package:kaboom_app/features/splash/screens/splash_screen.dart';

/// A small router: the splash, and a stand-in Sign Up page to land on.
GoRouter _router() => GoRouter(
      initialLocation: AppRoutes.splash,
      routes: [
        GoRoute(
          path: AppRoutes.splash,
          builder: (context, state) =>
              const SplashScreen(duration: Duration(milliseconds: 800)),
        ),
        GoRoute(
          path: AppRoutes.signUp,
          builder: (context, state) => const Scaffold(body: Text('sign up page')),
        ),
      ],
    );

void main() {
  testWidgets('shows Sparky, KABOOM! and the tagline', (tester) async {
    await tester.pumpWidget(MaterialApp.router(routerConfig: _router()));
    await tester.pump(const Duration(milliseconds: 300));

    expect(find.text('KABOOM!'), findsOneWidget);
    expect(find.text('Focus with Sparky'), findsOneWidget);

    // Let the timer finish so the test ends cleanly.
    await tester.pump(const Duration(seconds: 1));
  });

  testWidgets('goes to sign up by itself after the duration', (tester) async {
    await tester.pumpWidget(MaterialApp.router(routerConfig: _router()));

    await tester.pump(const Duration(milliseconds: 700));
    expect(find.text('sign up page'), findsNothing);

    await tester.pump(const Duration(milliseconds: 200)); // timer fires at 800 ms
    await tester.pump(const Duration(milliseconds: 500)); // page change
    expect(find.text('sign up page'), findsOneWidget);
    expect(find.text('KABOOM!'), findsNothing);
  });

  testWidgets('with animations turned off, everything shows at once', (tester) async {
    await tester.pumpWidget(MediaQuery(
      data: const MediaQueryData(disableAnimations: true),
      child: MaterialApp.router(routerConfig: _router()),
    ));
    await tester.pump();

    final fades = tester.widgetList<FadeTransition>(find.descendant(
        of: find.byType(SplashScreen), matching: find.byType(FadeTransition)));
    expect(fades.every((f) => f.opacity.value == 1), isTrue);

    await tester.pump(const Duration(seconds: 1));
  });
}
