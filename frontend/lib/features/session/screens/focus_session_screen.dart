import 'dart:async';
import 'dart:math' as math;

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:get/get.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

import '../../../core/network/api_client.dart';
import '../../../core/notifications/session_notifications.dart';
import '../../../core/router/app_router.dart';
import '../../../core/storage/token_storage.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/controllers/user_controller.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/robot_face.dart';
import '../../camera/services/phone_focus_detector.dart';
import '../models/focus_session.dart';
import '../models/focus_state.dart';
import '../models/session_clock.dart';
import '../models/session_result.dart';
import '../services/camera_session_service.dart';
import '../services/session_service.dart';

/// The running session (Figma "Focused / Distracted / Away Status In Session").
///
/// Robot mode: Sparky watches and sends the states; this screen asks the backend for them.
/// Camera mode: the phone's front camera watches (PhoneFocusDetector); this screen
/// shows the states and sends them to the backend itself.
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

class _FocusSessionScreenState extends State<FocusSessionScreen>
    with WidgetsBindingObserver {
  // Camera sessions pause, resume and end through /camera-sessions.
  late final SessionService _service = widget.service ??
      (widget.session?.isCamera ?? false
          ? CameraSessionService(ApiClient(TokenStorage()))
          : SessionService(ApiClient(TokenStorage())));

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

  /// The phone notification with the timer (Android only, does nothing elsewhere).
  final _notifications = SessionNotifications.instance;

  // ---- Camera mode only ----

  /// Watches the user with the front camera. Null in robot mode.
  PhoneFocusDetector? _detector;
  StreamSubscription<FocusState>? _cameraSub;
  Timer? _heartbeat;
  bool _cameraStarting = false;

  /// Why the camera isn't watching (no permission, no camera). Null while it works.
  String? _cameraProblem;

  /// Goes up by one for every state sent, so the backend ignores old or repeated ones.
  int _sequence = 0;

  /// True once the backend said Sparky exploded, so it's shown only once.
  bool _exploded = false;

  /// Sparky shows the KABOOM face until this time, then goes back to normal.
  DateTime? _kaboomUntil;

  bool get _showingKaboom {
    final until = _kaboomUntil;
    return until != null && DateTime.now().isBefore(until);
  }

  bool get _isCamera => widget.session?.isCamera ?? false;

  @override
  void initState() {
    super.initState();
    final session = widget.session;
    if (session == null) return;

    // The clock starts now, on the phone, not at the server's startTime.
    // The server sends its own local time with no time zone (for example
    // "2026-11-24T17:00:00" from Render, which runs in UTC). On a phone in
    // Riyadh (UTC+3) that looked 3 hours old, so the session ended at once.
    // The screen opens right after the session starts, so now is the real start.
    _clock = SessionClock(
      startTime: DateTime.now(),
      plannedSeconds: session.plannedDurationSeconds,
    );
    _ticker = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
    unawaited(_startNotification());

    final given = widget.states;
    if (given != null) {
      _stateSub = given.listen(_onState); // tests
    } else if (session.isCamera) {
      // The phone watches. Keep the screen on, or the phone would lock and stop the camera.
      WidgetsBinding.instance.addObserver(this);
      unawaited(WakelockPlus.enable());
      unawaited(_startCamera());
      // A "still here" message every 10 seconds, like the robot, so the backend keeps
      // counting the time in the current state even when nothing changes.
      _heartbeat = Timer.periodic(
        const Duration(seconds: 10),
        (_) => _sendState(heartbeat: true),
      );
    } else {
      _stateSub = _service.focusStates(session.id).listen(_onState); // Sparky
    }
  }

  @override
  void dispose() {
    _ticker?.cancel();
    _stateSub?.cancel();
    _stopCamera();
    super.dispose();
  }

  void _onState(FocusState state) {
    if (!mounted) return;
    setState(() {
      _state = state;
      _hasRobotState = true;
    });
  }

  // ---- Camera mode ----

  Future<void> _startCamera() async {
    if (_cameraStarting || _finishing) return;
    _cameraStarting = true;
    final detector = _detector ??= PhoneFocusDetector();
    try {
      await detector.start(); // asks for the camera permission the first time
      _cameraSub ??= detector.states.listen((state) {
        _onState(state);
        _sendState(); // tell the backend at once when the state changes
      });
      if (mounted) setState(() => _cameraProblem = null);
    } on CameraException catch (e) {
      if (mounted) {
        setState(() => _cameraProblem = e.code == 'CameraAccessDenied'
            ? 'Kaboom needs the camera to watch your focus. Allow it in the phone settings.'
            : 'The camera could not start (${e.code}).');
      }
    } catch (e) {
      if (mounted) setState(() => _cameraProblem = 'The camera could not start.');
    } finally {
      _cameraStarting = false;
    }
  }

  void _stopCamera() {
    final detector = _detector;
    if (detector == null) return;
    _detector = null;
    _heartbeat?.cancel();
    _cameraSub?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    unawaited(WakelockPlus.disable());
    unawaited(detector.dispose());
  }

  /// Android takes the camera away when the app goes to the background,
  /// so stop it then, and start it again when the user comes back.
  @override
  void didChangeAppLifecycleState(AppLifecycleState lifecycle) {
    final detector = _detector;
    if (detector == null || _finishing) return;
    if (lifecycle == AppLifecycleState.paused) {
      unawaited(detector.stop());
      setState(() {}); // hide the preview of the stopped camera
    } else if (lifecycle == AppLifecycleState.resumed && detector.camera == null) {
      unawaited(_startCamera());
    }
  }

  /// Sends the current state to the backend (camera mode only).
  Future<void> _sendState({bool heartbeat = false}) async {
    final service = _service;
    if (service is! CameraSessionService) return;
    if (_finishing || !_hasRobotState || _clock!.isPaused) return;
    try {
      final exploded = await service.sendState(
        widget.session!.id,
        state: _state,
        sequence: ++_sequence,
        heartbeat: heartbeat,
      );
      if (exploded && !_exploded && mounted) {
        _exploded = true;
        _showExplosion();
      }
    } catch (e) {
      debugPrint('Could not send the camera state: $e');
    }
  }

  /// Sparky explodes on screen: the KABOOM face shakes for 5 seconds,
  /// then the normal face comes back (the 1-second timer redraws the screen).
  void _showExplosion() {
    setState(() => _kaboomUntil = DateTime.now().add(const Duration(seconds: 5)));
  }

  /// The moment the session will end if it isn't paused again.
  DateTime _endTime(DateTime now) =>
      now.add(Duration(seconds: _clock!.remainingSeconds(now)));

  Future<void> _startNotification() async {
    await _notifications.askPermission();
    if (!mounted || _finishing || _clock!.isPaused) return;
    await _notifications.showRunning(_endTime(DateTime.now()));
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
    // Camera mode: after a resume, tell the backend the current state right away.
    if (!pausing) unawaited(_sendState());
    // Paused: the notification stops counting. Resumed: it counts to the new end time.
    unawaited(pausing
        ? _notifications.showPaused(_clock!.remainingSeconds(now))
        : _notifications.showRunning(_endTime(now)));
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
    _stopCamera();
    unawaited(_notifications.sessionEnded(endedEarly: endedEarly));

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
    if (Get.isRegistered<UserController>()) Get.find<UserController>().reload();
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

    // Everything fits on one screen, with no scrolling (same idea as Home):
    // the face and the ring are Flexible + FittedBox, so on a short phone they
    // shrink to leave room for the buttons. On a tall phone they keep their size.
    return Padding(
      padding: const EdgeInsets.fromLTRB(32, 16, 32, 24),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Flexible(
            flex: 3,
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: _showingKaboom
                  ? const _Shake(child: RobotFace(expression: RobotExpression.kaboom))
                  : RobotFace(expression: _expressionFor(_state)),
            ),
          ),
          if (_isCamera) ...[
            const SizedBox(height: 12),
            _CameraBadge(detector: _detector, problem: _cameraProblem),
          ],
          const SizedBox(height: 16),
          _hasRobotState ? _StatusChip(state: _state) : const _WaitingChip(),
          const SizedBox(height: 20),
          Flexible(
            flex: 5,
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: _TimerRing(
                fraction: clock.remainingFraction(now),
                time: SessionClock.format(clock.remainingSeconds(now)),
                message: clock.isPaused
                    ? 'Session paused'
                    : _showingKaboom
                        ? 'KABOOM! Too many distractions. Back to work!'
                        : _hasRobotState
                        ? _state.message
                        : _isCamera
                            ? 'Looking for you...'
                            : 'Waiting for Sparky...',
              ),
            ),
          ),
          const SizedBox(height: 20),
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
          const SizedBox(height: 20),
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
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
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

/// Shakes its child left and right, strongly at first, then calmer (the explosion).
class _Shake extends StatelessWidget {
  const _Shake({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 1200),
      builder: (context, t, child) => Transform.translate(
        offset: Offset(math.sin(t * math.pi * 14) * 12 * (1 - t), 0),
        child: child,
      ),
      child: child,
    );
  }
}

