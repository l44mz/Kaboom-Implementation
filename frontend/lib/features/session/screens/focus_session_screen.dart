import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/router/app_router.dart';
import '../../../core/storage/token_storage.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/controllers/user_controller.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/robot_face.dart';
import '../models/focus_session.dart';
import '../models/focus_state.dart';
import '../models/session_clock.dart';
import '../models/session_result.dart';
import '../services/session_service.dart';

/// The running session (Figma "Focused / Distracted / Away Status In Session").
class FocusSessionScreen extends StatefulWidget {
  const FocusSessionScreen({super.key, this.session, this.states, this.service});

  /// The session just started on Home, passed through the router's `extra`.
  final FocusSession? session;

  /// Robot states to show. Defaults to the real states from the backend
  /// (SessionService.focusStates). Tests pass their own stream.
  final Stream<FocusState>? states;

  /// The API calls for pause, resume and end. Defaults to a real SessionService.
  final SessionService? service;

  @override
  State<FocusSessionScreen> createState() => _FocusSessionScreenState();
}

class _FocusSessionScreenState extends State<FocusSessionScreen> {
  late final SessionService _service =
      widget.service ?? SessionService(ApiClient(TokenStorage()));

  SessionClock? _clock;
  Timer? _ticker;
  StreamSubscription<FocusState>? _stateSub;
  FocusState _state = FocusState.focused;

  /// False until the robot sends its first state, so we can say we're waiting for it.
  bool _hasRobotState = false;

  /// True once the session is ending, so it only ends once.
  bool _finishing = false;

  // Seconds spent FOCUSED and not paused, for the summary.
  int _focusedSeconds = 0;

  @override
  void initState() {
    super.initState();
    final session = widget.session;
    if (session == null) return;

    _clock = SessionClock(
      startTime: session.startTime,
      plannedSeconds: session.plannedDurationSeconds,
    );
    _ticker = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
    _stateSub = (widget.states ?? _service.focusStates(session.id)).listen((state) {
      if (!mounted) return;
      setState(() {
        _state = state;
        _hasRobotState = true;
      });
    });
  }

  @override
  void dispose() {
    _ticker?.cancel();
    _stateSub?.cancel();
    super.dispose();
  }

  void _tick() {
    if (!mounted || _finishing) return;
    final clock = _clock!;
    if (!clock.isPaused && _hasRobotState && _state == FocusState.focused) {
      _focusedSeconds++;
    }
    if (clock.isFinished(DateTime.now())) {
      _finish(endedEarly: false);
    } else {
      setState(() {});
    }
  }

  void _togglePause() {
    final now = DateTime.now();
    final pausing = !_clock!.isPaused;
    // Update the screen right away, then tell the backend (which also tells the robot).
    setState(() => pausing ? _clock!.pause(now) : _clock!.resume(now));
    unawaited(_syncPause(pausing));
  }

  Future<void> _syncPause(bool pausing) async {
    final id = widget.session!.id;
    try {
      if (pausing) {
        await _service.pauseSession(id);
      } else {
        await _service.resumeSession(id);
      }
    } catch (e) {
      debugPrint('Could not ${pausing ? 'pause' : 'resume'} the session: $e');
    }
  }

