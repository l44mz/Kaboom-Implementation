"""State machine tests. Times are fake numbers, so these run instantly.

They follow the acceptance test for US-05 in docs/qa/test-plan.md.
"""

from kaboom_robot.vision.observation import NO_FACE, FaceObservation
from kaboom_robot.vision.state_machine import FocusState, FocusStateMachine

FPS = 10  # fake frames per second

LOOKING_AT_WORK = FaceObservation(True, ear=0.30, gaze=0.0, yaw=0.0, pitch=0.0, roll=0.0)
HEAD_TURNED = FaceObservation(True, ear=0.30, gaze=0.0, yaw=50.0, pitch=0.0, roll=0.0)
EYES_CLOSED = FaceObservation(True, ear=0.05, gaze=0.0, yaw=0.0, pitch=0.0, roll=0.0)
LOOKING_AT_PHONE = FaceObservation(True, ear=0.30, gaze=0.0, yaw=0.0, pitch=-35.0, roll=0.0)


def run(machine, obs, start, seconds):
    """Feed the same observation for `seconds`; return the state and the end time."""
    t = start
    state = machine.state
    for _ in range(int(seconds * FPS)):
        t += 1 / FPS
        state = machine.update(t, obs)
    return state, t


def test_starts_focused_and_stays_focused():
    m = FocusStateMachine()
    state, _ = run(m, LOOKING_AT_WORK, 0, 30)
    assert state == FocusState.FOCUSED


def test_head_turned_for_5_seconds_is_distracted():
    m = FocusStateMachine()
    state, t = run(m, HEAD_TURNED, 0, 4.5)
    assert state == FocusState.FOCUSED
    state, _ = run(m, HEAD_TURNED, t, 1.0)
    assert state == FocusState.DISTRACTED
    assert "head turned" in m.reasons


def test_looking_down_at_phone_is_distracted():
    m = FocusStateMachine()
    state, _ = run(m, LOOKING_AT_PHONE, 0, 6)
    assert state == FocusState.DISTRACTED


def test_a_quick_glance_does_not_change_the_state():
    m = FocusStateMachine()
    _, t = run(m, LOOKING_AT_WORK, 0, 5)
    _, t = run(m, HEAD_TURNED, t, 2)
    state, _ = run(m, LOOKING_AT_WORK, t, 5)
    assert state == FocusState.FOCUSED


def test_a_blink_does_not_change_the_state():
    m = FocusStateMachine()
    t = 0
    for _ in range(10):  # 10 blinks of 0.2 s, every 3 s
        _, t = run(m, LOOKING_AT_WORK, t, 2.8)
        state, t = run(m, EYES_CLOSED, t, 0.2)
        assert state == FocusState.FOCUSED


def test_eyes_closed_for_3_seconds_is_distracted():
    m = FocusStateMachine()
    state, _ = run(m, EYES_CLOSED, 0, 3.2)
    assert state == FocusState.DISTRACTED
    assert "eyes closed" in m.reasons


def test_leaving_the_desk_is_away():
    m = FocusStateMachine()
    state, t = run(m, NO_FACE, 0, 7)
    assert state == FocusState.FOCUSED
    state, _ = run(m, NO_FACE, t, 1.5)
    assert state == FocusState.AWAY


def test_focused_then_distracted_then_away():
    """Acceptance test US-05: the state goes FOCUSED, DISTRACTED, AWAY."""
    m = FocusStateMachine()
    seen = []
    t = 0
    for obs, seconds in [(LOOKING_AT_WORK, 5), (HEAD_TURNED, 6), (NO_FACE, 9)]:
        state, t = run(m, obs, t, seconds)
        seen.append(state)
    assert seen == [FocusState.FOCUSED, FocusState.DISTRACTED, FocusState.AWAY]


def test_coming_back_returns_to_focused_within_two_seconds():
    # 0.5 s grace (the face must really be back) + 1 s of looking fine.
    m = FocusStateMachine()
    _, t = run(m, NO_FACE, 0, 10)
    assert m.state == FocusState.AWAY
    state, t = run(m, LOOKING_AT_WORK, t, 1.0)
    assert state == FocusState.AWAY
    state, _ = run(m, LOOKING_AT_WORK, t, 1.0)
    assert state == FocusState.FOCUSED


def test_one_lost_frame_does_not_restart_the_distraction_clock():
    m = FocusStateMachine()
    _, t = run(m, HEAD_TURNED, 0, 3)
    t += 0.1
    m.update(t, FaceObservation(True, ear=0.30, gaze=0.0, yaw=0.0, pitch=0.0, roll=0.0))
    state, _ = run(m, HEAD_TURNED, t, 2.1)
    assert state == FocusState.DISTRACTED


def test_losing_the_face_while_distracted_stays_distracted_until_away():
    m = FocusStateMachine()
    _, t = run(m, HEAD_TURNED, 0, 6)
    state, t = run(m, NO_FACE, t, 5)
    assert state == FocusState.DISTRACTED
    state, _ = run(m, NO_FACE, t, 4)
    assert state == FocusState.AWAY


def test_losing_the_face_briefly_while_focused_stays_focused():
    m = FocusStateMachine()
    _, t = run(m, LOOKING_AT_WORK, 0, 5)
    _, t = run(m, NO_FACE, t, 3)
    state, _ = run(m, LOOKING_AT_WORK, t, 2)
    assert state == FocusState.FOCUSED


def test_unknown_values_do_not_count_as_distracted():
    m = FocusStateMachine()
    state, _ = run(m, FaceObservation(True), 0, 10)
    assert state == FocusState.FOCUSED
