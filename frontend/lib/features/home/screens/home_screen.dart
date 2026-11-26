import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:get/get.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/router/app_router.dart';
import '../../../core/storage/token_storage.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/controllers/focus_mode_controller.dart';
import '../../../shared/controllers/outfit_controller.dart';
import '../../../shared/controllers/user_controller.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../../../shared/widgets/dressed_robot.dart';
import '../../../shared/widgets/coin_chip.dart';
import '../../../shared/widgets/error_view.dart';
import '../../../shared/widgets/loading_view.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../../shared/widgets/robot_face.dart';
import '../../session/services/camera_session_service.dart';
import '../../session/services/session_service.dart';
import '../widgets/duration_dial.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {


  final _sessionService = SessionService(ApiClient(TokenStorage()));
  final _cameraService = CameraSessionService(ApiClient(TokenStorage()));

  String? _error;
  bool _isLoading = true;
  bool _isStarting = false;
  int _minutes = 15;

  @override
  void initState() {
    super.initState();
    _loadUser();
  }

  Future<void> _loadUser() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      // Shared with the side menu on every screen, so the coins match everywhere.
      await Get.find<UserController>().load();
      // What Sparky wears. Not awaited: the robot shows right away and gets dressed
      // when the pictures arrive.
      if (Get.isRegistered<OutfitController>()) Get.find<OutfitController>().load();
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = apiErrorMessage(e));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _startSession() async {
    setState(() => _isStarting = true);
    try {
      // Robot mode: Sparky watches. Phone mode: the phone's camera watches (camera session).
      final service =
          FocusModeController.to.usesPhone ? _cameraService : _sessionService;
      final session = await service.startSession(
        plannedDurationSeconds: _minutes * 60,
      );
      if (!mounted) return;
      context.go(AppRoutes.session, extra: session);
    } catch (e) {
      if (!mounted) return;
      showAppSnackBar(context, startSessionErrorMessage(e), isError: true);
    } finally {
      if (mounted) setState(() => _isStarting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        foregroundColor: AppColors.textPrimary,
        // The coins at the top right, like the Store. Obx redraws the chip by itself
        // when the shared balance changes (after a session or a purchase).
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Obx(
              () => CoinChip(coins: Get.find<UserController>().user.value?.coins),
            ),
          ),
        ],
      ),
      drawer: const AppDrawer(),
      body: SafeArea(child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const LoadingView(message: 'Waking up Sparky...');
    }
    if (_error != null) {
      return ErrorView(message: _error!, onRetry: _loadUser);
    }
    
    return Padding(
      // 24 on top leaves room for the face's glow, so it isn't cut off.
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Flexible(
            flex: 3,
            child: FittedBox(
              fit: BoxFit.scaleDown,
              // Obx: Sparky changes outfit by himself when the Store equips something.
              child: Obx(() {
                final outfit = Get.find<OutfitController>();
                return DressedRobot(
                  expression: RobotExpression.ready,
                  glow: true,
                  animated: true, // alive while waiting for a session
                  hat: outfit.hat,
                  screen: outfit.screen,
                  effect: outfit.effect,
                );
              }),
            ),
          ),
          const SizedBox(height: 20),
          const Text(
            'Ready to Focus?',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 20),
          Flexible(
            flex: 4,
            child: FittedBox(
              fit: BoxFit.scaleDown,
              // Drag the knob around the clock to pick the minutes (5 to 60).
              child: DurationDial(
                minutes: _minutes,
                enabled: !_isStarting,
                onChanged: (m) => setState(() => _minutes = m),
              ),
            ),
          ),
          const SizedBox(height: 24),
          PrimaryButton(
            label: 'Start Session',
            onPressed: _startSession,
            isLoading: _isStarting,
          ),
          const SizedBox(height: 4),
          const _ModeLine(),
        ],
      ),
    );
  }
}

/// "With: Sparky robot · Change" under the Start button, so the user knows who will watch.
class _ModeLine extends StatelessWidget {
  const _ModeLine();

  @override
  Widget build(BuildContext context) {
    final modes = FocusModeController.to;
    return Obx(() {
      final phone = modes.usesPhone;
      return Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            phone ? Icons.phone_android : Icons.smart_toy_outlined,
            size: 16,
            color: AppColors.textMuted,
          ),
          const SizedBox(width: 6),
          Text(
            phone ? 'With: phone camera' : 'With: Sparky robot',
            style: const TextStyle(fontSize: 13, color: AppColors.textMuted),
          ),
          TextButton(
            onPressed: () => context.push(AppRoutes.focusMode),
            child: const Text('Change'),
          ),
        ],
      );
    });
  }
}
