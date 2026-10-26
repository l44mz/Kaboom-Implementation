import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/router/app_router.dart';
import '../../../core/storage/token_storage.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/models/user.dart';
import '../../../shared/services/user_service.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../../../shared/widgets/error_view.dart';
import '../../../shared/widgets/loading_view.dart';
import '../../../shared/widgets/primary_button.dart';
import '../../session/services/session_service.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  // Durations the user can pick, in minutes. The backend's minimum is 10.
  static const _durations = [10, 15, 25, 45, 60];

  final _api = ApiClient(TokenStorage());
  late final _userService = UserService(_api);
  late final _sessionService = SessionService(_api);

  User? _user;
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
      final user = await _userService.getMe();
      if (!mounted) return;
      setState(() => _user = user);
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
      showAppSnackBar(context, apiErrorMessage(e), isError: true);
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
      drawer: AppDrawer(user: _user),
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
          const _RobotFace(),
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

/// The robot's face in a rounded card.
class _RobotFace extends StatelessWidget {
  const _RobotFace();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(28),
      ),
      child: Image.asset('assets/images/Robot.png', height: 140),
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
