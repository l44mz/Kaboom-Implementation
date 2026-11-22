import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../controllers/outfit_controller.dart';
import 'robot_face.dart';

/// Sparky's face wearing what the user equipped in the Store:
///   - hat:    its picture on top of the head
///   - screen: a round badge on the bottom-left corner
///   - effect: a round badge on the bottom-right corner (Kaboom FX)
/// With nothing equipped it looks exactly like [RobotFace].
class DressedRobot extends StatelessWidget {
  const DressedRobot({
    super.key,
    required this.expression,
    this.width = 272,
    this.glow = false,
    this.hat,
    this.screen,
    this.effect,
  });

  final RobotExpression expression;
  final double width;
  final bool glow;
  final WornItem? hat;
  final WornItem? screen;
  final WornItem? effect;

  @override
  Widget build(BuildContext context) {
    final faceHeight = width * 188 / 272;
    final hatSize = width * 0.42;
    final badge = width * 0.22;

    // Room above the face for the hat (it overlaps the head by a third),
    // and below for half of the badges.
    final top = hat == null ? 0.0 : hatSize * 0.66;
    final bottom = (screen == null && effect == null) ? 0.0 : badge / 2;

    return SizedBox(
      width: width,
      height: top + faceHeight + bottom,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Positioned(
            top: top,
            left: 0,
            child: RobotFace(expression: expression, width: width, glow: glow),
          ),
          if (hat != null)
            Positioned(
              top: 0,
              left: (width - hatSize) / 2,
              width: hatSize,
              height: hatSize,
              child: _Picture(item: hat!, fallback: Icons.school),
            ),
          if (screen != null)
            Positioned(
              top: top + faceHeight - badge / 2,
              left: width * 0.04,
              child: _Badge(item: screen!, size: badge, fallback: Icons.tv),
            ),
          if (effect != null)
            Positioned(
              top: top + faceHeight - badge / 2,
              right: width * 0.04,
              child: _Badge(item: effect!, size: badge, fallback: Icons.auto_awesome),
            ),
        ],
      ),
    );
  }
}

/// The item's picture, or an icon if the admin didn't upload one.
class _Picture extends StatelessWidget {
  const _Picture({required this.item, required this.fallback});

  final WornItem item;
  final IconData fallback;

  @override
  Widget build(BuildContext context) {
    final image = item.image;
    if (image != null) return Image.memory(image, fit: BoxFit.contain);
    return FittedBox(child: Icon(fallback, color: AppColors.coin));
  }
}

/// A round badge with the item's picture, sitting on a corner of the face.
class _Badge extends StatelessWidget {
  const _Badge({required this.item, required this.size, required this.fallback});

  final WornItem item;
  final double size;
  final IconData fallback;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      padding: EdgeInsets.all(size * 0.12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        shape: BoxShape.circle,
        border: Border.all(color: AppColors.primary, width: 2),
      ),
      child: ClipOval(child: _Picture(item: item, fallback: fallback)),
    );
  }
}