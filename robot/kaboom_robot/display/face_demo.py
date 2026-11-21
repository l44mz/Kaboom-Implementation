"""Shows Sparky's animated face.

Run from the robot folder:
    python -m kaboom_robot.display.face_demo               # on the ILI9486 screen
    python -m kaboom_robot.display.face_demo --preview     # in a window (laptop, or the Pi over VNC)
    python -m kaboom_robot.display.face_demo --png frames  # save a few PNG frames to check the look

By default it changes expression every 5 seconds. --expression happy keeps one face.
In the preview window: keys 1-4 change the face, q quits.
"""

import argparse
import time
from pathlib import Path

from kaboom_robot.display.face import EXPRESSIONS, FaceAnimator


def main() -> None:
    parser = argparse.ArgumentParser(description="Sparky face animation")
    parser.add_argument("--expression", choices=EXPRESSIONS, help="keep one face")
    parser.add_argument("--every", type=float, default=5.0, help="seconds per face when cycling")
    parser.add_argument("--fps", type=float, default=20.0)
    parser.add_argument("--preview", action="store_true", help="OpenCV window instead of the screen")
    parser.add_argument("--png", type=Path, help="save 40 frames into this folder and stop")
    parser.add_argument("--rotation", default="landscape",
                        help="landscape, landscape_flipped, portrait or portrait_flipped")
    parser.add_argument("--swap-rb", action="store_true", help="use if red and blue look swapped")
    parser.add_argument("--delay", type=int, default=1,
                        help="slow the bus down (2, 3...) if the picture has wrong pixels")
    parser.add_argument("--rs-invert", action="store_true",
                        help="RS high = command (use if screen_test.py found that)")
    parser.add_argument("--invert", action="store_true",
                        help="use if black shows as white (some ST7796S panels)")
    parser.add_argument("--smooth", type=int, default=2,
                        help="2 = smooth edges, 1 = faster (more frames per second)")
    args = parser.parse_args()

    animator = FaceAnimator(args.expression or "focused", supersample=args.smooth)

    if args.png:
        save_frames(animator, args.png)
        return

    show, close = open_output(args)
    print("Showing Sparky's face. Ctrl+C to stop.")
    start = time.monotonic()
    frames, fps_since = 0, start
    try:
        while True:
            now = time.monotonic()
            if args.expression is None:
                index = int((now - start) // args.every) % len(EXPRESSIONS)
                animator.set_expression(EXPRESSIONS[index])

            key = show(animator.frame(now))
            if key == "q":
                break
            if key in ("1", "2", "3", "4"):
                args.expression = EXPRESSIONS[int(key) - 1]
                animator.set_expression(args.expression)

            frames += 1
            if now - fps_since >= 5:
                print(f"{animator.expression:<10} {frames / (now - fps_since):5.1f} fps")
                frames, fps_since = 0, now
            time.sleep(max(0.0, 1 / args.fps - (time.monotonic() - now)))
    except KeyboardInterrupt:
        pass
    finally:
        close()


def open_output(args):
    """Returns (show(image) -> pressed key or None, close())."""
    if args.preview:
        import cv2
        import numpy as np

        def show(image):
            cv2.imshow("Sparky (1-4 faces, q quits)", cv2.cvtColor(np.asarray(image), cv2.COLOR_RGB2BGR))
            key = cv2.waitKey(1) & 0xFF
            return chr(key) if key != 255 else None

        return show, cv2.destroyAllWindows

    from kaboom_robot.display.ili9486 import Ili9486

    screen = Ili9486(
        rotation=args.rotation,
        swap_red_blue=args.swap_rb,
        delay=args.delay,
        rs_invert=args.rs_invert,
        invert_colors=args.invert,
    )

    def show(image):
        screen.show(image)
        return None

    return show, screen.close


def save_frames(animator: FaceAnimator, folder: Path) -> None:
    folder.mkdir(parents=True, exist_ok=True)
    t = 0.0
    for i, expression in enumerate(EXPRESSIONS):
        animator.set_expression(expression)
        for j in range(10):
            t += 0.05
            animator.frame(t).save(folder / f"{i * 10 + j:02d}_{expression}.png")
    print(f"Saved 40 frames in {folder}/")


if __name__ == "__main__":
    main()
