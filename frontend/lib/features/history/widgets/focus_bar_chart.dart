import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../models/history.dart';

/// The "Focus minutes" card: one bar per day (or hour block, week, month),
/// with the minutes on top and the label under it. The current bar is brighter.
class FocusBarChart extends StatelessWidget {
  const FocusBarChart({super.key, required this.bars, required this.caption});

  final List<HistoryBar> bars;
  final String caption;

  static const _barArea = 96.0;
  static const _barColor = Color(0xFF7C76F5);

  @override
  Widget build(BuildContext context) {
    final maxMinutes = bars.fold<int>(0, (m, bar) => math.max(m, bar.focusMinutes));

    return Container(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text(
                'Focus minutes',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
              ),
              const Spacer(),
              Text(
                caption,
                style: const TextStyle(fontSize: 12, color: AppColors.textMuted),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              for (final bar in bars)
                Expanded(child: _Bar(bar: bar, maxMinutes: maxMinutes)),
            ],
          ),
        ],
      ),
    );
  }
}

class _Bar extends StatelessWidget {
  const _Bar({required this.bar, required this.maxMinutes});

  final HistoryBar bar;
  final int maxMinutes;

  @override
  Widget build(BuildContext context) {
    final empty = bar.focusMinutes == 0;
    final height = maxMinutes == 0 || empty
        ? 3.0
        : math.max(8.0, FocusBarChart._barArea * bar.focusMinutes / maxMinutes);
    final color = bar.current ? AppColors.primary : FocusBarChart._barColor;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          empty ? '–' : '${bar.focusMinutes}',
          style: AppTheme.numberStyle.copyWith(
            fontSize: 10,
            color: bar.current ? AppColors.textPrimary : AppColors.textMuted,
          ),
        ),
        const SizedBox(height: 4),
        Container(
          width: 20,
          height: height,
          decoration: BoxDecoration(
            color: empty ? AppColors.border : color,
            borderRadius: BorderRadius.circular(6),
          ),
        ),
        const SizedBox(height: 6),
        Text(
          bar.label,
          style: TextStyle(
            fontSize: 11,
            fontWeight: bar.current ? FontWeight.w700 : FontWeight.w400,
            color: bar.current ? AppColors.textPrimary : AppColors.textMuted,
          ),
        ),
      ],
    );
  }
}