  Future<void> _confirmEnd() async {
    final end = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.surface,
        title: const Text('End session early?'),
        content: const Text('Your focus time so far will still count.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Keep going'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('End session', style: TextStyle(color: AppColors.away)),
          ),
        ],
      ),
    );
    if (end == true) _finish(endedEarly: true);
  }

  Future<void> _finish({required bool endedEarly}) async {
    if (_finishing) return;
    _finishing = true;
    _ticker?.cancel();
    _stateSub?.cancel();

    // The backend ends the session, stops the robot, and decides the coins.
    // If it can't be reached, fall back to the app's own count.
    var result = SessionResult.local(
      focusedSeconds: _focusedSeconds,
      endedEarly: endedEarly,
    );
    try {
      final coins = await _service.endSession(widget.session!.id);
      result = SessionResult(
        focusedSeconds: _focusedSeconds,
        coinsEarned: coins,
        endedEarly: endedEarly,
      );
    } catch (e) {
      debugPrint('Could not end the session on the backend: $e');
    }

    if (!mounted) return;
    // The backend added the coins when the session ended, so reload them for the side menu.
    context.read<UserController?>()?.refresh();
    context.go(AppRoutes.sessionComplete, extra: result);
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
      body: SafeArea(child: _clock == null ? _buildNoSession() : _buildSession()),
    );
  }

  /// Shown if the screen opens without a session (for example a page refresh on web).
  Widget _buildNoSession() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('No session is running.'),
          const SizedBox(height: 16),
          OutlinedButton(
            onPressed: () => context.go(AppRoutes.home),
            child: const Text('Back to Home'),
          ),
        ],
      ),
    );
  }

  Widget _buildSession() {
    final now = DateTime.now();
    final clock = _clock!;

    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 32),
      child: Column(
        children: [
          RobotFace(expression: _expressionFor(_state)),
          const SizedBox(height: 27),
          _StatusChip(state: _state),
          const SizedBox(height: 39),
          // Shrinks the 317 px ring on phones narrower than the Figma frame.
          FittedBox(
            fit: BoxFit.scaleDown,
            child: _TimerRing(
              fraction: clock.remainingFraction(now),
              time: SessionClock.format(clock.remainingSeconds(now)),
              message: clock.isPaused
                  ? 'Session paused'
                  : _hasRobotState
                      ? _state.message
                      : 'Waiting for Sparky...',
            ),
          ),
          const SizedBox(height: 23),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _RoundIconButton(
                icon: Icons.pause_circle_outline,
                tooltip: 'Pause',
                onPressed: clock.isPaused ? null : _togglePause,
              ),
              const SizedBox(width: 18),
              _RoundIconButton(
                icon: Icons.play_circle_outline,
                tooltip: 'Resume',
                onPressed: clock.isPaused ? _togglePause : null,
              ),
            ],
          ),
          const SizedBox(height: 23),
          SizedBox(
            width: 256,
            height: 52,
            child: OutlinedButton(
              onPressed: _confirmEnd,
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.textPrimary,
                side: const BorderSide(color: AppColors.secondary, width: 2),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              child: const Text(
                'End Session',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// The robot face that matches each focus state.
RobotExpression _expressionFor(FocusState state) => switch (state) {
      FocusState.focused => RobotExpression.focused,
      FocusState.distracted => RobotExpression.distracted,
      FocusState.away => RobotExpression.away,
    };

/// The pill under the face: Focused, Distracted or Away.
class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.state});

  final FocusState state;

  @override
  Widget build(BuildContext context) {
    // At least Figma's 131 px, but wider if the text needs it (larger fonts, Arabic).
    return Container(
      constraints: const BoxConstraints(minWidth: 131),
      height: 35,
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: state.chipColor,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: state.chipBorder, width: 2),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          switch (state) {
            FocusState.focused => const Icon(Icons.visibility_outlined, size: 20),
            FocusState.distracted => const CustomPaint(
                size: Size(20, 20),
                painter: _ClosedEyePainter(),
              ),
            FocusState.away => const Icon(Icons.visibility_off_outlined, size: 20),
          },
          const SizedBox(width: 6),
          Text(
            state.label,
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}

/// A closed eye with lashes, like Figma's distracted icon (Material has none).
class _ClosedEyePainter extends CustomPainter {
  const _ClosedEyePainter();

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppColors.textPrimary
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.8
      ..strokeCap = StrokeCap.round;
    final w = size.width;
    final h = size.height;
    canvas.drawArc(
      Rect.fromLTWH(w * 0.1, h * 0.05, w * 0.8, h * 0.6),
      0.2,
      math.pi - 0.4,
      false,
      paint,
    );
    for (final x in [0.25, 0.5, 0.75]) {
      canvas.drawLine(
        Offset(w * x, h * 0.62),
        Offset(w * x + (x - 0.5) * 4, h * 0.85),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(_ClosedEyePainter old) => false;
}

/// The 317 x 317 ring: time left as a purple to blue arc, with the timer inside.
class _TimerRing extends StatelessWidget {
  const _TimerRing({
    required this.fraction,
    required this.time,
    required this.message,
  });

  final double fraction;
  final String time;
  final String message;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 317,
      height: 317,
      child: CustomPaint(
        painter: _RingPainter(fraction),
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                time,
                style: AppTheme.timerStyle.copyWith(
                  fontSize: 76,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                message,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RingPainter extends CustomPainter {
  _RingPainter(this.fraction);

  final double fraction;

  static const _stroke = 18.0;
  static const _track = Color(0xFF1E1E1E);
  static const _gradientStart = Color(0xFF8B5CF6);

  @override
  void paint(Canvas canvas, Size size) {
    final rect = (Offset.zero & size).deflate(_stroke / 2);
    canvas.drawOval(
      rect,
      Paint()
        ..color = _track
        ..style = PaintingStyle.stroke
        ..strokeWidth = _stroke,
    );
    if (fraction <= 0) return;

    canvas.drawArc(
      rect,
      -math.pi / 2,
      2 * math.pi * fraction,
      false,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = _stroke
        ..strokeCap = StrokeCap.round
        // Purple at the top to blue at the bottom, like Figma.
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [_gradientStart, AppColors.primary],
        ).createShader(rect),
    );
  }

  @override
  bool shouldRepaint(_RingPainter old) => old.fraction != fraction;
}

/// Pause or resume. Greyed out when it doesn't apply.
class _RoundIconButton extends StatelessWidget {
  const _RoundIconButton({
    required this.icon,
    required this.tooltip,
    required this.onPressed,
  });

  final IconData icon;
  final String tooltip;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: onPressed,
      tooltip: tooltip,
      iconSize: 39,
      color: AppColors.secondary,
      disabledColor: AppColors.secondary.withValues(alpha: 0.35),
      icon: Icon(icon),
    );
  }
}
