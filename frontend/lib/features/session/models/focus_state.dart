import 'package:flutter/material.dart';

/// What the robot sees during a session (Stage 3: FOCUSED, DISTRACTED, AWAY).
enum FocusState {
  focused(
    label: 'Focused',
    message: "Sparky's counting on you!",
    chipColor: Color(0xFF6D8CFF),
    chipBorder: Color(0xFF4F6BFF),
  ),
  distracted(
    label: 'Distracted',
    message: '3... 2... get back\nto work!',
    chipColor: Color(0xFFAA91FD),
    chipBorder: Color(0xFF8B5CF6),
  ),
  away(
    label: 'Away',
    message: 'Face Not Detected\nGet Back In Frame',
    chipColor: Color(0xFF757575),
    chipBorder: Color(0xFF5C5C5C),
  );

  const FocusState({
    required this.label,
    required this.message,
    required this.chipColor,
    required this.chipBorder,
  });

  /// Text on the status chip.
  final String label;

  /// Text under the timer (Figma "Focused / Distracted / Away Status In Session").
  final String message;

  final Color chipColor;
  final Color chipBorder;

  /// Reads the backend's value ("FOCUSED", "focused", ...). Unknown values count as focused.
  static FocusState fromApi(String value) => FocusState.values.firstWhere(
        (s) => s.name == value.toLowerCase(),
        orElse: () => FocusState.focused,
      );
}
