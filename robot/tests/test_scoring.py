"""Tests for the accuracy scoring. Fake recordings at 10 frames per second."""

import pytest

from kaboom_robot.vision import scoring
from kaboom_robot.vision.scoring import Sample

FPS = 10
DELAYS = {"FOCUSED": 3.0, "DISTRACTED": 6.5, "AWAY": 9.5}


def recording(*parts):
    """parts: (truth, predicted, seconds). Builds one sample per frame."""
    samples, t = [], 0.0
    for truth, predicted, seconds in parts:
        for _ in range(int(seconds * FPS)):
            samples.append(Sample(round(t, 3), truth, predicted))
            t += 1 / FPS
    return samples


def test_perfect_robot_scores_100_percent():
    rec = recording(("FOCUSED", "FOCUSED", 20), ("AWAY", "AWAY", 20))
    report = scoring.score([rec], DELAYS)
    assert report.accuracy == pytest.approx(1.0)


def test_robot_waiting_its_allowed_delay_is_not_a_mistake():
    rec = recording(
        ("FOCUSED", "FOCUSED", 20),
        ("DISTRACTED", "FOCUSED", 5),  # robot still waiting: allowed
        ("DISTRACTED", "DISTRACTED", 15),
    )
    report = scoring.score([rec], DELAYS)
    assert report.accuracy == pytest.approx(1.0)
    assert report.frames_waiting > 0
    assert report.detection_rate("DISTRACTED") == pytest.approx(1.0)
    assert report.mean_delay("DISTRACTED") == pytest.approx(5.0, abs=0.11)


def test_missed_distraction_lowers_the_score():
    rec = recording(("FOCUSED", "FOCUSED", 20), ("DISTRACTED", "FOCUSED", 20))
    report = scoring.score([rec], DELAYS)
    assert report.accuracy < 0.7
    assert report.detection_rate("DISTRACTED") == 0.0
    assert report.confusion["DISTRACTED"]["FOCUSED"] > 0


def test_a_quick_glance_is_not_counted_as_a_miss():
    rec = recording(
        ("FOCUSED", "FOCUSED", 20),
        ("DISTRACTED", "FOCUSED", 2),  # too short for the robot to react, by design
        ("FOCUSED", "FOCUSED", 20),
    )
    report = scoring.score([rec], DELAYS)
    assert report.detection_rate("DISTRACTED") is None
    assert report.accuracy == pytest.approx(1.0)


def test_calibration_frames_are_ignored():
    rec = recording(("FOCUSED", "CALIBRATING", 3), ("FOCUSED", "FOCUSED", 10))
    report = scoring.score([rec], DELAYS)
    assert report.frames_scored + report.frames_waiting == 100


def test_several_users_are_scored_together():
    good = recording(("FOCUSED", "FOCUSED", 20))
    bad = recording(("FOCUSED", "DISTRACTED", 20))
    report = scoring.score([good, bad], DELAYS)
    assert report.accuracy == pytest.approx(0.5, abs=0.05)


def test_csv_round_trip_and_markdown(tmp_path):
    path = tmp_path / "rec.csv"
    rows = [
        {
            "t": "0.000",
            "truth": "FOCUSED",
            "predicted": "FOCUSED",
            "ear": "0.3",
            "gaze": "",
            "yaw": "",
            "pitch": "",
            "roll": "",
        },
        {
            "t": "0.100",
            "truth": "",
            "predicted": "FOCUSED",
            "ear": "",
            "gaze": "",
            "yaw": "",
            "pitch": "",
            "roll": "",
        },
    ]
    scoring.write_csv(path, rows)
    samples = scoring.read_csv(path)
    assert samples == [Sample(0.0, "FOCUSED", "FOCUSED"), Sample(0.1, None, "FOCUSED")]
    text = scoring.to_markdown(scoring.score([samples], DELAYS))
    assert "Overall accuracy" in text
    assert "| FOCUSED |" in text
