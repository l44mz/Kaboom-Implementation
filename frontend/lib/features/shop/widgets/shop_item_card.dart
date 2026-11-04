import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/mini_robot.dart';
import '../models/shop_item.dart';

/// One card in the shop grid: Sparky wearing the item, its name, and its price,
/// or "Equipped" / "Owned" when the user already has it.
class ShopItemCard extends StatelessWidget {
  const ShopItemCard({
    super.key,
    required this.item,
    required this.onTap,
    this.busy = false,
    this.image,
  });

  final ShopItem item;
  final VoidCallback onTap;
  final bool busy;

  /// The admin's picture of the item, if there is one. Otherwise Sparky is drawn wearing it.
  final Uint8List? image;

  static const _cardColor = Color(0xFF282B7A);
  static const equippedColor = Color(0xFF38BDF8);

  @override
  Widget build(BuildContext context) {
    return Material(
      color: _cardColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(
          color: item.equipped ? equippedColor.withValues(alpha: 0.7) : AppColors.border,
        ),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: busy ? null : onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 12, 12, 10),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Center(
                  child: ItemPicture(item: item, image: image, size: 92),
                ),
              ),
              const SizedBox(height: 6),
              Text(
                item.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 4),
              SizedBox(height: 18, child: _status()),
            ],
          ),
        ),
      ),
    );
  }

  Widget _status() {
    if (busy) {
      return const Align(
        alignment: Alignment.centerLeft,
        child: SizedBox(
          width: 14,
          height: 14,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      );
    }
    if (item.equipped) {
      return Text(
        'Equipped',
        style: AppTheme.numberStyle.copyWith(fontSize: 12, color: equippedColor),
      );
    }
    if (item.owned) {
      return const Text(
        'Owned · tap to wear',
        style: TextStyle(fontSize: 12, color: AppColors.textMuted),
      );
    }
    return Row(
      children: [
        Container(
          width: 10,
          height: 10,
          decoration: const BoxDecoration(color: AppColors.coin, shape: BoxShape.circle),
        ),
        const SizedBox(width: 6),
        Text(
          '${item.price}',
          style: AppTheme.numberStyle.copyWith(
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: AppColors.coin,
          ),
        ),
      ],
    );
  }
}

/// The admin's picture of [item] if there is one, otherwise Sparky wearing it.
class ItemPicture extends StatelessWidget {
  const ItemPicture({super.key, required this.item, this.image, this.size = 92});

  final ShopItem item;
  final Uint8List? image;
  final double size;

  @override
  Widget build(BuildContext context) {
    final bytes = image;
    if (bytes != null) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(14),
        child: Image.memory(
          bytes,
          width: size,
          height: size,
          fit: BoxFit.contain,
          // A broken file falls back to the drawing.
          errorBuilder: (context, error, stack) => MiniRobot(
            look: RobotLook.forItem(item.name, item.category),
            size: size,
          ),
        ),
      );
    }
    return MiniRobot(look: RobotLook.forItem(item.name, item.category), size: size);
  }
}
