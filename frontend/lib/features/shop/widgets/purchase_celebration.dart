import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/app_lottie.dart';
import '../../../shared/widgets/primary_button.dart';
import '../models/shop_item.dart';
import 'shop_item_card.dart';

/// "You bought Top Hat!" with the item's picture and sparkles around it,
/// shown after a purchase goes through.
Future<void> showPurchaseCelebration(
  BuildContext context, {
  required ShopItem item,
  Uint8List? image,
}) {
  return showDialog<void>(
    context: context,
    barrierColor: Colors.black.withValues(alpha: 0.55),
    builder: (context) => AlertDialog(
      backgroundColor: AppColors.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // The item stays; the sparkles burst once over it.
          SizedBox(
            width: 180,
            height: 180,
            child: Stack(
              alignment: Alignment.center,
              children: [
                ItemPicture(item: item, image: image, size: 110),
                const IgnorePointer(
                  child: AppLottie(
                    AppAnimation.sparkles,
                    width: 180,
                    height: 180,
                    repeat: false,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'You bought ${item.name}!',
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Tap it in the Shop to put it on Sparky.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: AppColors.textMuted),
          ),
          const SizedBox(height: 20),
          // Same main button as Start Session and Login.
          PrimaryButton(
            label: 'Yay!',
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
    ),
  );
}
