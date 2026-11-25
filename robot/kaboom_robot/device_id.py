"""Finds a serial number that is unique to this robot, for pairing.

On a Raspberry Pi it is the CPU serial from /proc/cpuinfo (burned into the chip, so it
stays the same after reinstalling the SD card). On a laptop it falls back to the hostname.
"""

import socket
from pathlib import Path


def device_serial() -> str:
    try:
        for line in Path("/proc/cpuinfo").read_text().splitlines():
            if line.lower().startswith("serial"):
                serial = line.split(":", 1)[1].strip().lstrip("0")
                if serial:
                    return f"pi-{serial}"
    except OSError:
        pass
    return f"host-{socket.gethostname()}"


if __name__ == "__main__":
    print(device_serial())