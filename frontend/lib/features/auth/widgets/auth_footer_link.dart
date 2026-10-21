import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// "Already have an account? Login here" / "Don't have an account? Sign Up"
class AuthFooterLink extends StatelessWidget {
  const AuthFooterLink({
    super.key,
    required this.question,
    required this.action,
    required this.onTap,
  });

  final String question;
  final String action;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(question, style: const TextStyle(fontSize: 13, color: AppColors.textMuted)),
        TextButton(
          onPressed: onTap,
          child: Text(action,
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
        ),
      ],
    );
  }
}