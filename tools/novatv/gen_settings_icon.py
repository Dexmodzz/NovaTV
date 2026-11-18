#!/usr/bin/env python3
"""NovaTV: settings grid icon for the "NovaTV" category (palette), white on transparent, 302x302.
Usage: python3 tools/novatv/gen_settings_icon.py  (writes common/src/main/res/drawable-nodpi/settings_novatv.png)"""
import math
import os
import struct
import zlib

SIZE = 302
SS = 4  # supersampling

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'common', 'src', 'main', 'res', 'drawable-nodpi', 'settings_novatv.png')


def inside(x, y):
    """Palette: big disc with a bite (thumb hole + notch) and four paint wells cut out."""
    cx, cy, r = 151, 151, 128
    if (x - cx) ** 2 + (y - cy) ** 2 > r * r:
        return False
    # notch on the lower right
    if (x - 238) ** 2 + (y - 222) ** 2 < 44 ** 2:
        return False
    # thumb hole
    if (x - 164) ** 2 + (y - 200) ** 2 < 20 ** 2:
        return False
    # paint wells
    for wx, wy in ((92, 120), (138, 78), (198, 92), (96, 186)):
        if (x - wx) ** 2 + (y - wy) ** 2 < 24 ** 2:
            return False
    return True


def main():
    rows = []
    for py in range(SIZE):
        row = bytearray([0])
        for px in range(SIZE):
            hits = 0
            for sy in range(SS):
                for sx in range(SS):
                    if inside(px + (sx + 0.5) / SS, py + (sy + 0.5) / SS):
                        hits += 1
            a = round(255 * hits / (SS * SS))
            row += bytes((255, 255, 255, a))
        rows.append(bytes(row))

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(b''.join(rows), 9))
    png += chunk(b'IEND', b'')

    with open(OUT, 'wb') as f:
        f.write(png)
    print('written', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
