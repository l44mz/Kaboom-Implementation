"""ILI9486 / ST7796S screen (TZT MRB3503, 480 x 320) on a 16-bit parallel bus, for the Raspberry Pi 4.

The MRB3503 datasheet says ILI9486, but some boards ship with an ST7796S (same pins, same
resolution, different start-up commands). The controller is detected by reading its ID:
ILI9486 answers 94 86, ST7796S answers 77 96.

The fast part (toggling the pins) is in ili9486_par.c; this file loads it with ctypes,
sends the panel's start-up commands, and turns Pillow images into pixels.

Build the C part once on the Pi (from the robot folder):
    gcc -O2 -shared -fPIC -o kaboom_robot/display/libili9486.so kaboom_robot/display/ili9486_par.c
"""

import ctypes
import time
from pathlib import Path

import numpy as np

# ---- Wiring: BCM GPIO numbers (not header pin numbers). Change these to match your wires. ----
DATA_PINS = [4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 16, 17, 18, 19, 20, 21]  # D0 .. D15
RS_PIN = 22  # also called DC or D/C (command / data)
WR_PIN = 23
CS_PIN = 24
RST_PIN = 25
RD_PIN = 26  # set to -1 if RD is wired to 3.3V

WIDTH, HEIGHT = 480, 320  # landscape

# MADCTL (0x36) values. If the picture is mirrored or upside down, try the others.
ROTATIONS = {
    "landscape": 0x28,
    "landscape_flipped": 0xE8,
    "portrait": 0x48,
    "portrait_flipped": 0x88,
}
BGR_BIT = 0x08  # flips red and blue

LIB_PATH = Path(__file__).with_name("libili9486.so")


def rgb565(image_rgb: np.ndarray) -> np.ndarray:
    """(h, w, 3) uint8 RGB -> (h, w) uint16 RGB565."""
    rgb = image_rgb.astype(np.uint16)
    return ((rgb[..., 0] >> 3) << 11) | ((rgb[..., 1] >> 2) << 5) | (rgb[..., 2] >> 3)


