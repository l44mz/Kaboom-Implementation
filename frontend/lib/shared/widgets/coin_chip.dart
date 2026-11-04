import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_theme.dart';

/// The small "● 340" pill showing the user's coins. Shows "–" while loading.
class CoinChip extends StatelessWidget {
  const CoinChip({super.key, required this.coins});

  final int? coins;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 14,
            height: 14,
            decoration: const BoxDecoration(
              color: AppColors.coin,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 6),
          Text(
            coins?.toString() ?? '–',
            style: AppTheme.numberStyle.copyWith(
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: AppColors.coin,
            ),
          ),
        ],
      ),
    );
  }
}
