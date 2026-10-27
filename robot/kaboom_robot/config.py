"""Loads robot settings from config.yaml (falls back to config.example.yaml)."""

from dataclasses import dataclass, field, fields
from pathlib import Path

import yaml

from kaboom_robot.vision.state_machine import Thresholds


@dataclass(frozen=True)
class CameraConfig:
    source: str = "opencv"  # "opencv" (laptop or USB webcam) or "picamera2" (Pi Camera Module)
    index: int = 0
    width: int = 640
    height: int = 480


@dataclass(frozen=True)
class VisionConfig:
    model_path: Path = Path("models/face_landmarker.task")
    calibration_s: float = 3.0
    thresholds: Thresholds = field(default_factory=Thresholds)


@dataclass(frozen=True)
class RobotConfig:
    camera: CameraConfig = field(default_factory=CameraConfig)
    vision: VisionConfig = field(default_factory=VisionConfig)


def _pick(cls, data: dict | None) -> dict:
    """Keep only the keys this dataclass knows, so a typo is ignored instead of crashing."""
    names = {f.name for f in fields(cls)}
    return {k: v for k, v in (data or {}).items() if k in names}


def load_config(path: Path | None = None) -> RobotConfig:
    if path is None:
        path = Path("config.yaml") if Path("config.yaml").is_file() else Path("config.example.yaml")
    data = yaml.safe_load(path.read_text()) if path.is_file() else {}
    data = data or {}

    vision = data.get("vision") or {}
    vision_args = _pick(VisionConfig, vision)
    if "model_path" in vision_args:
        vision_args["model_path"] = Path(vision_args["model_path"])
    vision_args["thresholds"] = Thresholds(**_pick(Thresholds, vision.get("thresholds")))

    return RobotConfig(
        camera=CameraConfig(**_pick(CameraConfig, data.get("camera"))),
        vision=VisionConfig(**vision_args),
    )
