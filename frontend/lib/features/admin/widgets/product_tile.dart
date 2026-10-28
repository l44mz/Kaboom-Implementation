import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../models/product.dart';

/// One row in "Store products": icon, name, category · price,
/// a Visible/Hidden chip (tap to switch) and an edit button.
class ProductTile extends StatelessWidget {
  const ProductTile({
    super.key,
    required this.product,
    this.onEdit,
    this.onToggleVisibility,
  });

  final Product product;
  final VoidCallback? onEdit;
  final VoidCallback? onToggleVisibility;

  static const _visibleColor = Color(0xFF5CC8F5);

  @override
  Widget build(BuildContext context) {
    final visible = product.available;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.fromLTRB(14, 12, 6, 12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          // Icon box
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: const Color(0xFF2C2F7A),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Icon(
              Icons.view_in_ar_outlined,
              color: AppColors.secondary,
              size: 24,
            ),
          ),
          const SizedBox(width: 16),

          // Name + "Hats · ● 60"
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  product.name,
                  style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Text(
                      '${product.categoryLabel} · ',
                      style: const TextStyle(fontSize: 14, color: AppColors.textMuted),
                    ),
                    if (product.price == 0)
                      const Text(
                        'Free',
                        style: TextStyle(fontSize: 14, color: AppColors.textMuted),
                      )
                    else ...[
                      Container(
                        width: 8,
                        height: 8,
                        decoration: const BoxDecoration(
                          color: AppColors.coin,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Text(
                        '${product.price}',
                        style: AppTheme.numberStyle.copyWith(
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          color: AppColors.coin,
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),

          // Visible / Hidden chip (tap to switch)
          Tooltip(
            message: visible ? 'Hide from store' : 'Show in store',
            child: Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: onToggleVisibility,
                borderRadius: BorderRadius.circular(999),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(999),
                    border: Border.all(
                      color: visible ? _visibleColor : AppColors.border,
                    ),
                  ),
                  child: Text(
                    visible ? 'Visible' : 'Hidden',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: visible ? _visibleColor : AppColors.textMuted,
                    ),
                  ),
                ),
              ),
            ),
          ),

          // Edit
          IconButton(
            tooltip: 'Edit product',
            icon: const Icon(Icons.edit_outlined, color: AppColors.textMuted),
            onPressed: onEdit,
          ),
        ],
      ),
    );
  }
}