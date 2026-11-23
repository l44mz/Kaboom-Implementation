import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/app_lottie.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../account/models/robot_info.dart';
import '../controllers/pairing_controller.dart';
import '../services/robot_service.dart';
import '../widgets/code_input.dart';

/// Connect Robot: the user types the 6-digit code that Sparky shows on its screen.
/// When it works, the robot belongs to the user and starts watching their sessions.
class RobotPairingScreen extends StatefulWidget {
  const RobotPairingScreen({super.key, this.service});

  /// Only for tests: a fake service instead of the real backend.
  final RobotService? service;

  @override
  State<RobotPairingScreen> createState() => _RobotPairingScreenState();
}

class _RobotPairingScreenState extends State<RobotPairingScreen> {
  final _code = TextEditingController();
  final _focus = FocusNode();

  // Created on first use (in build), so it can read widget.service.
  late final PairingController _controller =
      Get.put(PairingController(service: widget.service));

  @override
  void dispose() {
    _code.dispose();
    _focus.dispose();
    // go_router closes this page, not GetX, so delete the controller ourselves.
    Get.delete<PairingController>();
    super.dispose();
  }

  Future<void> _connect() async {
    if (_controller.isPairing.value) return; // already sending (button + 6th digit)
    final ok = await _controller.pair(_code.text);
    if (!mounted || ok) return;
    // Wrong code: empty the boxes so the user can type it again.
    _code.clear();
    _focus.requestFocus();
  }

  void _done() {
    // true tells the page that opened this one (My Account) that a robot was added.
    if (context.canPop()) {
      context.pop(true);
    } else {
      context.go(AppRoutes.home);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        foregroundColor: AppColors.textPrimary,
      ),
      body: SafeArea(
        child: Obx(() {
          final robot = _controller.pairedRobot.value;
          return robot == null ? _buildForm() : _buildSuccess(robot);
        }),
      ),
    );
  }

  Widget _buildForm() {
    final error = _controller.errorMessage.value;
    final pairing = _controller.isPairing.value;

    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(32, 0, 32, 32),
      child: Column(
        children: [
          const AppLottie(AppAnimation.pairing, width: 150, height: 150),
          const SizedBox(height: 8),
          const Text(
            'Connect Sparky',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 16),
          const _Step(number: 1, text: 'Turn on Sparky and wait about a minute.'),
          const _Step(number: 2, text: 'Its screen shows a 6-digit code.'),
          const _Step(number: 3, text: 'Type the code here.'),
          const SizedBox(height: 24),
          CodeInput(
            controller: _code,
            focusNode: _focus,
            length: PairingController.codeLength,
            hasError: error != null,
            onChanged: (_) => _controller.clearError(),
            onCompleted: (_) => _connect(),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 40,
            child: error == null
                ? null
                : Text(
                    error,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 13, color: AppColors.away),
                  ),
          ),
          const SizedBox(height: 8),
          PrimaryButton(label: 'Connect', onPressed: _connect, isLoading: pairing),
          const SizedBox(height: 16),
          const Text(
            'No code on the screen? Check that Sparky and this phone use the same Wi-Fi, '
            'and that the code is not older than 10 minutes.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12, color: AppColors.textMuted),
          ),
        ],
      ),
    );
  }

  Widget _buildSuccess(RobotInfo robot) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const AppLottie(AppAnimation.confetti, width: 180, height: 180, repeat: false),
            const Text(
              'Sparky is yours!',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              robot.name,
              style: const TextStyle(fontSize: 15, color: AppColors.textMuted),
            ),
            const SizedBox(height: 16),
            const Text(
              'Start a focus session and Sparky will watch with you.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, color: AppColors.textPrimary),
            ),
            const SizedBox(height: 32),
            PrimaryButton(label: 'Done', onPressed: _done),
          ],
        ),
      ),
    );
  }
}

/// One numbered line of the instructions.
class _Step extends StatelessWidget {
  const _Step({required this.number, required this.text});

  final int number;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          CircleAvatar(
            radius: 12,
            backgroundColor: AppColors.primary,
            child: Text(
              '$number',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(fontSize: 14, color: AppColors.textPrimary),
            ),
          ),
        ],
      ),
    );
  }
}