"""Scores the focus detection against what really happened.

During a test session a person says what the user is really doing (the "truth":
FOCUSED, DISTRACTED or AWAY) while the robot logs what it detected. This module
compares the two. No camera and no MediaPipe here, so it is easy to test.

The robot is slow on purpose: it waits 3 s before saying DISTRACTED and 3 s before
AWAY, so a blink or a quick glance does not count. So right after the truth changes,
we give the robot that much time before we start comparing. Those frames are counted
as "waiting", not as mistakes.
"""

import csv
from dataclasses import dataclass, field
from pathlib import Path

from kaboom_robot.vision.state_machine import Thresholds

STATES = ("FOCUSED", "DISTRACTED", "AWAY")
CSV_FIELDS = ("t", "truth", "predicted", "ear", "gaze", "yaw", "pitch", "roll")


@dataclass(frozen=True)
class Sample:
    t: float  # seconds since the start of the recording
    truth: str | None  # what the user was really doing; None = not labelled
    predicted: str  # what the robot said (FOCUSED, DISTRACTED, AWAY, or CALIBRATING)


@dataclass
class Segment:
    """A stretch of time with the same truth."""

    truth: str
    start: float
    end: float
    detected_at: float | None = None  # first time the robot agreed

    @property
    def length(self) -> float:
        return self.end - self.start

    @property
    def delay(self) -> float | None:
        return None if self.detected_at is None else self.detected_at - self.start


@dataclass
class Report:
    frames_scored: int = 0
    frames_correct: int = 0
    frames_waiting: int = 0  # right after a change, inside the robot's allowed delay
    confusion: dict = field(default_factory=lambda: {s: dict.fromkeys(STATES, 0) for s in STATES})
    segments: list = field(default_factory=list)
    allowed_delay: dict = field(default_factory=dict)

    @property
    def accuracy(self) -> float | None:
        return None if self.frames_scored == 0 else self.frames_correct / self.frames_scored

    def state_accuracy(self, state: str) -> float | None:
        row = self.confusion[state]
        total = sum(row.values())
        return None if total == 0 else row[state] / total

    def judged_segments(self, state: str) -> list:
        """Segments long enough for the robot to be expected to notice them."""
        return [
            s for s in self.segments if s.truth == state and s.length >= self.allowed_delay[state]
        ]

    def detection_rate(self, state: str) -> float | None:
        judged = self.judged_segments(state)
        if not judged:
            return None
        return sum(1 for s in judged if s.detected_at is not None) / len(judged)

    def mean_delay(self, state: str) -> float | None:
        delays = [s.delay for s in self.judged_segments(state) if s.delay is not None]
        return None if not delays else sum(delays) / len(delays)


def allowed_delays(th: Thresholds | None = None, margin_s: float = 1.5) -> dict:
    """How long the robot may take to notice each state, from its own settings."""
    th = th or Thresholds()
    return {
        "FOCUSED": th.recover_after_s + th.grace_s + margin_s,
        "DISTRACTED": th.distracted_after_s + margin_s,
        "AWAY": th.away_after_s + margin_s,
    }


def segments_of(samples: list[Sample]) -> list[Segment]:
    """Split a recording into stretches with the same truth (unlabelled parts are skipped)."""
    segments: list[Segment] = []
    current = None
    for s in samples:
        if s.truth is None:
            current = None
            continue
        if current is None or current.truth != s.truth:
            current = Segment(s.truth, s.t, s.t)
            segments.append(current)
        current.end = s.t
        if current.detected_at is None and s.predicted == s.truth:
            current.detected_at = s.t
    return segments


def score(recordings: list[list[Sample]], delays: dict | None = None) -> Report:
    """Score one or more recordings (for example, one per user) together."""
    delays = delays or allowed_delays()
    report = Report(allowed_delay=dict(delays))
    for samples in recordings:
        report.segments.extend(segments_of(samples))
        truth, start = None, 0.0
        for s in samples:
            if s.truth != truth:  # the real state changed: a new stretch starts here
                truth, start = s.truth, s.t
            if s.truth not in STATES or s.predicted not in STATES:
                continue  # not labelled, or still calibrating
            if s.t - start < delays[s.truth]:
                report.frames_waiting += 1
                continue
            report.frames_scored += 1
            report.confusion[s.truth][s.predicted] += 1
            if s.predicted == s.truth:
                report.frames_correct += 1
    return report


def _pct(value: float | None) -> str:
    return "n/a" if value is None else f"{value * 100:.0f}%"


def _secs(value: float | None) -> str:
    return "n/a" if value is None else f"{value:.1f} s"


def to_markdown(report: Report, title: str = "Focus detection accuracy") -> str:
    """A short report to paste into docs/qa as testing evidence."""
    lines = [
        f"# {title}",
        "",
        f"**Overall accuracy: {_pct(report.accuracy)}** "
        f"({report.frames_correct} of {report.frames_scored} scored frames correct; "
        f"{report.frames_waiting} frames skipped while the robot was allowed to wait).",
        "",
        "## By state",
        "",
        "| Real state | Frames correct | Times it happened | Times detected | Average delay |",
        "|---|---|---|---|---|",
    ]
    for state in STATES:
        judged = report.judged_segments(state)
        detected = sum(1 for s in judged if s.detected_at is not None)
        rate = _pct(report.detection_rate(state))
        lines.append(
            f"| {state} | {_pct(report.state_accuracy(state))} | {len(judged)} | "
            f"{detected} ({rate}) | {_secs(report.mean_delay(state))} |"
        )
    lines += [
        "",
        "## What the robot said (rows: real state, columns: robot)",
        "",
        "| Real \\ Robot | " + " | ".join(STATES) + " |",
        "|---|" + "---|" * len(STATES),
    ]
    for state in STATES:
        row = report.confusion[state]
        lines.append(f"| {state} | " + " | ".join(str(row[p]) for p in STATES) + " |")
    lines += [
        "",
        "Allowed delay before comparing: "
        + ", ".join(f"{s} {report.allowed_delay[s]:.1f} s" for s in STATES)
        + ". Stretches shorter than that (a quick glance) are not counted as misses.",
        "",
    ]
    return "\n".join(lines)


def write_csv(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        writer.writeheader()
        writer.writerows(rows)


def read_csv(path: Path) -> list[Sample]:
    with Path(path).open(newline="") as f:
        return [
            Sample(float(row["t"]), row["truth"] or None, row["predicted"])
            for row in csv.DictReader(f)
        ]
