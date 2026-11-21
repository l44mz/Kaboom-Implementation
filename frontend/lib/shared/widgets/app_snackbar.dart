import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// Shows a short message at the bottom of the screen. Green for success, red for errors.
void showAppSnackBar(
  BuildContext context,
  String text, {
  bool isError = false,
}) {
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(text),
      backgroundColor: isError ? AppColors.away : AppColors.focused,
    ),
  );
}
