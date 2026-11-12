import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/router/app_router.dart';
import '../../../core/storage/token_storage.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/controllers/user_controller.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../../../shared/widgets/error_view.dart';
import '../../../shared/widgets/loading_view.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../../shared/widgets/robot_face.dart';
import '../../session/services/session_service.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  // Durations the user can pick, in minutes. The backend's minimum is 10.
  static const _durations = [10, 15, 25, 45, 60];

  final _sessionService = SessionService(ApiClient(TokenStorage()));

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
      await context.read<UserController>().load();
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
      final session = await _sessionService.startSession(
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
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 32),
      child: Column(
        children: [
          const RobotFace(expression: RobotExpression.ready, glow: true),
          const SizedBox(height: 24),
          const Text(
            'Ready to Focus?',
            style: TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 32),
          _DurationRing(minutes: _minutes),
          const SizedBox(height: 24),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            alignment: WrapAlignment.center,
            children: [
              for (final m in _durations)
                ChoiceChip(
                  label: Text('$m min'),
                  selected: m == _minutes,
                  showCheckmark: false,
                  selectedColor: AppColors.secondary,
                  onSelected: _isStarting
                      ? null
                      : (_) => setState(() => _minutes = m),
                ),
            ],
          ),
          const SizedBox(height: 40),
          PrimaryButton(
            label: 'Start Session',
            onPressed: _startSession,
            isLoading: _isStarting,
          ),
        ],
      ),
    );
  }
}

/// The ring with the chosen time in the middle.
class _DurationRing extends StatelessWidget {
  const _DurationRing({required this.minutes});

  final int minutes;
  static const _maxMinutes = 60;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 260,
      height: 260,
      child: Stack(
        alignment: Alignment.center,
        children: [
          SizedBox.expand(
            child: CircularProgressIndicator(
              value: minutes / _maxMinutes,
              strokeWidth: 18,
              strokeCap: StrokeCap.round,
              backgroundColor: const Color(0xFF1F1F2A),
              color: AppColors.secondary,
            ),
          ),
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '$minutes:00',
                style: AppTheme.timerStyle.copyWith(
                  fontSize: 64,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Sparky is waiting\nfor you',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