/// Camera mode: a small round preview, so the user can check the phone sees them.
/// Or the reason the camera isn't working.
class _CameraBadge extends StatelessWidget {
  const _CameraBadge({required this.detector, required this.problem});

  final PhoneFocusDetector? detector;
  final String? problem;

  @override
  Widget build(BuildContext context) {
    final text = problem;
    if (text != null) {
      return Text(
        text,
        textAlign: TextAlign.center,
        style: const TextStyle(fontSize: 12, color: AppColors.away),
      );
    }
    final camera = detector?.camera;
    if (camera == null || !camera.value.isInitialized) {
      return const SizedBox(width: 64, height: 64);
    }
    return ClipOval(
      child: SizedBox(
        width: 64,
        height: 64,
        child: FittedBox(
          fit: BoxFit.cover,
          child: SizedBox(
            width: 100,
            height: 100 * camera.value.aspectRatio,
            child: CameraPreview(camera),
          ),
        ),
      ),
    );
  }
}

/// Before the first state arrives: not "Focused" yet, because nobody has looked.
class _WaitingChip extends StatelessWidget {
  const _WaitingChip();

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minWidth: 131),
      height: 35,
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border, width: 2),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.hourglass_empty, size: 18),
          SizedBox(width: 6),
          Text('Waiting', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

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
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
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
                  fontSize: 14,
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