import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/controllers/user_controller.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../../../shared/widgets/error_view.dart';
import '../../../shared/widgets/page_header.dart';
import '../../../shared/widgets/stat_box.dart';
import '../controllers/account_controller.dart';
import '../widgets/change_password_dialog.dart';
import '../widgets/edit_profile_dialog.dart';
import '../widgets/settings_group.dart';

/// My Account (Figma "My Account").
class AccountScreen extends StatefulWidget {
  const AccountScreen({super.key});

  @override
  State<AccountScreen> createState() => _AccountScreenState();
}

class _AccountScreenState extends State<AccountScreen> {
  late final AccountController _controller;

  static const _connectedColor = Color(0xFF38BDF8);

  @override
  void initState() {
    super.initState();
    _controller = AccountController(users: context.read<UserController>());
    _controller.load();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _editProfile() async {
    final user = _controller.user;
    if (user == null) return;

    var newEmail = user.email;
    final saved = await showEditProfileDialog(
      context,
      name: user.name,
      email: user.email,
      onSave: (name, email) async {
        final error = await _controller.updateProfile(name, email);
        if (error == null) newEmail = email.trim().toLowerCase();
        return error;
      },
    );
    if (!saved || !mounted) return;

    // The login token holds the old email, so a new email needs a new login.
    final emailChanged = newEmail != user.email.toLowerCase();
    if (emailChanged) {
      await _controller.logout();
      if (!mounted) return;
      showAppSnackBar(context, 'Email changed. Please log in with your new email.');
      context.go(AppRoutes.login);
    } else {
      showAppSnackBar(context, 'Profile updated.');
    }
  }

  Future<void> _changePassword() async {
    final saved = await showChangePasswordDialog(
      context,
      onSave: _controller.changePassword,
    );
    if (saved && mounted) showAppSnackBar(context, 'Password changed.');
  }

  Future<void> _logout() async {
    await _controller.logout();
    if (!mounted) return;
    context.go(AppRoutes.login);
  }

  /// 64800 → "18h", 2700 → "45m", null → "–"
  static String _focusHours(int? seconds) {
    if (seconds == null) return '–';
    if (seconds < 3600) return '${seconds ~/ 60}m';
    return '${(seconds / 3600).round()}h';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: ListenableBuilder(
          // Rebuild when the account data or the shared user (name, coins) changes.
          listenable: Listenable.merge([_controller, _controller.users]),
          builder: (context, _) => RefreshIndicator(
            onRefresh: _controller.load,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
              children: [
                const PageHeader(showMenu: false),
                const Padding(
                  padding: EdgeInsets.fromLTRB(8, 4, 8, 16),
                  child: PageTitle('My Account'),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: _buildContent(),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildContent() {
    final user = _controller.user;

    if (_controller.isLoading && user == null) {
      return const Padding(
        padding: EdgeInsets.only(top: 60),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (_controller.errorMessage != null && user == null) {
      return SizedBox(
        height: 260,
        child: ErrorView(message: _controller.errorMessage!, onRetry: _controller.load),
      );
    }
    if (user == null) return const SizedBox.shrink();

    final robot = _controller.robot;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Profile card
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.border),
          ),
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
                      user.name,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      user.email,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 12, color: AppColors.textMuted),
                    ),
                  ],
                ),
              ),
              OutlinedButton(
                onPressed: _editProfile,
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textPrimary,
                  side: const BorderSide(color: AppColors.border),
                  shape: const StadiumBorder(),
                  visualDensity: VisualDensity.compact,
                ),
                child: const Text('Edit'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // Coins / Sessions / Focus hours
        Row(
          children: [
            Expanded(
              child: StatBox(
                value: '${user.coins}',
                label: 'Coins',
                valueColor: AppColors.coin,
                centered: true,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: StatBox(
                value: _controller.sessionCount?.toString() ?? '–',
                label: 'Sessions',
                centered: true,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: StatBox(
                value: _focusHours(_controller.focusSeconds),
                label: 'Focus hours',
                valueColor: _connectedColor,
                centered: true,
              ),
            ),
          ],
        ),

        // Profile
        const SettingsLabel('Profile'),
        SettingsGroup(
          children: [
            SettingsRow(
              icon: Icons.edit_outlined,
              title: 'Edit profile',
              onTap: _editProfile,
            ),
            SettingsRow(
              icon: Icons.lock_outline,
              title: 'Change password',
              onTap: _changePassword,
            ),
          ],
        ),

        // Robot
        const SettingsLabel('Robot'),
        SettingsGroup(
          children: [
            SettingsRow(
              icon: Icons.smart_toy_outlined,
              title: robot?.name ?? 'No robot yet',
              value: robot == null
                  ? 'Connect'
                  : robot.connected
                      ? 'Connected'
                      : 'Offline',
              valueColor: robot?.connected ?? false ? _connectedColor : AppColors.textMuted,
              onTap: () => context.push(AppRoutes.robotPair),
            ),
          ],
        ),

        // Preferences
        const SettingsLabel('Preferences'),
        SettingsGroup(
          children: [
            SettingsRow(
              icon: Icons.notifications_none,
              title: 'Notifications',
              trailing: Switch(
                value: _controller.notificationsOn,
                onChanged: _controller.setNotifications,
              ),
              onTap: () => _controller.setNotifications(!_controller.notificationsOn),
            ),
            SettingsRow(
              icon: Icons.language,
              title: 'Language',
              value: 'English',
              onTap: () => showAppSnackBar(
                context,
                'Only English for now. Arabic is coming soon.',
              ),
            ),
          ],
        ),
        const SizedBox(height: 24),

        // Log out
        OutlinedButton.icon(
          onPressed: _logout,
          icon: const Icon(Icons.logout),
          label: const Text('Log out', style: TextStyle(fontWeight: FontWeight.w600)),
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.away,
            side: const BorderSide(color: AppColors.away, width: 1.5),
            minimumSize: const Size.fromHeight(50),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          ),
        ),
      ],
    );
  }
}
