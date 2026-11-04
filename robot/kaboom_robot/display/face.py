"""Sparky's animated face for the robot's screen (same faces as the app's RobotFace).

FaceAnimator.frame(now) gives a Pillow image of the screen size. It blinks on its own,
looks around a little, and blinks to switch when the expression changes.
Nothing here touches the hardware, so it also runs on a laptop (see face_demo.py --preview).
"""

import math
import random

from PIL import Image, ImageDraw

EXPRESSIONS = ("focused", "distracted", "away", "happy")

BACKGROUND = (15, 16, 48)  # #0F1030, the app's robot screen
EYE = (79, 107, 255)  # #4F6BFF, AppColors.primary
MOUTH = (56, 189, 248)  # #38BDF8

# The app draws the face on a 228 x 150 canvas; these numbers come from robot_face.dart.
FACE_W, FACE_H = 228, 150


class FaceRenderer:
    """Draws one face. eye_open: 0 (closed) .. 1 (open). gaze: (dx, dy) in face units."""

    def __init__(self, width: int = 480, height: int = 320, supersample: int = 2):
        self.width, self.height = width, height
        self.ss = supersample  # draw bigger, then shrink: smooth edges
        self.scale = min(width / FACE_W, height / FACE_H) * self.ss
        self.ox = (width * self.ss - FACE_W * self.scale) / 2
        self.oy = (height * self.ss - FACE_H * self.scale) / 2

    def render(self, expression, eye_open=1.0, gaze=(0.0, 0.0), bob=0.0):
        img = Image.new("RGB", (self.width * self.ss, self.height * self.ss), BACKGROUND)
        draw = ImageDraw.Draw(img)
        eye_open = max(0.06, min(1.0, eye_open))
        gx, gy = gaze
        eye_shift = (gx, gy + bob)
        mouth_shift = (gx * 0.5, gy * 0.5 + bob)

        if expression == "focused":
            self._bar(draw, 33, 58, 48, 14, EYE, eye_open, eye_shift)
            self._bar(draw, 144, 58, 48, 14, EYE, eye_open, eye_shift)
            self._bar(draw, 88, 92, 51, 10, MOUTH, 1.0, mouth_shift)
        elif expression == "distracted":
            self._chevron(draw, [(32, 47), (74, 65), (32, 83)], eye_open, eye_shift)
            self._chevron(draw, [(196, 47), (154, 65), (196, 83)], eye_open, eye_shift)
            self._half_arc(draw, (117, 104), 22, top=True, width=7, color=MOUTH,
                           squash=1.0, shift=mouth_shift)
        elif expression == "away":
            self._bar(draw, 36, 64, 48, 14, EYE, eye_open, eye_shift)
            self._bar(draw, 144, 64, 48, 14, EYE, eye_open, eye_shift)
            self._circle(draw, (114, 98), 16, MOUTH, mouth_shift)
            self._circle(draw, (114, 98), 5, BACKGROUND, mouth_shift)
        else:  # happy: ^ ^ and a smile
            for cx in (57, 171):
                self._half_arc(draw, (cx, 70), 20, top=True, width=9, color=EYE,
                               squash=eye_open, shift=eye_shift)
            self._half_arc(draw, (114, 80), 22, top=False, width=8, color=MOUTH,
                           squash=1.0, shift=mouth_shift)

        if self.ss > 1:
            img = img.resize((self.width, self.height), Image.Resampling.LANCZOS)
        return img

    # ---- shapes, in face units (228 x 150) ----

    def _p(self, x, y, shift=(0.0, 0.0)):
        return (self.ox + (x + shift[0]) * self.scale, self.oy + (y + shift[1]) * self.scale)

    def _bar(self, draw, x, y, w, h, color, open_, shift):
        cy = y + h / 2
        h2 = max(2.0, h * open_) / 2
        x0, y0 = self._p(x, cy - h2, shift)
        x1, y1 = self._p(x + w, cy + h2, shift)
        draw.rounded_rectangle((x0, y0, x1, y1), radius=(y1 - y0) / 2, fill=color)

    def _chevron(self, draw, points, open_, shift):
        cy = points[1][1]
        squashed = [(x, cy + (y - cy) * open_) for x, y in points]
        pts = [self._p(x, y, shift) for x, y in squashed]
        width = 10 * self.scale
        draw.line(pts, fill=EYE, width=round(width), joint="curve")
        for px, py in (pts[0], pts[-1]):  # round ends
            r = width / 2
            draw.ellipse((px - r, py - r, px + r, py + r), fill=EYE)

    def _half_arc(self, draw, center, radius, top, width, color, squash, shift):
        """Half circle (top = like "^", bottom = like a smile), squashed for blinking."""
        cx, cy = center
        start = math.pi if top else 0.0
        steps = 24
        pts = []
        for i in range(steps + 1):
            a = start + math.pi * i / steps
            pts.append(self._p(cx + radius * math.cos(a), cy + radius * squash * math.sin(a), shift))
        w = width * self.scale
        draw.line(pts, fill=color, width=round(w), joint="curve")
        r = w / 2
        for px, py in (pts[0], pts[-1]):  # round ends
            draw.ellipse((px - r, py - r, px + r, py + r), fill=color)

    def _circle(self, draw, center, radius, color, shift):
        x0, y0 = self._p(center[0] - radius, center[1] - radius, shift)
        x1, y1 = self._p(center[0] + radius, center[1] + radius, shift)
        draw.ellipse((x0, y0, x1, y1), fill=color)


