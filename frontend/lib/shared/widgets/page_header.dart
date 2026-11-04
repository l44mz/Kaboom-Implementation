import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import 'coin_chip.dart';

/// The top row of Shop, History and My Account: a back arrow on the left,
/// and optionally the coins and the menu button on the right.
class PageHeader extends StatelessWidget {
  const PageHeader({
    super.key,
    this.showMenu = true,
    this.showCoins = false,
    this.coins,
  });

  final bool showMenu;
  final bool showCoins;
  final int? coins;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        IconButton(
          tooltip: 'Back',
          icon: const Icon(Icons.arrow_back, size: 26),
          onPressed: () {
            if (context.canPop()) {
              context.pop();
            } else {
              context.go(AppRoutes.home);
            }
          },
        ),
        const Spacer(),
        if (showCoins) CoinChip(coins: coins),
        if (showMenu)
          IconButton(
            tooltip: 'Open menu',
            icon: const Icon(Icons.menu, size: 30),
            onPressed: () => Scaffold.of(context).openDrawer(),
          ),
      ],
    );
  }
}

/// The big page title, with an optional grey line under it.
class PageTitle extends StatelessWidget {
  const PageTitle(this.title, {super.key, this.subtitle});

  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(fontSize: 34, fontWeight: FontWeight.w700),
        ),
        if (subtitle != null) ...[
          const SizedBox(height: 4),
          Text(
            subtitle!,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w300,
              color: AppColors.textMuted,
            ),
          ),
        ],
      ],
    );
  }
}
