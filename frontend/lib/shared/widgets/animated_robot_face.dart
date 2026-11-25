import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// Sparky's face, alive: it floats up and down, looks left and right,
/// and blinks. Like the face on the robot's own screen.
///
/// Same look as [RobotFace] with the "ready" expression (o o eyes and a smile),
/// so it matches Home. Used at the top of Sign Up and Log In.
class AnimatedRobotFace extends StatefulWidget {
  const AnimatedRobotFace({
    super.key,
    this.width = 185,
    this.animate = true,
    this.float = true,
  });

  /// The card is 272 x 188 in Figma; this scales it.
  final double width;

  /// False shows the still face (for tests or screenshots).
  final bool animate;

  /// False keeps the card still (the eyes still move and blink). Used under a hat,
  /// so the hat doesn't look like it floats off the head.
  final bool float;

  @override
  State<AnimatedRobotFace> createState() => _AnimatedRobotFaceState();
}

class _AnimatedRobotFaceState extends State<AnimatedRobotFace>
    with SingleTickerProviderStateMixin {
  /// One loop of the whole animation: look around, blink twice, float.
  static const _loop = Duration(seconds: 6);

  late final AnimationController _controller =
      AnimationController(vsync: this, duration: _loop);

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Respect the phone's "remove animations" setting.
    final reduceMotion = MediaQuery.maybeDisableAnimationsOf(context) ?? false;
    if (widget.animate && !reduceMotion) {
      if (!_controller.isAnimating) _controller.repeat();
    } else {
      _controller.stop();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final height = widget.width * 188 / 272;

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        final t = _controller.value; // 0.0 -> 1.0, then again
        final pose = _FacePose.at(t);

        return Transform.translate(
          offset: Offset(0, widget.float ? pose.floatY * widget.width / 272 : 0),
          child: SizedBox(
            width: widget.width,
            height: height,
            child: FittedBox(
              child: Container(
                width: 272,
                height: 188,
                decoration: BoxDecoration(
                  color: AppColors.field,
                  borderRadius: BorderRadius.circular(28),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.primary.withValues(alpha: 0.35),
                      blurRadius: 32,
                      spreadRadius: 2,
                    ),
                  ],
                ),
                alignment: Alignment.center,
                child: CustomPaint(
                  size: const Size(228, 150),
                  painter: _AliveFacePainter(pose),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

/// Where everything is at one moment of the loop.
class _FacePose {
  const _FacePose({
    required this.floatY,
    required this.lookX,
    required this.eyeOpen,
  });

  /// Up/down float of the whole card, in Figma pixels (-4 .. 4).
  final double floatY;

  /// Eyes left/right, in Figma pixels (-10 .. 10).
  final double lookX;

  /// 1 = eyes open, 0 = closed (blink).
  final double eyeOpen;

  /// The script of one 6-second loop (t goes 0 -> 1):
  ///   0.00-0.20 look ahead       0.15 blink
  ///   0.20-0.30 look left        0.30-0.45 hold
  ///   0.45-0.55 look right       0.55-0.70 hold
  ///   0.70-0.80 back to center   0.90 blink
  /// The card floats up and down twice per loop.
  factory _FacePose.at(double t) {
    final floatY = math.sin(t * 2 * math.pi * 2) * 4;

    double lookX;
    if (t < 0.20) {
      lookX = 0;
    } else if (t < 0.30) {
      lookX = _ease(0, -10, (t - 0.20) / 0.10);
    } else if (t < 0.45) {
      lookX = -10;
    } else if (t < 0.55) {
      lookX = _ease(-10, 10, (t - 0.45) / 0.10);
    } else if (t < 0.70) {
      lookX = 10;
    } else if (t < 0.80) {
      lookX = _ease(10, 0, (t - 0.70) / 0.10);
    } else {
      lookX = 0;
    }

    final double eyeOpen =
        1.0 - math.max<double>(_blink(t, 0.15), _blink(t, 0.90));

    return _FacePose(floatY: floatY, lookX: lookX, eyeOpen: eyeOpen);
  }

  /// Smooth move from [a] to [b] as [p] goes 0 -> 1.
  static double _ease(double a, double b, double p) =>
      a + (b - a) * Curves.easeInOut.transform(p.clamp(0.0, 1.0));

  /// 0 most of the time, rises to 1 (closed) around [at], back to 0. Lasts ~0.2 s.
  static double _blink(double t, double at) {
    const half = 0.018; // 0.018 of 6 s = about 0.1 s to close, 0.1 s to open
    final d = (t - at).abs();
    return d >= half ? 0.0 : 1.0 - d / half;
  }
}

class _AliveFacePainter extends CustomPainter {
  _AliveFacePainter(this.pose);

  final _FacePose pose;

  static const _screen = Color(0xFF0F1030);
  static const _mouth = Color(0xFF38BDF8);

  @override
  void paint(Canvas canvas, Size size) {
    // The screen: dark rounded rectangle with a blue border (same as RobotFace).
    final screen = RRect.fromRectAndRadius(
      Offset.zero & size,
      const Radius.circular(24),
    );
    canvas.drawRRect(screen, Paint()..color = _screen);
    canvas.drawRRect(
      screen.deflate(1.5),
      Paint()
        ..color = AppColors.primary
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3,
    );

    final eyeStroke = Paint()
      ..color = AppColors.primary
      ..style = PaintingStyle.stroke
      ..strokeWidth = 9
      ..strokeCap = StrokeCap.round;

    _eye(canvas, Offset(57 + pose.lookX, 68), eyeStroke);
    _eye(canvas, Offset(171 + pose.lookX, 68), eyeStroke);

    // The smile follows the eyes a little, so the whole face turns.
    canvas.drawArc(
      Rect.fromCircle(center: Offset(114 + pose.lookX * 0.4, 92), radius: 18),
      0,
      math.pi,
      false,
      Paint()
        ..color = _mouth
        ..style = PaintingStyle.stroke
        ..strokeWidth = 8
        ..strokeCap = StrokeCap.round,
    );
  }

  /// An "o" eye. While blinking it squashes into a flat line.
  void _eye(Canvas canvas, Offset center, Paint stroke) {
    const radius = 20.0;
    if (pose.eyeOpen < 0.2) {
      canvas.drawLine(
        center.translate(-radius, 0),
        center.translate(radius, 0),
        stroke,
      );
      return;
    }
    canvas.drawOval(
      Rect.fromCenter(
        center: center,
        width: radius * 2,
        height: radius * 2 * pose.eyeOpen,
      ),
      stroke,
    );
  }

  @override
  bool shouldRepaint(_AliveFacePainter old) =>
      old.pose.lookX != pose.lookX || old.pose.eyeOpen != pose.eyeOpen;
}