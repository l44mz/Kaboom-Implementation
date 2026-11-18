import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// Sparky's expressions, from the Figma "Robot faces" component.
enum RobotExpression { focused, distracted, away, happy }

/// Sparky's face in its rounded card. Figma size is 272 x 188; [width] scales it.
class RobotFace extends StatelessWidget {
  const RobotFace({
    super.key,
    required this.expression,
    this.width = 272,
    this.glow = false,
  });

  final RobotExpression expression;
  final double width;

  /// The soft blue glow around the card (Figma "Session Complete").
  final bool glow;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: width,
      height: width * 188 / 272,
      child: FittedBox(
        child: Container(
          width: 272,
          height: 188,
          decoration: BoxDecoration(
            color: AppColors.field,
            borderRadius: BorderRadius.circular(28),
            boxShadow: glow
                ? [
                    BoxShadow(
                      color: AppColors.primary.withValues(alpha: 0.35),
                      blurRadius: 32,
                      spreadRadius: 2,
                    ),
                  ]
                : null,
          ),
          alignment: Alignment.center,
          child: CustomPaint(
            size: const Size(228, 150),
            painter: _FacePainter(expression),
          ),
        ),
      ),
    );
  }
}

class _FacePainter extends CustomPainter {
  _FacePainter(this.expression);

  final RobotExpression expression;

  static const _screen = Color(0xFF0F1030);
  static const _mouth = Color(0xFF38BDF8);

  @override
  void paint(Canvas canvas, Size size) {
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

    final eye = Paint()..color = AppColors.primary;
    final mouth = Paint()..color = _mouth;

    switch (expression) {
      case RobotExpression.focused:
        _bar(canvas, const Rect.fromLTWH(33, 58, 48, 14), eye);
        _bar(canvas, const Rect.fromLTWH(144, 58, 48, 14), eye);
        _bar(canvas, const Rect.fromLTWH(88, 92, 51, 10), mouth);
      case RobotExpression.distracted:
        final stroke = _stroke(AppColors.primary, 10);
        canvas.drawPath(
          Path()
            ..moveTo(32, 47)
            ..lineTo(74, 65)
            ..lineTo(32, 83),
          stroke,
        );
        canvas.drawPath(
          Path()
            ..moveTo(196, 47)
            ..lineTo(154, 65)
            ..lineTo(196, 83),
          stroke,
        );
        _arc(canvas, const Offset(117, 104), 22, math.pi, _stroke(_mouth, 7));
      case RobotExpression.away:
        _bar(canvas, const Rect.fromLTWH(36, 64, 48, 14), eye);
        _bar(canvas, const Rect.fromLTWH(144, 64, 48, 14), eye);
        canvas.drawCircle(const Offset(114, 98), 16, mouth);
        canvas.drawCircle(const Offset(114, 98), 5, Paint()..color = _screen);
      case RobotExpression.happy:
        // ^ ^ eyes (top half circles) and a smile (bottom half circle).
        final eyes = _stroke(AppColors.primary, 9);
        _arc(canvas, const Offset(57, 70), 20, math.pi, eyes);
        _arc(canvas, const Offset(171, 70), 20, math.pi, eyes);
        _arc(canvas, const Offset(114, 80), 22, 0, _stroke(_mouth, 8));
    }
  }

  Paint _stroke(Color color, double width) => Paint()
    ..color = color
    ..style = PaintingStyle.stroke
    ..strokeWidth = width
    ..strokeCap = StrokeCap.round
    ..strokeJoin = StrokeJoin.round;

  /// Half circle starting at [start] (pi = top half, 0 = bottom half).
  void _arc(Canvas canvas, Offset center, double radius, double start, Paint paint) {
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      start,
      math.pi,
      false,
      paint,
    );
  }

  void _bar(Canvas canvas, Rect rect, Paint paint) {
    canvas.drawRRect(
      RRect.fromRectAndRadius(rect, Radius.circular(rect.height / 2)),
      paint,
    );
  }

  @override
  bool shouldRepaint(_FacePainter old) => old.expression != expression;
}
