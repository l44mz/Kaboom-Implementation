import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';

/// The Home screen timer: a clock face the user drags to pick the minutes.
///
/// One full turn is 60 minutes, like a real clock. The value snaps every 5 minutes
/// (5, 10, ... 60), so it is easy to land on a number with a finger.
/// Drag the white knob around the ring, or tap a spot on the ring to jump there.
class DurationDial extends StatefulWidget {
  const DurationDial({
    super.key,
    required this.minutes,
    required this.onChanged,
    this.enabled = true,
  });

  final int minutes;
  final ValueChanged<int> onChanged;

  /// false while a session is starting, so the time can't change under the user.
  final bool enabled;

  static const minMinutes = 5;
  static const maxMinutes = 60;
  static const step = 5;
  static const size = 260.0;

  /// Room around the ring so the knob is not cut off at the edge.
  static const _edge = 8.0;
  static const _stroke = 18.0;
  static double get _ringRadius => size / 2 - _edge - _stroke / 2;

  /// Turns a touch point into minutes. [previous] keeps a drag from jumping
  /// across the top of the clock (from 60 straight to 5, or back).
  /// Returns null for a touch too close to the middle, where the angle means nothing.
  static int? minutesAt(Offset local, {required int previous, bool dragging = false}) {
    const center = Offset(size / 2, size / 2);
    final d = local - center;
    if (d.distance < size * 0.15) return null;

    // Angle from 12 o'clock, going clockwise, 0 to 2*pi.
    var angle = math.atan2(d.dx, -d.dy);
    if (angle < 0) angle += 2 * math.pi;

    var value = (angle / (2 * math.pi) * maxMinutes / step).round() * step;
    if (value == 0) value = previous >= maxMinutes / 2 ? maxMinutes : minMinutes;
    value = value.clamp(minMinutes, maxMinutes);

    if (dragging) {
      // Crossing 12 o'clock while dragging stops at the end instead of wrapping.
      if (previous >= 45 && value <= 15) return maxMinutes;
      if (previous <= 15 && value >= 45) return minMinutes;
    }
    return value;
  }

  @override
  State<DurationDial> createState() => _DurationDialState();
}

class _DurationDialState extends State<DurationDial> {
  /// The last value sent out. A fast drag can send several moves before the
  /// screen redraws with the new [DurationDial.minutes], so compare with this.
  late int _last = widget.minutes;

  @override
  void didUpdateWidget(DurationDial old) {
    super.didUpdateWidget(old);
    _last = widget.minutes;
  }

  void _update(Offset local, {required bool dragging}) {
    if (!widget.enabled) return;
    final value = DurationDial.minutesAt(local, previous: _last, dragging: dragging);
    if (value == null || value == _last) return;
    _last = value;
    HapticFeedback.selectionClick();
    widget.onChanged(value);
  }

  void _nudge(int by) {
    if (!widget.enabled) return;
    final value = (_last + by).clamp(DurationDial.minMinutes, DurationDial.maxMinutes);
    if (value == _last) return;
    _last = value;
    widget.onChanged(value);
  }

  @override
  Widget build(BuildContext context) {
    const size = DurationDial.size;
    const step = DurationDial.step;
    final minutes = widget.minutes;
    final enabled = widget.enabled;
    final ringRadius = DurationDial._ringRadius;
    return Semantics(
      label: 'Session length',
      value: '$minutes minutes',
      increasedValue: '${math.min(minutes + step, DurationDial.maxMinutes)} minutes',
      decreasedValue: '${math.max(minutes - step, DurationDial.minMinutes)} minutes',
      onIncrease: enabled ? () => _nudge(step) : null,
      onDecrease: enabled ? () => _nudge(-step) : null,
      slider: true,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapUp: (d) => _update(d.localPosition, dragging: false),
        onPanStart: (d) => _update(d.localPosition, dragging: false),
        onPanUpdate: (d) => _update(d.localPosition, dragging: true),
        child: SizedBox(
          width: size,
          height: size,
          child: Stack(
            alignment: Alignment.center,
            children: [
              CustomPaint(
                size: const Size.square(size),
                painter: _DialPainter(
                  minutes: minutes,
                  ringRadius: ringRadius,
                  stroke: DurationDial._stroke,
                  enabled: enabled,
                ),
              ),
              // The time in the middle. FittedBox keeps it inside the ring.
              SizedBox(
                width: ringRadius * 1.4,
                height: ringRadius * 1.4,
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '$minutes:00',
                        style: AppTheme.timerStyle.copyWith(
                          fontSize: 56,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        'Sparky is waiting\nfor you',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textPrimary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Draws the ring, the filled part and the knob.
class _DialPainter extends CustomPainter {
  _DialPainter({
    required this.minutes,
    required this.ringRadius,
    required this.stroke,
    required this.enabled,
  });

  final int minutes;
  final double ringRadius;
  final double stroke;
  final bool enabled;

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final rect = Rect.fromCircle(center: center, radius: ringRadius);
    final sweep = 2 * math.pi * minutes / DurationDial.maxMinutes;

    // The empty ring.
    canvas.drawCircle(
      center,
      ringRadius,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke
        ..color = const Color(0xFF1F1F2A),
    );

    // The filled part, from 12 o'clock clockwise.
    canvas.drawArc(
      rect,
      -math.pi / 2,
      sweep,
      false,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke
        ..strokeCap = StrokeCap.round
        ..color = enabled ? AppColors.secondary : AppColors.border,
    );

    // The knob at the end of the filled part: the thing to drag.
    final knobAngle = sweep - math.pi / 2;
    final knob = center + Offset(math.cos(knobAngle), math.sin(knobAngle)) * ringRadius;
    canvas.drawCircle(
      knob,
      stroke * 0.85,
      Paint()..color = Colors.black.withValues(alpha: 0.25),
    );
    canvas.drawCircle(
      knob,
      stroke * 0.75,
      Paint()..color = enabled ? AppColors.textPrimary : AppColors.textMuted,
    );
  }

  @override
  bool shouldRepaint(_DialPainter old) =>
      old.minutes != minutes || old.enabled != enabled;
}
