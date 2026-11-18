import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/robot_face.dart';
import '../models/session_result.dart';

/// The summary after a session (Figma "Session Complete").
class SessionCompleteScreen extends StatelessWidget {
  const SessionCompleteScreen({super.key, this.result});

  /// Passed from the session screen through the router's `extra`.
  final SessionResult? result;

  @override
  Widget build(BuildContext context) {
    final result = this.result;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        foregroundColor: AppColors.textPrimary,
      ),
      extendBodyBehindAppBar: true,
      drawer: const AppDrawer(),
      body: Stack(
        children: [
          const Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: CustomPaint(size: Size(401, 260), painter: _ConfettiPainter()),
          ),
          SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(24, 40, 24, 32),
              child: Column(
                children: [
                  const RobotFace(
                    expression: RobotExpression.happy,
                    width: 228,
                    glow: true,
                  ),
                  const SizedBox(height: 20),
                  const Text(
                    'Good Job!',
                    style: TextStyle(
                      fontSize: 34,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    result != null && result.endedEarly
                        ? 'Session ended early'
                        : 'Focus Session Summary',
                    style: const TextStyle(fontSize: 15, color: AppColors.textMuted),
                  ),
                  const SizedBox(height: 18),
                  _SummaryCard(result: result),
                  const SizedBox(height: 56),
                  SizedBox(
                    width: 256,
                    height: 52,
                    child: FilledButton(
                      onPressed: () => context.go(AppRoutes.shop),
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.secondary,
                        foregroundColor: AppColors.textPrimary,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                      ),
                      child: const Text(
                        'Shop',
                        style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ),
                  const SizedBox(height: 22),
                  SizedBox(
                    width: 256,
                    height: 52,
                    child: OutlinedButton(
                      onPressed: () => context.go(AppRoutes.home),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppColors.textPrimary,
                        side: const BorderSide(color: AppColors.secondary, width: 2),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14),
                        ),
                      ),
                      child: const Text(
                        'Back Home',
                        style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Time focused, a divider, and the coin with the coins earned (353 x 233 in Figma).
class _SummaryCard extends StatelessWidget {
  const _SummaryCard({required this.result});

  final SessionResult? result;

  @override
  Widget build(BuildContext context) {
    // Figma's 353 px on wide phones, narrower on small ones.
    return Container(
      width: double.infinity,
      constraints: const BoxConstraints(maxWidth: 353),
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 20),
      decoration: BoxDecoration(
        color: AppColors.field,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.primary),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Time Focused',
            style: TextStyle(fontSize: 18, color: AppColors.textMuted),
          ),
          const SizedBox(height: 2),
          Text(
            result?.timeFocusedLabel ?? '--',
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 10),
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 40),
            child: Divider(height: 1, color: Color(0xFF3847A6)),
          ),
          const SizedBox(height: 14),
          Center(child: _Coin(amount: result?.coinsEarned)),
          const SizedBox(height: 6),
          const Center(
            child: Text(
              'Coins Earned',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: AppColors.textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// The gold coin with the number of coins in it (73 x 73 in Figma).
class _Coin extends StatelessWidget {
  const _Coin({required this.amount});

  final int? amount;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 73,
      height: 73,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: const Color(0xFFF8D063),
        border: Border.all(color: AppColors.coin, width: 3),
        boxShadow: [
          BoxShadow(
            color: AppColors.coin.withValues(alpha: 0.35),
            blurRadius: 10,
          ),
        ],
      ),
      child: Text(
        amount?.toString() ?? '--',
        style: AppTheme.numberStyle.copyWith(
          fontSize: 28,
          color: const Color(0xFF2A2A2A),
        ),
      ),
    );
  }
}

/// Confetti across the top, at the positions of the Figma "Frame" pieces.
class _ConfettiPainter extends CustomPainter {
  const _ConfettiPainter();

  // (x, y, width, height, color, rotation in radians, round)
  static const _pieces = [
    (34.0, 19.0, 12.0, 8.0, Color(0xFF6B4BCC), -0.6, false),
    (336.0, 43.0, 10.0, 15.0, Color(0xFFB99039), 0.5, false),
    (56.0, 64.0, 12.0, 12.0, Color(0xFF9067D0), 0.0, true),
    (323.0, 84.0, 12.0, 12.0, Color(0xFF4258D8), 0.0, true),
    (99.0, 14.0, 10.0, 6.0, Color(0xFF8F66C8), 0.7, false),
    (274.0, 22.0, 9.0, 13.0, Color(0xFF318FCD), -0.6, false),
    (200.0, 13.0, 10.0, 10.0, Color(0xFFB99039), 0.0, true),
    (130.0, 51.0, 8.0, 8.0, Color(0xFFAFB2CA), 0.0, true),
    (238.0, 59.0, 8.0, 14.0, Color(0xFF8F66C8), 0.3, false),
    (15.0, 115.0, 10.0, 10.0, Color(0xFF4156D2), 0.0, true),
    (375.0, 125.0, 10.0, 10.0, Color(0xFF6B4BCC), 0.0, true),
  ];

  @override
  void paint(Canvas canvas, Size size) {
    // Figma's frame is 401 px wide; spread the pieces over the real width.
    final scale = size.width / 401;
    for (final (x, y, w, h, color, angle, round) in _pieces) {
      final paint = Paint()..color = color;
      final center = Offset((x + w / 2) * scale, y + h / 2);
      if (round) {
        canvas.drawCircle(center, w / 2, paint);
      } else {
        canvas
          ..save()
          ..translate(center.dx, center.dy)
          ..rotate(angle)
          ..drawRRect(
            RRect.fromRectAndRadius(
              Rect.fromCenter(center: Offset.zero, width: w, height: h),
              const Radius.circular(1.5),
            ),
            paint,
          )
          ..restore();
      }
    }
  }

  @override
  bool shouldRepaint(_ConfettiPainter old) => false;
}
