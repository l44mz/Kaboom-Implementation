"""Checks the ILI9486 wiring without a multimeter.

1. Resets the screen and asks for its ID (command 0xD3). An ILI9486 answers 94 86,
   an ST7796S answers 77 96. If it answers, the data bus, RD, RS, WR, CS and RST all work.
2. If not, it tries again with RS flipped (the module's table describes RS the other way).
3. Fills the screen red, green, blue, white, black, so you can check the colours.

Run from the folder that holds kaboom_robot (e.g. ~/sparky):
    python3 -m kaboom_robot.display.screen_test
"""

import time

from kaboom_robot.display.ili9486 import DATA_PINS, Ili9486


def try_id(rs_invert: bool, delay: int):
    screen = Ili9486(delay=delay, rs_invert=rs_invert, start=False)
    answer = screen.read_id()
    return screen, answer


def main() -> None:
    print("Data pins D0-D15 are GPIO", DATA_PINS)
    found = None
    for rs_invert in (False, True):
        screen, answer = try_id(rs_invert, delay=5)
        text = " ".join(f"{b:02X}" for b in answer)
        mode = "RS flipped" if rs_invert else "RS standard"
        print(f"{mode:<12} ID answer: {text}")
        if answer[2:] in ([0x94, 0x86], [0x77, 0x96]):
            chip = "ILI9486" if answer[2] == 0x94 else "ST7796S"
            print(f"Found an {chip}.")
            found = (screen, rs_invert)
            break
        screen.close()

    if found is None:
        print()
        print("The screen didn't answer 94 86 (ILI9486) or 77 96 (ST7796S).")
        print("- The same bytes for both tries (e.g. 1F 1F 1F 1F) = nothing answers:")
        print("  check RD, CS, RS, RST and WR first, then VDD and GND.")
        print("- Other numbers = some data wires (DB0-DB7) are swapped or loose.")
        return

    screen, rs_invert = found
    print()
    print("The screen answered: the wiring works!")
    if rs_invert:
        print("Your module needs RS flipped: add --rs-invert when you run face_demo.")
    print("Now filling red, green, blue, white, black (1 second each)...")
    print("If black looks white (and white looks black), add --invert when you run face_demo.")
    screen.start()
    for color in [(255, 0, 0), (0, 255, 0), (0, 0, 255), (255, 255, 255), (0, 0, 0)]:
        screen.fill(color)
        time.sleep(1)
    screen.close()
    print("Done. If the colours were right, run:")
    print("  python3 -m kaboom_robot.display.face_demo" + (" --rs-invert" if rs_invert else ""))


if __name__ == "__main__":
    main()
