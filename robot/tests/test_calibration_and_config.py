from pathlib import Path

import pytest

from kaboom_robot.config import load_config
from kaboom_robot.vision.calibration import Baseline
from kaboom_robot.vision.observation import NO_FACE, FaceObservation


def _obs(yaw, pitch=-10.0, gaze=0.05):
    return FaceObservation(True, ear=0.3, gaze=gaze, yaw=yaw, pitch=pitch, roll=0.0)


def test_baseline_learns_the_working_pose_and_subtracts_it():
    b = Baseline(seconds=3, min_samples=10)
    for i in range(40):  # 4 s at 10 fps, user turned 25 degrees toward the laptop
        b.add(i / 10, _obs(yaw=25.0))
    assert b.ready
    adjusted = b.apply(_obs(yaw=25.0))
    assert adjusted.yaw == pytest.approx(0.0)
    assert adjusted.pitch == pytest.approx(0.0)
    assert adjusted.gaze == pytest.approx(0.0)


def test_baseline_waits_for_a_face():
    b = Baseline(seconds=3, min_samples=10)
    for i in range(100):
        b.add(i / 10, NO_FACE)
    assert not b.ready


def test_config_reads_the_example_file():
    cfg = load_config(Path("config.example.yaml"))
    assert cfg.camera.source == "opencv"
    assert cfg.vision.thresholds.distracted_after_s == 5
    assert cfg.vision.model_path == Path("models/face_landmarker.task")


def test_config_ignores_unknown_keys(tmp_path):
    path = tmp_path / "config.yaml"
    path.write_text("camera:\n  index: 1\n  typo_key: 5\nvision:\n  thresholds:\n    yaw_deg: 40\n")
    cfg = load_config(path)
    assert cfg.camera.index == 1
    assert cfg.vision.thresholds.yaw_deg == 40
    assert cfg.vision.thresholds.pitch_deg == 20
