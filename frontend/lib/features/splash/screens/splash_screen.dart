import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/animated_robot_face.dart';

/// The first screen every time the app opens: Sparky pops in, "KABOOM!" and the
/// tagline slide up, three dots pulse, then the app goes to [next] (or Sign Up).
class SplashScreen extends StatefulWidget {
  const SplashScreen({
    super.key,
    this.duration = const Duration(milliseconds: 2400),
    this.next,
  });

  /// True once the splash has been shown in this run of the app, so the router
  /// stops sending pages here (see splashFirst in app_router.dart).
  static bool shown = false;

  /// How long the splash stays before moving on.
  final Duration duration;

  /// The page the app was opened on (e.g. "/home"); null or unknown means Sign Up.
  final String? next;

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with TickerProviderStateMixin {
  /// Plays once: Sparky pops in (0 to 0.7 s), then the text (0.4 to 1 s).
  late final AnimationController _intro =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 1000));

  /// Loops forever: the three dots pulse one after the other.
  late final AnimationController _dots =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 1200));

  // Sparky grows from 60 % to full size with a small bounce, and fades in.
  late final Animation<double> _faceScale = Tween(begin: 0.6, end: 1.0).animate(
      CurvedAnimation(parent: _intro, curve: const Interval(0, 0.7, curve: Curves.easeOutBack)));
  late final Animation<double> _faceFade =
      CurvedAnimation(parent: _intro, curve: const Interval(0, 0.5, curve: Curves.easeOut));

  // The text fades in and slides up a little, after Sparky.
  late final Animation<double> _textFade =
      CurvedAnimation(parent: _intro, curve: const Interval(0.4, 1, curve: Curves.easeOut));
  late final Animation<Offset> _textSlide =
      Tween(begin: const Offset(0, 0.3), end: Offset.zero).animate(_textFade);

  Timer? _timer;

  @override
  void initState() {
    super.initState();
    SplashScreen.shown = true;
    _timer = Timer(widget.duration, _goNext);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Respect the phone's "remove animations" setting: show everything at once.
    final reduceMotion = MediaQuery.maybeDisableAnimationsOf(context) ?? false;
    if (reduceMotion) {
      _intro.value = 1;
      _dots.stop();
    } else {
      if (!_intro.isAnimating && _intro.value == 0) _intro.forward();
      if (!_dots.isAnimating) _dots.repeat();
    }
  }

  void _goNext() {
    if (!mounted) return;
    // Only pages inside the app ("/..."), and never the splash again.
    final next = widget.next;
    final ok = next != null &&
        next.startsWith('/') &&
        !next.startsWith('//') &&
        !next.startsWith(AppRoutes.splash);
    context.go(ok ? next : AppRoutes.signUp);
  }

  @override
  void dispose() {
    _timer?.cancel();
    _intro.dispose();
    _dots.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            FadeTransition(
              opacity: _faceFade,
              child: ScaleTransition(
                scale: _faceScale,
                child: const AnimatedRobotFace(width: 220),
              ),
            ),
            const SizedBox(height: 32),
            FadeTransition(
              opacity: _textFade,
              child: SlideTransition(
                position: _textSlide,
                child: const Column(
                  children: [
                    Text('KABOOM!',
                        style: TextStyle(
                            fontSize: 40,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 2,
                            color: AppColors.textPrimary)),
                    SizedBox(height: 6),
                    Text('Focus with Sparky',
                        style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w300,
                            color: AppColors.textMuted)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 40),
            _LoadingDots(controller: _dots),
          ],
        ),
      ),
    );
  }
}

/// Three dots that grow and brighten one after the other.
class _LoadingDots extends StatelessWidget {
  const _LoadingDots({required this.controller});

  final AnimationController controller;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, _) => Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 0; i < 3; i++) _dot(i),
        ],
      ),
    );
  }

  Widget _dot(int index) {
    // Each dot peaks a third of a loop after the one before it.
    final t = (controller.value - index / 3) % 1;
    final pulse = t < 0.5 ? t * 2 : (1 - t) * 2; // 0 -> 1 -> 0
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 5),
      width: 8 + 4 * pulse,
      height: 8 + 4 * pulse,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: Color.lerp(AppColors.border, AppColors.secondary, pulse),
      ),
    );
  }
}
