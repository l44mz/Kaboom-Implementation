import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/animated_robot_face.dart';

/// Sparky's moving face + title + subtitle at the top of Sign Up and Login.
class AuthHeader extends StatelessWidget {
  const AuthHeader({super.key, required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // 185 wide = 128 high, the same height as the old Robot.png.
        // The padding leaves room for the glow and the float, so nothing is cut.
        const Padding(
          padding: EdgeInsets.symmetric(vertical: 8),
          child: Center(child: AnimatedRobotFace(width: 185)),
        ),
        const SizedBox(height: 16),
        Text(title,
            textAlign: TextAlign.center,
            style: const TextStyle(
                fontSize: 20, height: 41 / 34, fontWeight: FontWeight.w700,
                color: AppColors.textPrimary)),
        const SizedBox(height: 4),
        Text(subtitle,
            textAlign: TextAlign.center,
            style: const TextStyle(
                fontSize: 14, height: 21 / 16, fontWeight: FontWeight.w300,
                color: AppColors.textMuted)),
      ],
    );
  }
}