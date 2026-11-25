import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:kaboom_app/core/router/app_router.dart';
import 'package:kaboom_app/features/splash/screens/splash_screen.dart';

/// A small router like the app's: the same splash-first rule, the splash,
/// and stand-in Sign Up and Log In pages to land on.
GoRouter _router({String initial = AppRoutes.splash}) => GoRouter(
      initialLocation: initial,
      redirect: splashFirst,
      routes: [
        GoRoute(
          path: AppRoutes.splash,
          builder: (context, state) => SplashScreen(
            duration: const Duration(milliseconds: 800),
            next: state.uri.queryParameters['next'],
          ),
        ),
        GoRoute(
          path: AppRoutes.signUp,
          builder: (context, state) => const Scaffold(body: Text('sign up page')),
        ),
        GoRoute(
          path: AppRoutes.login,
          builder: (context, state) => const Scaffold(body: Text('log in page')),
        ),
      ],
    );

/// Waits for the 800 ms splash and the page change after it.
Future<void> _finishSplash(WidgetTester tester) async {
  await tester.pump(const Duration(milliseconds: 900));
  await tester.pump(const Duration(milliseconds: 500));
}

void main() {
  // Each test is a fresh app start.
  setUp(() => SplashScreen.shown = false);

  testWidgets('shows Sparky, KABOOM! and the tagline', (tester) async {
    await tester.pumpWidget(MaterialApp.router(routerConfig: _router()));
    await tester.pump(const Duration(milliseconds: 300));

    expect(find.text('KABOOM!'), findsOneWidget);
    expect(find.text('Focus with Sparky'), findsOneWidget);

    // Let the timer finish so the test ends cleanly.
    await _finishSplash(tester);
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

  testWidgets('opening the app on another page shows the splash first, then that page',
      (tester) async {
    // Like reloading https://<site>/#/login
    await tester.pumpWidget(
        MaterialApp.router(routerConfig: _router(initial: AppRoutes.login)));
    await tester.pump(const Duration(milliseconds: 300));
    expect(find.text('KABOOM!'), findsOneWidget);
    expect(find.text('log in page'), findsNothing);

    await _finishSplash(tester);
    expect(find.text('log in page'), findsOneWidget);
    expect(find.text('KABOOM!'), findsNothing);
  });

  testWidgets('after the splash, moving between pages never shows it again',
      (tester) async {
    final router = _router();
    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await _finishSplash(tester);

    router.go(AppRoutes.login);
    await tester.pump(const Duration(milliseconds: 500));
    expect(find.text('log in page'), findsOneWidget);
    expect(find.text('KABOOM!'), findsNothing);
  });

  testWidgets('an address outside the app goes to sign up instead', (tester) async {
    await tester.pumpWidget(MaterialApp.router(
        routerConfig: _router(initial: '/splash?next=https://example.com')));
    await _finishSplash(tester);

    expect(find.text('sign up page'), findsOneWidget);
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

    await _finishSplash(tester);
  });
}