class Ili9486:
    """The screen. Use show(image) for a full frame, or let it send only what changed."""

    def __init__(
        self,
        rotation: str = "landscape",
        swap_red_blue: bool = False,
        delay: int = 1,
        rs_invert: bool = False,
        start: bool = True,
        controller: str = "auto",
        invert_colors: bool = False,
    ):
        """rs_invert: True if the module wants RS high for commands (its pin table says so).
        start: False only resets the panel (used by screen_test.py to read the ID first).
        controller: "auto" (read the ID), "ili9486" or "st7796".
        invert_colors: True if black shows as white (some ST7796S panels need it)."""
        if not LIB_PATH.is_file():
            raise FileNotFoundError(
                f"{LIB_PATH.name} not found. Build it first (from the robot folder):\n"
                "  gcc -O2 -shared -fPIC -o kaboom_robot/display/libili9486.so "
                "kaboom_robot/display/ili9486_par.c"
            )
        self._lib = ctypes.CDLL(str(LIB_PATH))
        self._lib.ili_open.argtypes = [ctypes.POINTER(ctypes.c_int)] + [ctypes.c_int] * 6
        self._lib.ili_command.argtypes = [ctypes.c_uint8]
        self._lib.ili_data.argtypes = [ctypes.POINTER(ctypes.c_uint8), ctypes.c_int]
        self._lib.ili_pixels.argtypes = [ctypes.POINTER(ctypes.c_uint16), ctypes.c_int]
        self._lib.ili_fill.argtypes = [ctypes.c_uint16, ctypes.c_int]
        self._lib.ili_read.argtypes = [ctypes.c_uint8, ctypes.POINTER(ctypes.c_uint8), ctypes.c_int]
        self._lib.ili_set_rs_invert.argtypes = [ctypes.c_int]

        pins = (ctypes.c_int * 16)(*DATA_PINS)
        result = self._lib.ili_open(pins, RS_PIN, WR_PIN, CS_PIN, RST_PIN, RD_PIN, delay)
        if result == -1:
            raise PermissionError("Can't open /dev/gpiomem. Is your user in the 'gpio' group?")
        if result != 0:
            raise RuntimeError(f"ili_open failed ({result}). Check the pin numbers.")

        self._lib.ili_set_rs_invert(1 if rs_invert else 0)
        self.controller = controller
        self._invert = invert_colors
        self._madctl = ROTATIONS[rotation] ^ (BGR_BIT if swap_red_blue else 0)
        self.width, self.height = (WIDTH, HEIGHT) if "landscape" in rotation else (HEIGHT, WIDTH)
        self._last = None  # last frame sent, to send only the changed part next time
        if start:
            self.start()
        else:
            self._lib.ili_reset()

    # ---- low level ----

    def _cmd(self, command: int, *params: int) -> None:
        self._lib.ili_command(command)
        if params:
            data = (ctypes.c_uint8 * len(params))(*params)
            self._lib.ili_data(data, len(params))

    def start(self) -> None:
        """Resets the panel and sends the start-up commands for its controller."""
        self._lib.ili_reset()
        if self.controller == "auto":
            chip_id = self.read_id()
            self.controller = "st7796" if chip_id[2:] == [0x77, 0x96] else "ili9486"
        if self.controller == "st7796":
            self._init_st7796()
        else:
            self._init_ili9486()
        self._cmd(0x21 if self._invert else 0x20)  # colour inversion on / off
        self._cmd(0x36, self._madctl)  # rotation and colour order
        self._cmd(0x29)  # display on
        time.sleep(0.05)
        self.fill((0, 0, 0))

    def _init_ili9486(self) -> None:
        """ILI9486 power-on sequence (values used by common ILI9486 drivers)."""
        self._cmd(0x01)  # software reset
        time.sleep(0.12)
        self._cmd(0x11)  # sleep out
        time.sleep(0.12)
        self._cmd(0x3A, 0x55)  # 16 bits per pixel (RGB565)
        self._cmd(0xC2, 0x44)  # power control 3
        self._cmd(0xC5, 0x00, 0x00, 0x00, 0x00)  # VCOM
        self._cmd(0xE0, 0x0F, 0x1F, 0x1C, 0x0C, 0x0F, 0x08, 0x48, 0x98,
                  0x37, 0x0A, 0x13, 0x04, 0x11, 0x0D, 0x00)  # positive gamma
        self._cmd(0xE1, 0x0F, 0x32, 0x2E, 0x0B, 0x0D, 0x05, 0x47, 0x75,
                  0x37, 0x06, 0x10, 0x03, 0x24, 0x20, 0x00)  # negative gamma

    def _init_st7796(self) -> None:
        """ST7796S power-on sequence (values used by common ST7796 drivers)."""
        self._cmd(0x01)  # software reset
        time.sleep(0.12)
        self._cmd(0x11)  # sleep out
        time.sleep(0.12)
        self._cmd(0xF0, 0xC3)  # unlock command set 2, part 1
        self._cmd(0xF0, 0x96)  # unlock command set 2, part 2
        self._cmd(0x3A, 0x55)  # 16 bits per pixel (RGB565)
        self._cmd(0xB4, 0x01)  # 1-dot inversion
        self._cmd(0xB6, 0x80, 0x02, 0x3B)  # display function control
        self._cmd(0xE8, 0x40, 0x8A, 0x00, 0x00, 0x29, 0x19, 0xA5, 0x33)  # output timing
        self._cmd(0xC1, 0x06)  # power control 2
        self._cmd(0xC2, 0xA7)  # power control 3
        self._cmd(0xC5, 0x18)  # VCOM
        time.sleep(0.12)
        self._cmd(0xE0, 0xF0, 0x09, 0x0B, 0x06, 0x04, 0x15, 0x2F,
                  0x54, 0x42, 0x3C, 0x17, 0x14, 0x18, 0x1B)  # positive gamma
        self._cmd(0xE1, 0xE0, 0x09, 0x0B, 0x06, 0x04, 0x03, 0x2B,
                  0x43, 0x42, 0x3B, 0x16, 0x14, 0x17, 0x1B)  # negative gamma
        time.sleep(0.12)
        self._cmd(0xF0, 0x3C)  # lock command set 2
        self._cmd(0xF0, 0x69)
        time.sleep(0.12)

    def _window(self, x0: int, y0: int, x1: int, y1: int) -> None:
        """Pixels written next go into this rectangle (inclusive corners)."""
        self._cmd(0x2A, x0 >> 8, x0 & 0xFF, x1 >> 8, x1 & 0xFF)
        self._cmd(0x2B, y0 >> 8, y0 & 0xFF, y1 >> 8, y1 & 0xFF)
        self._lib.ili_command(0x2C)

    def _send(self, pixels: np.ndarray, x: int, y: int) -> None:
        h, w = pixels.shape
        block = np.ascontiguousarray(pixels, dtype=np.uint16)
        self._window(x, y, x + w - 1, y + h - 1)
        self._lib.ili_pixels(block.ctypes.data_as(ctypes.POINTER(ctypes.c_uint16)), w * h)

    def read(self, command: int, count: int) -> list[int]:
        """Sends a command and reads count bytes back (RD must be wired to RD_PIN)."""
        buffer = (ctypes.c_uint8 * count)()
        if self._lib.ili_read(command, buffer, count) != 0:
            raise RuntimeError("RD isn't connected to a GPIO (RD_PIN = -1), so nothing can be read.")
        return list(buffer)

    def read_id(self) -> list[int]:
        """Command 0xD3 (read ID4): [dummy, 0x00, 0x94, 0x86] = ILI9486, [.., 0x77, 0x96] = ST7796S."""
        return self.read(0xD3, 4)

    # ---- what the rest of the robot uses ----

    def fill(self, color: tuple[int, int, int]) -> None:
        value = int(rgb565(np.array([[color]], dtype=np.uint8))[0, 0])
        self._window(0, 0, self.width - 1, self.height - 1)
        self._lib.ili_fill(value, self.width * self.height)
        self._last = None

    def show(self, image) -> None:
        """Show a Pillow image of the screen's size. Only the changed rectangle is sent."""
        frame = rgb565(np.asarray(image.convert("RGB")))
        if self._last is None or self._last.shape != frame.shape:
            self._send(frame, 0, 0)
        else:
            changed = frame != self._last
            rows = np.flatnonzero(changed.any(axis=1))
            if rows.size == 0:
                return  # nothing changed
            cols = np.flatnonzero(changed.any(axis=0))
            y0, y1, x0, x1 = rows[0], rows[-1], cols[0], cols[-1]
            self._send(frame[y0:y1 + 1, x0:x1 + 1], int(x0), int(y0))
        self._last = frame

    def close(self) -> None:
        self._lib.ili_close()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
