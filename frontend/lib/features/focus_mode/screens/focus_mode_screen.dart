import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/controllers/focus_mode_controller.dart';

/// "How do you want to focus?": the user picks who watches them during a session,
/// Sparky the robot or the phone's camera. The choice is saved, and Home uses it
/// when the user taps Start Session.
class FocusModeScreen extends StatelessWidget {
  const FocusModeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final modes = FocusModeController.to;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        foregroundColor: AppColors.textPrimary,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 32),
          // Obx: the selected card changes by itself when the choice changes.
          child: Obx(() {
            final mode = modes.mode.value;
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'How do you want to focus?',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  'You can change this any time.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 14, color: AppColors.textMuted),
                ),
                const SizedBox(height: 24),
                _ModeCard(
                  icon: Icons.smart_toy_outlined,
                  title: 'Sparky robot',
                  description: 'Sparky sits on your desk and watches you with its camera. '
                      'You need to connect your robot once.',
                  selected: mode == FocusMode.robot,
                  onTap: () => modes.choose(FocusMode.robot),
                  action: TextButton.icon(
                    onPressed: () => context.push(AppRoutes.robotPair),
                    icon: const Icon(Icons.link, size: 18),
                    label: const Text('Connect a robot'),
                  ),
                ),
                const SizedBox(height: 16),
                _ModeCard(
                  icon: Icons.phone_android,
                  title: 'Phone camera',
                  description: kIsWeb
                      ? "No robot needed. Your computer's camera watches you, "
                          "and the screen shows Sparky's face. Sit facing the camera."
                      : "No robot needed. Your phone's front camera watches you, "
                          "and the phone shows Sparky's face. Put the phone on a stand facing you.",
                  selected: mode == FocusMode.phone,
                  enabled: FocusModeController.phoneSupported,
                  disabledNote: 'Only in the phone app and on the website.',
                  onTap: () => modes.choose(FocusMode.phone),
                  action: FocusModeController.phoneSupported
                      ? TextButton.icon(
                          onPressed: () => context.push(AppRoutes.cameraTest),
                          icon: const Icon(Icons.videocam_outlined, size: 18),
                          label: const Text('Test the camera'),
                        )
                      : null,
                ),
                const SizedBox(height: 24),
                const Text(
                  'Camera mode checks your focus on your phone or computer itself. '
                  'No video is sent anywhere, only Focused, Distracted or Away.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 12, color: AppColors.textMuted),
                ),
              ],
            );
          }),
        ),
      ),
    );
  }
}

/// One choice: an icon, a title, a short text, and a border when selected.
class _ModeCard extends StatelessWidget {
  const _ModeCard({
    required this.icon,
    required this.title,
    required this.description,
    required this.selected,
    required this.onTap,
    this.enabled = true,
    this.disabledNote,
    this.action,
  });

  final IconData icon;
  final String title;
  final String description;
  final bool selected;
  final VoidCallback onTap;
  final bool enabled;

  /// Why the card can't be chosen here (shown only when [enabled] is false).
  final String? disabledNote;

  /// An extra button under the text (Connect a robot).
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: enabled ? 1 : 0.5,
      child: Material(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: enabled ? onTap : null,
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: selected ? AppColors.primary : AppColors.border,
                width: selected ? 2 : 1,
              ),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                CircleAvatar(
                  radius: 24,
                  backgroundColor: selected ? AppColors.primary : AppColors.field,
                  child: Icon(icon, color: AppColors.textPrimary),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              title,
                              style: const TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.w600,
                                color: AppColors.textPrimary,
                              ),
                            ),
                          ),
                          if (selected)
                            const Icon(Icons.check_circle, color: AppColors.primary),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(
                        description,
                        style: const TextStyle(fontSize: 13, color: AppColors.textMuted),
                      ),
                      if (!enabled && disabledNote != null) ...[
                        const SizedBox(height: 6),
                        Text(
                          disabledNote!,
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: AppColors.distracted,
                          ),
                        ),
                      ],
                      if (action != null) ...[
                        const SizedBox(height: 4),
                        action!,
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}