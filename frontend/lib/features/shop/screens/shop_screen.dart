import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_colors.dart';
import '../../../shared/controllers/user_controller.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../../../shared/widgets/error_view.dart';
import '../../../shared/widgets/filter_pills.dart';
import '../../../shared/widgets/page_header.dart';
import '../controllers/shop_controller.dart';
import '../models/shop_item.dart';
import '../widgets/buy_item_dialog.dart';
import '../widgets/shop_item_card.dart';

/// The Shop (Figma "Shop" and "Buy Confirmation").
class ShopScreen extends StatefulWidget {
  const ShopScreen({super.key});

  @override
  State<ShopScreen> createState() => _ShopScreenState();
}

class _ShopScreenState extends State<ShopScreen> {
  late final ShopController _controller;

  @override
  void initState() {
    super.initState();
    _controller = ShopController(users: context.read<UserController>());
    _controller.load();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  /// Not owned → ask to buy. Owned → put on or take off.
  Future<void> _onTap(ShopItem item) async {
    if (item.owned) {
      final error = await _controller.toggleEquipped(item);
      if (!mounted) return;
      showAppSnackBar(
        context,
        error ?? (item.equipped ? '${item.name} taken off.' : 'Sparky is wearing ${item.name}!'),
        isError: error != null,
      );
      return;
    }

    final coins = _controller.coins;
    if (coins == null) return;

    final confirmed = await showBuyItemDialog(
      context,
      item: item,
      coins: coins,
      image: _controller.images[item.id],
    );
    if (!confirmed || !mounted) return;

    final error = await _controller.buy(item);
    if (!mounted) return;
    showAppSnackBar(
      context,
      error ?? 'You bought ${item.name}!',
      isError: error != null,
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      // Rebuild when the shop changes or the shared coins change.
      listenable: Listenable.merge([_controller, _controller.users]),
      builder: (context, _) => Scaffold(
        drawer: const AppDrawer(),
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PageHeader(showCoins: true, coins: _controller.coins),
                const Padding(
                  padding: EdgeInsets.fromLTRB(8, 4, 8, 16),
                  child: PageTitle('Shop'),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: FilterPills<String>(
                    options: ShopItem.filters,
                    selected: _controller.filter,
                    onSelected: _controller.setFilter,
                  ),
                ),
                const SizedBox(height: 16),
                Expanded(child: _buildBody()),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBody() {
    if (_controller.isLoading && _controller.items.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_controller.errorMessage != null && _controller.items.isEmpty) {
      return ErrorView(message: _controller.errorMessage!, onRetry: _controller.load);
    }

    final items = _controller.visibleItems;
    if (items.isEmpty) {
      return const Center(
        child: Text(
          'Nothing here yet. Check back soon!',
          style: TextStyle(color: AppColors.textMuted),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _controller.load,
      child: GridView.builder(
        padding: const EdgeInsets.fromLTRB(8, 0, 8, 24),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          mainAxisSpacing: 16,
          crossAxisSpacing: 16,
          childAspectRatio: 0.92,
        ),
        itemCount: items.length,
        itemBuilder: (context, index) {
          final item = items[index];
          return ShopItemCard(
            item: item,
            busy: _controller.busyItemId == item.id,
            image: _controller.images[item.id],
            onTap: () => _onTap(item),
          );
        },
      ),
    );
  }
}
