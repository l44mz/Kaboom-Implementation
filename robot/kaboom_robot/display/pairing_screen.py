"""The pairing screen: shows the 6-digit code big, so the user can type it in the app.

    render_pairing(code, now) -> a Pillow image of the screen size (480 x 320).

Same colours as the face. Nothing here touches the hardware, so it runs on a laptop too:
    python -m kaboom_robot.display.pairing_screen 482913     # saves pairing_preview.png
"""

import math

from PIL import Image, ImageDraw, ImageFont

from kaboom_robot.display.face import BACKGROUND, EYE, MOUTH

TEXT = (229, 231, 255)       # almost white
MUTED = (156, 163, 212)      # #9CA3D4, AppColors.textMuted
BOX = (30, 32, 80)           # #1E2050, AppColors.field

# Raspberry Pi OS and most Linux systems have DejaVu; Pillow's own font is the fallback.
_FONT_FILES = (
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "DejaVuSans-Bold.ttf",
)
_fonts = {}


def _font(size: int, bold: bool = True):
    key = (size, bold)
    if key not in _fonts:
        names = _FONT_FILES if bold else _FONT_FILES[1:]
        for name in names:
            try:
                _fonts[key] = ImageFont.truetype(name, size)
                break
            except OSError:
                continue
        else:
            _fonts[key] = ImageFont.load_default(size=size)  # Pillow 10.1+
    return _fonts[key]


def _center_text(draw, cx, y, text, font, fill):
    left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
    draw.text((cx - (right - left) / 2 - left, y - top), text, font=font, fill=fill)


def render_pairing(code: str, now: float = 0.0, width: int = 480, height: int = 320):
    """The code in 6 boxes, a title, a hint, and 3 dots that pulse while it waits."""
    img = Image.new("RGB", (width, height), BACKGROUND)
    draw = ImageDraw.Draw(img)
    s = width / 480  # scale, if the screen size ever changes

    # Little Sparky eyes on top: ^ ^, like the happy face
    for cx in (width / 2 - 28 * s, width / 2 + 28 * s):
        draw.arc([cx - 14 * s, 26 * s, cx + 14 * s, 54 * s], 200, 340, fill=EYE, width=int(6 * s))

    _center_text(draw, width / 2, 66 * s, "Let's be friends!", _font(int(26 * s)), TEXT)
    _center_text(draw, width / 2, 102 * s, "In the Kaboom app open Connect Robot", _font(int(16 * s), bold=False), MUTED)
    _center_text(draw, width / 2, 124 * s, "and type this code:", _font(int(16 * s), bold=False), MUTED)

    # 6 boxes with one digit each, a small gap in the middle: 482 913
    digits = (code or "").strip()[:6].ljust(6)
    box_w, box_h, gap, mid_gap = 56 * s, 76 * s, 10 * s, 22 * s
    total = 6 * box_w + 4 * gap + mid_gap
    x = (width - total) / 2
    top = 158 * s
    digit_font = _font(int(52 * s))
    for i, digit in enumerate(digits):
        draw.rounded_rectangle([x, top, x + box_w, top + box_h], radius=int(10 * s),
                               fill=BOX, outline=EYE, width=int(3 * s))
        _center_text(draw, x + box_w / 2, top + 10 * s, digit, digit_font, TEXT)
        x += box_w + (mid_gap if i == 2 else gap)

    # "waiting" dots that pulse
    for i in range(3):
        level = (math.sin(now * 4 - i * 0.8) + 1) / 2
        r = (4 + 2 * level) * s
        cx = width / 2 + (i - 1) * 22 * s
        cy = 268 * s
        color = tuple(int(MUTED[c] + (MOUTH[c] - MUTED[c]) * level) for c in range(3))
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
    _center_text(draw, width / 2, 286 * s, "The code changes every 10 minutes", _font(int(13 * s), bold=False), MUTED)
    return img


if __name__ == "__main__":
    import sys

    code = sys.argv[1] if len(sys.argv) > 1 else "482913"
    render_pairing(code).save("pairing_preview.png")
    print("Saved pairing_preview.png")