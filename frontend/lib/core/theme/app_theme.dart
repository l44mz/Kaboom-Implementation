import 'package:flutter/material.dart';

import 'app_colors.dart';

class AppTheme {
  
  static ThemeData get light => ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        fontFamily: 'ReadexPro',
        colorScheme: ColorScheme.fromSeed(
          seedColor: AppColors.primary,
          brightness: Brightness.dark,
          primary: AppColors.primary,
          surface: AppColors.background,
        ),
        scaffoldBackgroundColor: AppColors.background,
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: AppColors.field,
          labelStyle: const TextStyle(color: AppColors.textMuted),
          hintStyle: const TextStyle(
            color: AppColors.textMuted,
            fontWeight: FontWeight.w300,
          ),
          prefixIconColor: AppColors.textMuted,
          enabledBorder: _border(AppColors.primary),
          focusedBorder: _border(AppColors.secondary, width: 2),
          errorBorder: _border(AppColors.away),
          focusedErrorBorder: _border(AppColors.away, width: 2),
        ),
      );

  static OutlineInputBorder _border(Color color, {double width = 1}) =>
      OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: color, width: width),
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