import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// Robot image + title + subtitle at the top of Sign Up and Login.
class AuthHeader extends StatelessWidget {
  const AuthHeader({super.key, required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Image.asset('assets/images/Robot.png', height: 128),
        const SizedBox(height: 24),
        Text(title,
            textAlign: TextAlign.center,
            style: const TextStyle(
                fontSize: 34, height: 41 / 34, fontWeight: FontWeight.w700,
                color: AppColors.textPrimary)),
        const SizedBox(height: 4),
        Text(subtitle,
            textAlign: TextAlign.center,
            style: const TextStyle(
                fontSize: 16, height: 21 / 16, fontWeight: FontWeight.w300,
                color: AppColors.textMuted)),
      ],
    );
  }
}