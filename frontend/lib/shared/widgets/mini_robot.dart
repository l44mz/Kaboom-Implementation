import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// What Sparky wears in a small picture: a hat-type accessory, a screen colour, and a glow.
enum RobotAccessory { none, antenna, cap, crown, headphones }

class RobotLook {
  const RobotLook({
    this.accessory = RobotAccessory.none,
    this.screenColor = AppColors.primary,
    this.glowColor,
  });

  final RobotAccessory accessory;
  final Color screenColor;

  /// Kaboom FX items make Sparky glow in this colour.
  final Color? glowColor;

  /// Picks the look from a shop item's name and category (hat, screen or effect).
  /// The shop has no pictures yet, so the name decides, e.g. "Gold Crown" draws a crown.
  factory RobotLook.forItem(String name, String category) {
    final n = name.toLowerCase();
    switch (category) {
      case 'hat':
        if (n.contains('antenna')) return const RobotLook(accessory: RobotAccessory.antenna);
        if (n.contains('crown')) return const RobotLook(accessory: RobotAccessory.crown);
        if (n.contains('headphone')) return const RobotLook(accessory: RobotAccessory.headphones);
        return const RobotLook(accessory: RobotAccessory.cap);
      case 'screen':
        if (n.contains('ice')) return const RobotLook(screenColor: Color(0xFF38BDF8));
        if (n.contains('sunset')) return const RobotLook(screenColor: Color(0xFFF5B93A));
        if (n.contains('mint') || n.contains('green')) {
          return const RobotLook(screenColor: AppColors.focused);
        }
        if (n.contains('rose') || n.contains('pink')) {
          return const RobotLook(screenColor: Color(0xFFF472B6));
        }
        return const RobotLook(screenColor: AppColors.secondary);
      case 'effect':
        if (n.contains('fire') || n.contains('flame')) {
          return const RobotLook(glowColor: Color(0xFFFF7A3D));
        }
        if (n.contains('spark') || n.contains('kaboom') || n.contains('star')) {
          return const RobotLook(glowColor: Color(0xFFF5C542));
        }
        return const RobotLook(glowColor: AppColors.secondary);
      default:
        return const RobotLook();
    }
  }
}

/// A small Sparky head (O O and a smile) wearing [look]. Used on shop cards and the buy dialog.
class MiniRobot extends StatelessWidget {
  const MiniRobot({super.key, this.look = const RobotLook(), this.size = 96});

  final RobotLook look;
  final double size;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size * 110 / 120,
      child: FittedBox(
        child: CustomPaint(
          size: const Size(120, 110),
          painter: _MiniRobotPainter(look),
        ),
      ),
    );
  }
}

class _MiniRobotPainter extends CustomPainter {
  _MiniRobotPainter(this.look);

  final RobotLook look;

  static const _head = Color(0xFF1E2050);
  static const _screen = Color(0xFF0F1030);
  static const _purple = Color(0xFF8B5CF6);
  static const _gold = Color(0xFFF5C542);
  static const _antennaTip = Color(0xFF38BDF8);

  // The head's box inside the 120 x 110 canvas.
  static const _headRect = Rect.fromLTWH(18, 40, 84, 64);

  @override
  void paint(Canvas canvas, Size size) {
    final head = RRect.fromRectAndRadius(_headRect, const Radius.circular(18));

    // Glow: always a soft blue one, stronger and coloured for Kaboom FX.
    final glow = look.glowColor ?? AppColors.primary;
    canvas.drawRRect(
      head.inflate(look.glowColor == null ? 2.0 : 6.0),
      Paint()
        ..color = glow.withValues(alpha: look.glowColor == null ? 0.25 : 0.55)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 10),
    );

    // Headphone band goes behind the head.
    if (look.accessory == RobotAccessory.headphones) _headphoneBand(canvas);

    // Head and screen.
    canvas.drawRRect(head, Paint()..color = _head);
    final screen = RRect.fromRectAndRadius(_headRect.deflate(9), const Radius.circular(11));
    canvas.drawRRect(screen, Paint()..color = _screen);
    canvas.drawRRect(
      screen,
      Paint()
        ..color = look.screenColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.5,
    );

    // Face: O O and a smile.
    final line = Paint()
      ..color = look.screenColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.8
      ..strokeCap = StrokeCap.round;
    canvas.drawCircle(const Offset(46, 66), 7, line);
    canvas.drawCircle(const Offset(74, 66), 7, line);
    canvas.drawArc(
      Rect.fromCircle(center: const Offset(60, 76), radius: 8),
      0.25,
      math.pi - 0.5,
      false,
      line,
    );

    switch (look.accessory) {
      case RobotAccessory.antenna:
        _antenna(canvas);
      case RobotAccessory.cap:
        _cap(canvas);
      case RobotAccessory.crown:
        _crown(canvas);
      case RobotAccessory.headphones:
        _earCups(canvas);
      case RobotAccessory.none:
        break;
    }
  }

  void _antenna(Canvas canvas) {
    canvas.drawLine(
      const Offset(60, 40),
      const Offset(60, 22),
      Paint()
        ..color = AppColors.border
        ..strokeWidth = 3
        ..strokeCap = StrokeCap.round,
    );
    canvas.drawCircle(const Offset(60, 18), 5, Paint()..color = _antennaTip);
  }

  void _cap(Canvas canvas) {
    final paint = Paint()..color = _purple;
    // Dome
    canvas.drawArc(const Rect.fromLTWH(30, 18, 60, 46), math.pi, math.pi, true, paint);
    // Brim to the right
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(60, 37, 44, 7), const Radius.circular(4)),
      paint,
    );
  }

  void _crown(Canvas canvas) {
    final path = Path()
      ..moveTo(42, 40)
      ..lineTo(40, 20)
      ..lineTo(50, 30)
      ..lineTo(60, 14)
      ..lineTo(70, 30)
      ..lineTo(80, 20)
      ..lineTo(78, 40)
      ..close();
    canvas.drawPath(path, Paint()..color = _gold);
    canvas.drawCircle(const Offset(60, 14), 3, Paint()..color = _gold);
  }

  void _headphoneBand(Canvas canvas) {
    canvas.drawArc(
      const Rect.fromLTWH(10, 20, 100, 84),
      math.pi * 1.05,
      math.pi * 0.9,
      false,
      Paint()
        ..color = const Color(0xFF5B6CFF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 6
        ..strokeCap = StrokeCap.round,
    );
  }

  void _earCups(Canvas canvas) {
    final paint = Paint()..color = _purple;
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(6, 56, 14, 28), const Radius.circular(6)),
      paint,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(100, 56, 14, 28), const Radius.circular(6)),
      paint,
    );
  }

  @override
  bool shouldRepaint(_MiniRobotPainter old) =>
      old.look.accessory != look.accessory ||
      old.look.screenColor != look.screenColor ||
      old.look.glowColor != look.glowColor;
}
