import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:go_router/go_router.dart';

import '../../core/router/app_router.dart';
import '../../core/storage/token_storage.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_theme.dart';
import '../controllers/user_controller.dart';
import '../models/user.dart';

/// The side menu (Figma "Menu Open").
class AppDrawer extends StatelessWidget {
  const AppDrawer({super.key});

  @override
  Widget build(BuildContext context) {
   
    final users = Get.isRegistered<UserController>() ? Get.find<UserController>() : null;
    if (users == null) return _buildMenu(context, null);

    
    return Obx(() => _buildMenu(context, users.user.value));
  }

  Widget _buildMenu(BuildContext context, User? user) {
    return Drawer(
      backgroundColor: AppColors.surface,
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _DrawerHeader(user: user),
            // The menu items scroll if the window is short.
            Expanded(
              child: ListView(
                padding: EdgeInsets.zero,
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: FilledButton.icon(
                      // Before: only closed the menu, so on any other page nothing happened.
                      // Now: close the menu and go Home, where you pick the time and start.
                      onPressed: () {
                        final router = GoRouter.of(context); // read it before the menu closes
                        Navigator.of(context).pop();
                        router.go(AppRoutes.home);
                      },
                      icon: const Icon(Icons.play_arrow_outlined),
                      label: const Text('Start Session'),
                      style: FilledButton.styleFrom(
                        alignment: Alignment.centerLeft,
                        minimumSize: const Size.fromHeight(48),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                  const _SectionLabel('FOCUS'),
                  const _MenuItem(
                    icon: Icons.history,
                    label: 'Session History',
                    route: AppRoutes.history,
                  ),
                  const _SectionLabel('SPARKY'),
                  const _MenuItem(
                    icon: Icons.shopping_bag_outlined,
                    label: 'Store',
                    route: AppRoutes.shop,
                  ),
                  // Robot or phone camera. Connecting the robot is inside this screen.
                  const _MenuItem(
                    icon: Icons.center_focus_strong_outlined,
                    label: 'Focus Mode',
                    route: AppRoutes.focusMode,
                  ),
                  const _SectionLabel('ACCOUNT'),
                  const _MenuItem(
                    icon: Icons.person_outline,
                    label: 'My Account',
                    route: AppRoutes.account,
                  ),
                  if (user?.isAdmin ?? false) ...[
                    const _SectionLabel('ADMIN'),
                    const _MenuItem(
                      icon: Icons.dashboard_outlined,
                      label: 'Admin Dashboard',
                      route: AppRoutes.admin,
                    ),
                  ],
                ],
              ),
            ),
            const Divider(color: AppColors.border),
            ListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 24),
              leading: const Icon(Icons.logout, color: AppColors.textMuted),
              title: const Text('Log out'),
              onTap: () => _logout(context),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _logout(BuildContext context) async {
    await TokenStorage().clear();
    if (!context.mounted) return;
    if (Get.isRegistered<UserController>()) Get.find<UserController>().clear();
    context.go(AppRoutes.login);
  }
}

/// Avatar, greeting and coin balance at the top of the menu.
class _DrawerHeader extends StatelessWidget {
  const _DrawerHeader({this.user});

  final User? user;

  @override
  Widget build(BuildContext context) {
    final name = user?.name ?? 'there';
    final coins = user?.coins;

    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 24),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 26,
            backgroundColor: AppColors.primary,
            child: Icon(Icons.person_outline, color: AppColors.textPrimary),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Hi $name!',
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    const Icon(Icons.circle, size: 10, color: AppColors.coin),
                    const SizedBox(width: 6),
                    Text(
                      coins == null ? '-- coins' : '$coins coins',
                      style: AppTheme.numberStyle.copyWith(
                        fontSize: 12,
                        color: AppColors.coin,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}


class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 20, 24, 4),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          letterSpacing: 1,
          color: AppColors.textMuted,
        ),
      ),
    );
  }
}

/// One menu row that closes the menu and opens a screen.
class _MenuItem extends StatelessWidget {
  const _MenuItem({
    required this.icon,
    required this.label,
    required this.route,
  });

  final IconData icon;
  final String label;
  final String route;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 24),
      leading: Icon(icon, color: AppColors.textMuted),
      title: Text(label),
      trailing: const Icon(Icons.chevron_right, color: AppColors.textMuted),
      onTap: () {
        final router = GoRouter.of(context);
        Navigator.of(context).pop();
        router.push(route);
      },
    );
  }
}