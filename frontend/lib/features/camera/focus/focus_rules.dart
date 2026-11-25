import 'dart:math' as math;

import '../../session/models/focus_state.dart';
import 'face_observation.dart';

/// Turns camera frames into FOCUSED, DISTRACTED or AWAY on the phone.
///
/// This is a Dart copy of Sparky's rules (robot/kaboom_robot/vision/state_machine.py
/// and calibration.py), so both modes judge the user the same way:
/// - no face for 8 seconds: AWAY
/// - head turned (or looking down) for 5 seconds: DISTRACTED
/// - eyes closed for 3 seconds: DISTRACTED
/// - everything fine for 1 second: back to FOCUSED
/// A single frame is never trusted: a blink or a quick glance changes nothing.
///
/// Time [t] is passed in (seconds), so tests can use fake times instead of waiting.

/// All the numbers that decide the state. The same values as the robot's Thresholds.
class FocusThresholds {
  const FocusThresholds({
    this.eyeOpenClosed = 0.3,
    this.eyeOpenClosedRatio = 0.4,
    this.yawDeg = 30,
    this.pitchDeg = 20,
    this.rollDeg = 30,
    this.smoothingS = 0.15,
    this.eyesClosedAfterS = 3,
    this.distractedAfterS = 5,
    this.awayAfterS = 8,
    this.recoverAfterS = 1,
    this.graceS = 0.5,
  });

  /// Eyes count as closed under this (ML Kit gives about 0.9 for open eyes), before calibration.
  final double eyeOpenClosed;

  /// After calibration: closed under this share of the user's own open eyes.
  final double eyeOpenClosedRatio;

  final double yawDeg; // head turned left/right more than this
  final double pitchDeg; // head up/down (for example, looking at a phone on the desk)
  final double rollDeg; // head tilted to a shoulder
  final double smoothingS; // averages the numbers over about this long
  final double eyesClosedAfterS;
  final double distractedAfterS;
  final double awayAfterS;
  final double recoverAfterS; // how long all must look fine before FOCUSED again
  final double graceS; // gaps shorter than this do not break a streak
}

/// Measures for how long a yes/no signal has been true without a break.
/// Short "no" gaps (under [graceS]) are ignored, so a flickering detection
/// doesn't restart the clock.
class Streak {
  Streak(this.graceS);

  final double graceS;
  double? _start;
  double? _lastTrue;

  /// [value] null means "unknown this frame": keep what we had.
  void update(double t, bool? value) {
    if (value == null) return;
    if (value) {
      _start ??= t;
      _lastTrue = t;
    } else if (_start != null && t - _lastTrue! > graceS) {
      reset();
    }
  }

  void reset() {
    _start = null;
    _lastTrue = null;
  }

  double duration(double t) => _start == null ? 0 : t - _start!;
}

/// Averages each number over the last moments, so camera jitter near a
/// threshold doesn't make the head "turn" and "un-turn" many times a second.
/// It restarts when the face is lost.
class Smoother {
  Smoother(this.timeConstantS);

  final double timeConstantS;
  final _values = <String, double>{};
  double? _lastT;

  FaceObservation update(double t, FaceObservation obs) {
    if (!obs.facePresent || timeConstantS <= 0) {
      reset();
      return obs;
    }
    final last = _lastT;
    final a = last == null ? 1.0 : 1.0 - math.exp(-(t - last) / timeConstantS);
    _lastT = t;

    double? smooth(String name, double? value) {
      if (value == null) return null; // unknown this frame: leave it unknown
      final old = _values[name];
      final next = old == null ? value : old + a * (value - old);
      _values[name] = next;
      return next;
    }

    return FaceObservation(
      facePresent: true,
      yaw: smooth('yaw', obs.yaw),
      pitch: smooth('pitch', obs.pitch),
      roll: smooth('roll', obs.roll),
      eyeOpen: smooth('eyeOpen', obs.eyeOpen),
      eyeOpenRatio: smooth('eyeOpenRatio', obs.eyeOpenRatio),
    );
  }

  void reset() {
    _values.clear();
    _lastT = null;
  }
}

/// Learns the user's normal working pose in the first seconds of a session:
/// the phone may stand a bit to the side or below the face, so a focused user
/// isn't always looking straight at it. Every frame is then measured from that pose.
/// It also learns how open this user's eyes normally are.
class Baseline {
  Baseline({this.seconds = 3, this.minSamples = 10});

  final double seconds;
  final int minSamples;

  double yaw = 0;
  double pitch = 0;
  double roll = 0;
  double? eyeOpen; // this user's normal eyes, for "eyes closed"
  bool ready = false;

  double? _start;
  final _samples = <FaceObservation>[];

  /// Feed frames until ready. Frames without a face are skipped.
  void add(double t, FaceObservation obs) {
    if (ready || !obs.facePresent || obs.yaw == null) return;
    _start ??= t;
    _samples.add(obs);
    if (t - _start! >= seconds && _samples.length >= minSamples) {
      yaw = _median(_samples.map((o) => o.yaw));
      pitch = _median(_samples.map((o) => o.pitch));
      roll = _median(_samples.map((o) => o.roll));
      final eyes = _samples.map((o) => o.eyeOpen).whereType<double>().toList();
      // The median skips blinks during calibration. Very low values mean ML Kit
      // couldn't see the eyes well, so don't use them as "normal".
      final normalEyes = eyes.isEmpty ? null : _median(eyes);
      eyeOpen = normalEyes != null && normalEyes > 0.3 ? normalEyes : null;
      ready = true;
    }
  }

