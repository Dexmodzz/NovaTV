"""Generate NovaTV launcher icon + TV banner as PNG (no dependencies)."""
import math, struct, sys, zlib

ACCENT = (0xFF, 0x8A, 0x3D)
DARK = (0x0F, 0x0F, 0x10)
TEXT = (0xF1, 0xF1, 0xF1)
SS = 4  # supersampling factor


def png(path, w, h, rows):
    raw = b"".join(b"\x00" + bytes(r) for r in rows)
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    open(path, "wb").write(data)


def in_rrect(x, y, x0, y0, x1, y1, r):
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = min(max(x, x0 + r), x1 - r)
    cy = min(max(y, y0 + r), y1 - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def in_tri(x, y, a, b, c):
    def s(p, q, r):
        return (p[0] - r[0]) * (q[1] - r[1]) - (q[0] - r[0]) * (p[1] - r[1])
    d1, d2, d3 = s((x, y), a, b), s((x, y), b, c), s((x, y), c, a)
    neg = d1 < 0 or d2 < 0 or d3 < 0
    pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (neg and pos)


def seg_dist(x, y, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(x - ax - t * dx, y - ay - t * dy)


def icon_shapes(ox, oy, size):
    """Rounded accent square with a dark play triangle, inside box (ox, oy, size)."""
    r = size * 0.24
    # play triangle, shifted right slightly for optical centering
    cx, cy, t = ox + size * 0.53, oy + size * 0.5, size * 0.42
    tri = ((cx - t * 0.43, cy - t * 0.5), (cx - t * 0.43, cy + t * 0.5), (cx + t * 0.57, cy))
    def color(x, y):
        if not in_rrect(x, y, ox, oy, ox + size, oy + size, r):
            return None
        return DARK if in_tri(x, y, *tri) else ACCENT
    return color


def text_shapes(ox, oy, h):
    """Geometric 'NovaTV' built from strokes. Cap height h, returns (color fn, width)."""
    sw = h * 0.16  # stroke width
    xh = h * 0.72  # x-height
    segs, rings = [], []
    x = ox
    base = oy + h
    def seg(a, b, c, d):
        segs.append((a, b, c, d))
    # N
    w = h * 0.78
    seg(x, base, x, oy); seg(x, oy, x + w, base); seg(x + w, base, x + w, oy); x += w + h * 0.30
    # o
    rr = xh / 2 - sw / 2
    rings.append((x + rr + sw / 2, base - xh / 2, rr)); x += 2 * rr + sw + h * 0.22
    # v
    w = xh * 0.85
    seg(x, base - xh + sw / 2, x + w / 2, base - sw / 4); seg(x + w / 2, base - sw / 4, x + w, base - xh + sw / 2); x += w + h * 0.22
    # a (ring + stem)
    rings.append((x + rr + sw / 2, base - xh / 2, rr)); seg(x + 2 * rr + sw / 2, base - xh + sw / 2, x + 2 * rr + sw / 2, base); x += 2 * rr + sw + h * 0.30
    # T
    w = h * 0.78
    seg(x, oy, x + w, oy); seg(x + w / 2, oy, x + w / 2, base); x += w + h * 0.20
    # V
    w = h * 0.82
    seg(x, oy, x + w / 2, base - sw / 4); seg(x + w / 2, base - sw / 4, x + w, oy); x += w
    half = sw / 2
    def color(px, py):
        for s in segs:
            if seg_dist(px, py, *s) <= half:
                return TEXT
        for cx, cy, r in rings:
            if abs(math.hypot(px - cx, py - cy) - r) <= half:
                return TEXT
        return None
    return color, x - ox


def render(w, h, bg, layers):
    rows = []
    n = SS * SS
    for y in range(h):
        row = []
        for x in range(w):
            acc = [0, 0, 0, 0]
            for sy in range(SS):
                for sx in range(SS):
                    px, py = x + (sx + 0.5) / SS, y + (sy + 0.5) / SS
                    c = None
                    for layer in layers:
                        c = layer(px, py) or c
                    if c is None and bg is not None:
                        c = bg
                    if c is not None:
                        acc[0] += c[0]; acc[1] += c[1]; acc[2] += c[2]; acc[3] += 255
            a = acc[3] / 255
            if a == 0:
                row += [0, 0, 0, 0]
            else:
                row += [round(acc[0] / a), round(acc[1] / a), round(acc[2] / a), round(acc[3] / n)]
        rows.append(row)
    return rows


def main(outdir):
    # Launcher icon 320x320 (transparent outside the rounded square)
    s = 320
    m = s * 0.06
    png(f"{outdir}/app_icon.png", s, s, render(s, s, None, [icon_shapes(m, m, s - 2 * m)]))
    # TV banner 320x180: dark background, icon + wordmark
    bw, bh = 320, 180
    isz = 72
    th = 32
    tcolor, tw = text_shapes(0, 0, th)  # measure
    gap = 16
    total = isz + gap + tw
    ix = (bw - total) / 2
    tcolor, _ = text_shapes(ix + isz + gap, (bh - th) / 2, th)
    png(f"{outdir}/app_banner.png", bw, bh, render(bw, bh, DARK, [icon_shapes(ix, (bh - isz) / 2, isz), tcolor]))


if __name__ == "__main__":
    main(sys.argv[1])
