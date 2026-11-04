import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../models/shop_item.dart';
import 'shop_item_card.dart';

/// "Buy Party Hat?" with the price and the balance after. Returns true when the user confirms.
Future<bool> showBuyItemDialog(
  BuildContext context, {
  required ShopItem item,
  required int coins,
  Uint8List? image,
}) async {
  final confirmed = await showDialog<bool>(
    context: context,
    barrierColor: Colors.black.withValues(alpha: 0.55),
    builder: (context) => BuyItemDialog(item: item, coins: coins, image: image),
  );
  return confirmed ?? false;
}

class BuyItemDialog extends StatelessWidget {
  const BuyItemDialog({super.key, required this.item, required this.coins, this.image});

  final ShopItem item;
  final int coins;
  final Uint8List? image;

  static const _dialogColor = Color(0xFF262974);
  static const _boxColor = Color(0xFF1E2060);

  @override
  Widget build(BuildContext context) {
    final balanceAfter = coins - item.price;
    final canAfford = balanceAfter >= 0;

    return Dialog(
      backgroundColor: _dialogColor,
      insetPadding: const EdgeInsets.symmetric(horizontal: 32),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
        side: const BorderSide(color: AppColors.border),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 24, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ItemPicture(item: item, image: image, size: 120),
            const SizedBox(height: 12),
            Text(
              'Buy ${item.name}?',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 6),
            Text(
              canAfford
                  ? 'Sparky will wear it in your next session.'
                  : 'You need ${-balanceAfter} more coins. Finish a session to earn them!',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 13,
                color: canAfford ? AppColors.textMuted : AppColors.away,
              ),
            ),
            const SizedBox(height: 16),

            // Price / Balance after
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              decoration: BoxDecoration(
                color: _boxColor,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  _Amount(label: 'Price', value: item.price),
                  const Spacer(),
                  _Amount(
                    label: 'Balance after',
                    value: balanceAfter,
                    alignEnd: true,
                    color: canAfford ? AppColors.coin : AppColors.away,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),

            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(false),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.textPrimary,
                      side: const BorderSide(color: AppColors.border, width: 1.5),
                      minimumSize: const Size.fromHeight(48),
                      shape: const StadiumBorder(),
                    ),
                    child: const Text('Cancel', style: TextStyle(fontWeight: FontWeight.w600)),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: canAfford ? () => Navigator.of(context).pop(true) : null,
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                      minimumSize: const Size.fromHeight(48),
                      shape: const StadiumBorder(),
                    ),
                    child: const Text('Confirm', style: TextStyle(fontWeight: FontWeight.w600)),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// "Price  ● 60" — a small grey label over a coin amount.
class _Amount extends StatelessWidget {
  const _Amount({
    required this.label,
    required this.value,
    this.alignEnd = false,
    this.color = AppColors.coin,
  });

  final String label;
  final int value;
  final bool alignEnd;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: alignEnd ? CrossAxisAlignment.end : CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: AppColors.textMuted)),
        const SizedBox(height: 4),
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 10,
              height: 10,
              decoration: const BoxDecoration(color: AppColors.coin, shape: BoxShape.circle),
            ),
            const SizedBox(width: 6),
            Text(
              '$value',
              style: AppTheme.numberStyle.copyWith(
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: color,
              ),
            ),
          ],
        ),
      ],
    );
  }
}
