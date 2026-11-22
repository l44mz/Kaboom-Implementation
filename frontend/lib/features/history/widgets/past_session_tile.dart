import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../models/history.dart';

/// One finished session: "Today · 4:30 PM / 50 min · 1 distraction" and "94% / ● +20".
class PastSessionTile extends StatelessWidget {
  const PastSessionTile({super.key, required this.session});

  final PastSession session;

  static const _percentColor = Color(0xFF38BDF8);

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  HistoryFormat.sessionTitle(session.startTime),
                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 4),
                Text(
                  session.summary,
                  style: const TextStyle(fontSize: 12, color: AppColors.textMuted),
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '${session.focusedPercentage.round()}%',
                style: AppTheme.numberStyle.copyWith(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: _percentColor,
                ),
              ),
              const SizedBox(height: 4),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: AppColors.coin,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 4),
                  Text(
                    '+${session.rewardEarned}',
                    style: AppTheme.numberStyle.copyWith(
                      fontSize: 12,
                      color: AppColors.coin,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }
}
