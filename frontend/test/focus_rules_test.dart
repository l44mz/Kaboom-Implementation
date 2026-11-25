import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/camera/focus/face_observation.dart';
import 'package:kaboom_app/features/camera/focus/focus_rules.dart';
import 'package:kaboom_app/features/session/models/focus_state.dart';

// The same tests as the robot's (robot/tests/test_state_machine.py), so the phone
// and Sparky judge the user the same way. Times are fake, so these run instantly.

const fps = 10; // fake frames per second

const lookingAtWork =
    FaceObservation(facePresent: true, yaw: 0, pitch: 0, roll: 0, eyeOpen: 0.9);
const headTurned =
    FaceObservation(facePresent: true, yaw: 50, pitch: 0, roll: 0, eyeOpen: 0.9);
const eyesClosed =
    FaceObservation(facePresent: true, yaw: 0, pitch: 0, roll: 0, eyeOpen: 0.05);
const lookingAtPhone =
    FaceObservation(facePresent: true, yaw: 0, pitch: -35, roll: 0, eyeOpen: 0.9);
const noFace = FaceObservation.noFace();

/// Feeds the same frame for [seconds]. Returns the end time; the state is rules.state.
double run(FocusRules rules, FaceObservation obs, double start, double seconds) {
  var t = start;
  for (var i = 0; i < (seconds * fps).round(); i++) {
    t += 1 / fps;
    rules.update(t, obs);
  }
  return t;
}

/// The timers alone, like the robot's state machine tests.
FocusRules timersOnly() => FocusRules(calibrate: false);

void main() {
  test('starts focused and stays focused', () {
    final r = timersOnly();
    run(r, lookingAtWork, 0, 30);
    expect(r.state, FocusState.focused);
  });

  test('head turned for 5 seconds is distracted', () {
    final r = timersOnly();
    final t = run(r, headTurned, 0, 4.5);
    expect(r.state, FocusState.focused);
    run(r, headTurned, t, 1);
    expect(r.state, FocusState.distracted);
    expect(r.reasons, contains('head turned'));
  });

  test('looking down at a phone is distracted', () {
    final r = timersOnly();
    run(r, lookingAtPhone, 0, 6);
    expect(r.state, FocusState.distracted);
  });

  test('a quick glance does not change the state', () {
    final r = timersOnly();
    var t = run(r, lookingAtWork, 0, 5);
    t = run(r, headTurned, t, 2);
    run(r, lookingAtWork, t, 5);
    expect(r.state, FocusState.focused);
  });

  test('a blink does not change the state', () {
    final r = timersOnly();
    double t = 0;
    for (var i = 0; i < 10; i++) {
      t = run(r, lookingAtWork, t, 2.8);
      t = run(r, eyesClosed, t, 0.2);
      expect(r.state, FocusState.focused);
    }
  });

  test('eyes closed for 3 seconds is distracted', () {
    final r = timersOnly();
    run(r, eyesClosed, 0, 3.2);
    expect(r.state, FocusState.distracted);
    expect(r.reasons, contains('eyes closed'));
  });

  test('leaving the desk is away', () {
    final r = timersOnly();
    final t = run(r, noFace, 0, 7);
    expect(r.state, FocusState.focused);
    run(r, noFace, t, 1.5);
    expect(r.state, FocusState.away);
  });

  test('focused, then distracted, then away (acceptance test US-05)', () {
    final r = timersOnly();
    final seen = <FocusState>[];
    double t = 0;
    for (final (obs, seconds) in [(lookingAtWork, 5.0), (headTurned, 6.0), (noFace, 9.0)]) {
      t = run(r, obs, t, seconds);
      seen.add(r.state);
    }
    expect(seen, [FocusState.focused, FocusState.distracted, FocusState.away]);
  });

  test('coming back returns to focused within two seconds', () {
    final r = timersOnly();
    var t = run(r, noFace, 0, 10);
    expect(r.state, FocusState.away);
    t = run(r, lookingAtWork, t, 1);
    expect(r.state, FocusState.away);
    run(r, lookingAtWork, t, 1);
    expect(r.state, FocusState.focused);
  });

  test('one lost frame does not restart the distraction clock', () {
    final r = timersOnly();
    var t = run(r, headTurned, 0, 3);
    t += 0.1;
    r.update(t, lookingAtWork);
    run(r, headTurned, t, 2.1);
    expect(r.state, FocusState.distracted);
  });

  test('losing the face while distracted stays distracted until away', () {
    final r = timersOnly();
    var t = run(r, headTurned, 0, 6);
    t = run(r, noFace, t, 5);
    expect(r.state, FocusState.distracted);
    run(r, noFace, t, 4);
    expect(r.state, FocusState.away);
  });

  test('losing the face briefly while focused stays focused', () {
    final r = timersOnly();
    var t = run(r, lookingAtWork, 0, 5);
    t = run(r, noFace, t, 3);
    run(r, lookingAtWork, t, 2);
    expect(r.state, FocusState.focused);
  });

  test('unknown values do not count as distracted', () {
    final r = timersOnly();
    run(r, const FaceObservation(facePresent: true), 0, 10);
    expect(r.state, FocusState.focused);
  });

  group('calibration', () {
    test('a phone standing to the side is learned as the normal pose', () {
      // The phone is beside the laptop: a focused user has the head turned 35 degrees.
      const workingFromTheSide =
          FaceObservation(facePresent: true, yaw: 35, pitch: 0, roll: 0, eyeOpen: 0.9);
      final r = FocusRules();
      run(r, workingFromTheSide, 0, 20);
      expect(r.calibrated, isTrue);
      expect(r.state, FocusState.focused);
    });

    test('narrow eyes are judged against the user\'s own eyes', () {
      // This user's open eyes only reach 0.5, which is still "open" for them.
      const narrowEyes =
          FaceObservation(facePresent: true, yaw: 0, pitch: 0, roll: 0, eyeOpen: 0.5);
      const narrowEyesClosed =
          FaceObservation(facePresent: true, yaw: 0, pitch: 0, roll: 0, eyeOpen: 0.1);
      final r = FocusRules();
      final t = run(r, narrowEyes, 0, 20);
      expect(r.state, FocusState.focused);
      run(r, narrowEyesClosed, t, 3.5);
      expect(r.state, FocusState.distracted);
    });
  });
}