class FaceAnimator:
    """Keeps the face alive: blinking, looking around, and blink-to-switch expressions."""

    BLINK_S = 0.18  # one blink
    SWITCH_S = 0.30  # close, change face, open

    def __init__(self, expression="focused", width=480, height=320, supersample=2, seed=None):
        self.renderer = FaceRenderer(width, height, supersample)
        self.expression = expression
        self._next = None  # expression we are switching to
        self._switch_start = None
        self._rng = random.Random(seed)
        self._blink_start = None
        self._next_blink = None
        self._gaze = [0.0, 0.0]
        self._gaze_target = [0.0, 0.0]
        self._next_glance = None
        self._last_t = None

    def set_expression(self, expression: str) -> None:
        if expression not in EXPRESSIONS:
            raise ValueError(f"Unknown expression {expression!r}; use one of {EXPRESSIONS}")
        if expression != self.expression and expression != self._next:
            self._next = expression
            self._switch_start = None  # starts on the next frame

    def frame(self, now: float) -> Image.Image:
        dt = 0.0 if self._last_t is None else min(0.2, now - self._last_t)
        self._last_t = now
        if self._next_blink is None:
            self._next_blink = now + self._rng.uniform(1.5, 4.0)
            self._next_glance = now + self._rng.uniform(1.0, 3.0)

        eye_open = self._eyes(now)
        self._update_gaze(now, dt)
        bob = self._bob(now)
        return self.renderer.render(self.expression, eye_open, tuple(self._gaze), bob)

    # ---- blinking and switching ----

    def _eyes(self, now):
        if self._next is not None:
            if self._switch_start is None:
                self._switch_start = now
            phase = (now - self._switch_start) / self.SWITCH_S
            if phase >= 0.5 and self.expression != self._next:
                self.expression = self._next  # swap while the eyes are closed
            if phase >= 1:
                self._next = None
                self._switch_start = None
                return 1.0
            return 1 - math.sin(math.pi * phase)

        if self._blink_start is None and now >= self._next_blink:
            self._blink_start = now
        if self._blink_start is not None:
            phase = (now - self._blink_start) / self.BLINK_S
            if phase >= 1:
                self._blink_start = None
                # Sometimes a quick double blink, like people do.
                double = self._rng.random() < 0.2
                self._next_blink = now + (0.25 if double else self._rng.uniform(2.5, 6.0))
                return 1.0
            return 1 - math.sin(math.pi * phase)

        # Away: sleepy, half-open eyes.
        return 0.55 if self.expression == "away" else 1.0

    # ---- looking around ----

    def _update_gaze(self, now, dt):
        if self.expression == "away":
            # Searching for the user: slow look left and right.
            self._gaze_target = [10 * math.sin(now * 0.9), 0.0]
        elif now >= self._next_glance:
            if self.expression == "distracted":
                self._gaze_target = [self._rng.choice((-9, 9)), self._rng.uniform(-3, 3)]
                self._next_glance = now + self._rng.uniform(0.8, 1.6)
            else:
                # Mostly look at the user, sometimes glance away a little.
                if self._rng.random() < 0.6:
                    self._gaze_target = [0.0, 0.0]
                else:
                    self._gaze_target = [self._rng.uniform(-6, 6), self._rng.uniform(-3, 2)]
                self._next_glance = now + self._rng.uniform(1.5, 4.0)

        follow = min(1.0, dt * 8)  # smooth movement
        for i in (0, 1):
            self._gaze[i] += (self._gaze_target[i] - self._gaze[i]) * follow

    def _bob(self, now):
        if self.expression == "happy":
            return -abs(math.sin(now * 4)) * 4  # little bounces
        if self.expression == "away":
            return math.sin(now * 1.2) * 2
        if self.expression == "distracted":
            return math.sin(now * 20) * 0.6  # small shake
        return math.sin(now * 1.5) * 1.2  # calm breathing
