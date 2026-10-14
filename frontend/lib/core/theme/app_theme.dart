import 'package:flutter/material.dart';

import 'app_colors.dart';

class AppTheme {
  static ThemeData get light => ThemeData(
    useMaterial3: true,
    fontFamily: 'ReadexPro',
    colorScheme: ColorScheme.fromSeed(seedColor: AppColors.primary),
    scaffoldBackgroundColor: AppColors.background,
  );

  static const timerStyle = TextStyle(
    fontFamily: 'JetBrainsMono',
    fontSize: 48,
  );
  static const numberStyle = TextStyle(
    fontFamily: 'JetBrainsMono',
    fontSize: 16,
  );
}