  /// The same frame, measured from the working pose.
  FaceObservation apply(FaceObservation obs) {
    if (!obs.facePresent) return obs;
    final normal = eyeOpen;
    final eyes = obs.eyeOpen;
    return FaceObservation(
      facePresent: true,
      yaw: obs.yaw == null ? null : obs.yaw! - yaw,
      pitch: obs.pitch == null ? null : obs.pitch! - pitch,
      roll: obs.roll == null ? null : obs.roll! - roll,
      eyeOpen: eyes,
      eyeOpenRatio: eyes != null && normal != null ? eyes / normal : null,
    );
  }

  static double _median(Iterable<double?> values) {
    final list = values.whereType<double>().toList()..sort();
    if (list.isEmpty) return 0;
    final mid = list.length ~/ 2;
    return list.length.isOdd ? list[mid] : (list[mid - 1] + list[mid]) / 2;
  }
}

/// The whole decision: calibration, then smoothing, then the timers.
class FocusRules {
  /// [calibrate] false skips the working-pose calibration (tests use it to
  /// check the timers alone, like the robot's state machine tests).
  FocusRules({this.thresholds = const FocusThresholds(), bool calibrate = true})
      : baseline = calibrate ? Baseline() : null {
    final g = thresholds.graceS;
    _smoother = Smoother(thresholds.smoothingS);
    _noFace = Streak(g);
    _eyesClosed = Streak(g);
    _headTurned = Streak(g);
    _focusedCandidate = Streak(0);
  }

  final FocusThresholds thresholds;
  final Baseline? baseline;

  late final Smoother _smoother;
  late final Streak _noFace;
  late final Streak _eyesClosed;
  late final Streak _headTurned;
  late final Streak _focusedCandidate;

  FocusState state = FocusState.focused;

  /// Why we are not FOCUSED ("no face", "eyes closed", "head turned"), for the test screen.
  List<String> reasons = const [];

  /// The last frame after calibration and smoothing, for the test screen.
  FaceObservation lastObservation = const FaceObservation.noFace();

  /// True once the working pose is learned (always false without calibration).
  bool get calibrated => baseline?.ready ?? false;

  /// Feed one frame from the camera. Returns the state.
  FocusState update(double t, FaceObservation raw) {
    final th = thresholds;
    final pose = baseline;
    pose?.add(t, raw);
    // Before calibration, measure from straight ahead (the phone usually faces the user).
    final measured = pose != null && pose.ready ? pose.apply(raw) : raw;
    final obs = _smoother.update(t, measured);
    lastObservation = obs;

    _noFace.update(t, !obs.facePresent);
    if (obs.facePresent) {
      _eyesClosed.update(t, _isEyesClosed(obs));
      _headTurned.update(t, _isHeadTurned(obs));
    } else {
      // No face means no eyes or head to judge; start those clocks fresh later.
      _eyesClosed.reset();
      _headTurned.reset();
    }

    final candidate = _candidate(t);

    if (!obs.facePresent) {
      // We can't see the user, so we can't say they look fine again.
      // Keep the current state until the AWAY timer decides.
      _focusedCandidate.reset();
      if (candidate == FocusState.away) state = FocusState.away;
      return state;
    }

    _focusedCandidate.update(t, candidate == FocusState.focused);
    if (candidate != FocusState.focused) {
      state = candidate;
    } else if (_focusedCandidate.duration(t) >= th.recoverAfterS) {
      state = FocusState.focused;
    }
    return state;
  }

  FocusState _candidate(double t) {
    final th = thresholds;
    final why = <String>[];
    if (_noFace.duration(t) >= th.awayAfterS) {
      reasons = const ['no face'];
      return FocusState.away;
    }
    if (_eyesClosed.duration(t) >= th.eyesClosedAfterS) why.add('eyes closed');
    if (_headTurned.duration(t) >= th.distractedAfterS) why.add('head turned');
    reasons = why;
    return why.isEmpty ? FocusState.focused : FocusState.distracted;
  }

  bool? _isEyesClosed(FaceObservation obs) {
    final ratio = obs.eyeOpenRatio;
    if (ratio != null) return ratio < thresholds.eyeOpenClosedRatio;
    final eyes = obs.eyeOpen;
    return eyes == null ? null : eyes < thresholds.eyeOpenClosed;
  }

  bool? _isHeadTurned(FaceObservation obs) {
    final yaw = obs.yaw;
    final pitch = obs.pitch;
    final roll = obs.roll;
    if (yaw == null || pitch == null || roll == null) return null;
    final th = thresholds;
    return yaw.abs() > th.yawDeg || pitch.abs() > th.pitchDeg || roll.abs() > th.rollDeg;
  }
}