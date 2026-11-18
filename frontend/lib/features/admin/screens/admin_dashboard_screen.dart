import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../../../shared/widgets/error_view.dart';
import '../controllers/admin_controller.dart';
import '../models/product.dart';
import '../widgets/product_tile.dart';
import '../widgets/stat_card.dart';

class AdminDashboardScreen extends StatefulWidget {
  const AdminDashboardScreen({super.key});

  @override
  State<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends State<AdminDashboardScreen> {
  final _controller = AdminController();

  static const _robotColor = Color(0xFF5CC8F5);

  @override
  void initState() {
    super.initState();
    _controller.loadAll();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  /// Opens the form: empty to add, filled to edit [product].
  Future<void> _openForm([Product? product]) async {
    final saved = await context.push<bool>(AppRoutes.addProduct, extra: product);
    if (saved == true && mounted) {
      showAppSnackBar(
        context,
        product == null ? 'Product added.' : 'Product updated.',
      );
      _controller.loadAll();
    }
  }

  /// Visible ⇄ Hidden.
  Future<void> _toggle(Product product) async {
    final ok = await _controller.toggleAvailability(product);
    if (!mounted) return;
    if (ok) {
      showAppSnackBar(
        context,
        product.available
            ? '${product.name} is hidden.'
            : '${product.name} is visible.',
      );
    } else {
      showAppSnackBar(context, _controller.errorMessage!, isError: true);
    }
  }

  /// 1248 → "1,248"; null (not loaded yet) → "–".
  static String _format(int? n) => n == null
      ? '–'
      : n.toString().replaceAllMapped(
            RegExp(r'\B(?=(\d{3})+(?!\d))'),
            (_) => ',',
          );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: const AppDrawer(),
      body: SafeArea(
        child: ListenableBuilder(
          listenable: _controller,
          builder: (context, _) {
            final stats = _controller.stats;

            return RefreshIndicator(
              onRefresh: _controller.loadAll,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
                children: [
                  // Menu button
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Builder(
                      builder: (context) => IconButton(
                        tooltip: 'Open menu',
                        icon: const Icon(Icons.menu, size: 30),
                        onPressed: () => Scaffold.of(context).openDrawer(),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),

                  // Title
                  const Text(
                    'Admin',
                    style: TextStyle(fontSize: 40, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    "Manage Sparky's store and users",
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w300,
                      color: AppColors.textMuted,
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Stats
                  GridView.count(
                    crossAxisCount: 2,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    mainAxisSpacing: 14,
                    crossAxisSpacing: 14,
                    childAspectRatio: 1.3,
                    children: [
                      StatCard(
                        icon: Icons.group_outlined,
                        value: _format(stats?.totalUsers),
                        label: 'Total users',
                      ),
                      StatCard(
                        icon: Icons.timer_outlined,
                        value: _format(stats?.sessionsToday),
                        label: 'Sessions today',
                      ),
                      StatCard(
                        icon: Icons.smart_toy_outlined,
                        value: _format(stats?.robotsOnline),
                        label: 'Robots online',
                        valueColor: _robotColor,
                      ),
                      StatCard(
                        icon: Icons.view_in_ar_outlined,
                        value: _format(stats?.storeProducts),
                        label: 'Store products',
                        valueColor: AppColors.coin,
                      ),
                    ],
                  ),
                  const SizedBox(height: 28),

                  // "Store products" + Add product
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Store products',
                        style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                      ),
                      FilledButton.icon(
                        onPressed: () => _openForm(),
                        icon: const Icon(Icons.add),
                        label: const Text('Add product'),
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          foregroundColor: Colors.white,
                          shape: const StadiumBorder(),
                          textStyle: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 18,
                            vertical: 12,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Product list (or loading / error / empty)
                  if (_controller.isLoading && _controller.products.isEmpty)
                    const Padding(
                      padding: EdgeInsets.only(top: 40),
                      child: Center(child: CircularProgressIndicator()),
                    )
                  else if (_controller.errorMessage != null &&
                      _controller.products.isEmpty)
                    SizedBox(
                      height: 240,
                      child: ErrorView(
                        message: _controller.errorMessage!,
                        onRetry: _controller.loadAll,
                      ),
                    )
                  else if (_controller.products.isEmpty)
                    const Padding(
                      padding: EdgeInsets.only(top: 24),
                      child: Text(
                        'No products yet. Tap "Add product" to create the first one.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: AppColors.textMuted),
                      ),
                    )
                  else
                    ..._controller.products.map(
                      (p) => ProductTile(
                        product: p,
                        onEdit: () => _openForm(p),
                        onToggleVisibility: () => _toggle(p),
                      ),
                    ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}