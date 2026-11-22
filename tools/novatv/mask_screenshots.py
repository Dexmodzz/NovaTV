"""NovaTV: README screenshots (docs/screenshots). Needs Pillow.
Usage: python3 tools/novatv/mask_screenshots.py <captures dir with it/ and en/> <output dir>
Captures: adb exec-out screencap -p on a 3840x2160 TV, one PNG per screen (home, rail, search,
channel, player, shorts, settings). The player is captured on a black video and paused.

Turns TV screenshots into 'skeleton' screenshots: video covers, avatars, banners, titles,
channel names and search queries become neutral grey shapes, the app interface stays.
Coordinates are given on a 2000x1125 grid (scaled to the real 3840x2160 capture)."""
import sys, os
from collections import Counter
from PIL import Image, ImageDraw

THUMB = (43, 43, 48)
AVATAR = (60, 60, 66)
BAR_BRIGHT = (78, 78, 86)
BAR_DIM = (56, 56, 62)


def sc(img, r):
    k = img.width / 2000
    return tuple(int(v * k) for v in r)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def rect(d, img, r, color=THUMB, radius=14, top_only=False, grow=0):
    x0, y0, x1, y1 = sc(img, r)
    x0, y0, x1 = x0 - grow, y0 - grow, x1 + grow
    rad = int(radius * img.width / 2000)
    d.rounded_rectangle((x0, y0, x1, y1), rad, fill=color)
    if top_only:  # square bottom corners (cover above the card text)
        d.rectangle((x0, y1 - rad, x1, y1), fill=color)


def circle(d, img, r, color=AVATAR, bg=None, pad=0, grow=4):
    x0, y0, x1, y1 = sc(img, r)
    x0, y0, x1, y1 = x0 - grow, y0 - grow, x1 + grow, y1 + grow
    if bg is not None:
        d.ellipse((x0 - pad, y0 - pad, x1 + pad, y1 + pad), fill=bg)
    d.ellipse((x0, y0, x1, y1), fill=color)


def text(d, img, px, r, gap=30, thr=38):
    """Replace each text run inside r with a rounded bar (square blobs become circles)."""
    x0, y0, x1, y1 = sc(img, r)
    x1, y1 = min(x1, img.width), min(y1, img.height)
    gap_px = int(gap * img.width / 2000)
    region = [px[x, y] for y in range(y0, y1, 3) for x in range(x0, x1, 3)]
    bg = Counter(region).most_common(1)[0][0]
    bl = lum(bg)

    def is_text(x, y):
        return abs(lum(px[x, y]) - bl) > thr

    rows = [y for y in range(y0, y1) if sum(1 for x in range(x0, x1, 2) if is_text(x, y)) >= 2]
    lines, cur = [], []
    for y in rows:
        if cur and y - cur[-1] > 3:
            lines.append(cur); cur = []
        cur.append(y)
    if cur:
        lines.append(cur)
    for ln in lines:
        ly0, ly1 = ln[0], ln[-1]
        if ly1 - ly0 < 4:
            continue
        cols = [x for x in range(x0, x1) if any(is_text(x, y) for y in range(ly0, ly1 + 1, 2))]
        groups, g = [], []
        for x in cols:
            if g and x - g[-1] > gap_px:
                groups.append(g); g = []
            g.append(x)
        if g:
            groups.append(g)
        h = ly1 - ly0
        for g in groups:
            gx0, gx1 = g[0], g[-1]
            bright = max(lum(px[x, y]) for x in range(gx0, gx1 + 1, 3) for y in range(ly0, ly1 + 1, 3))
            d.rectangle((gx0 - 4, ly0 - 4, gx1 + 4, ly1 + 4), fill=bg)
            w = gx1 - gx0
            if 0.75 < w / max(h, 1) < 1.3 and h > 40:
                d.ellipse((gx0, ly0, gx1, ly1), fill=AVATAR)
                continue
            bh = max(10, int(h * 0.55))
            cy = (ly0 + ly1) // 2
            d.rounded_rectangle((gx0, cy - bh // 2, gx1, cy + bh // 2), bh // 2,
                                fill=BAR_BRIGHT if bright > 200 else BAR_DIM)


def card(d, img, px, thumb, avatar, body, badge=True):
    if body:
        text(d, img, px, body)
    if avatar:
        circle(d, img, avatar)
    if thumb:
        rect(d, img, thumb, top_only=True, grow=5)
        if badge:
            x0, y0, x1, y1 = thumb
            if x1 - x0 > 300:
                rect(d, img, (x1 - 78, y1 - 46, x1 - 10, y1 - 12), color=(24, 24, 27), radius=5)


ACCOUNT = (70, 168, 146, 244)

SHOTS = {
    'home': lambda d, i, p, lang: [
        card(d, i, p, (104, 303, 985, 786), (119, 810, 191, 882), (205, 800, 982, 1000)),
        card(d, i, p, (1005, 303, 1860, 775), (1024, 795, 1096, 867), (1108, 795, 1856, 1000)),
        card(d, i, p, (1893, 303, 2000, 775), (1912, 795, 1984, 867), None, badge=False),
        text(d, i, p, (110, 1078, 1400, 1125)),
    ],
    'rail': lambda d, i, p, lang: [
        circle(d, i, ACCOUNT, bg=(24, 24, 27), pad=2),
        card(d, i, p, (400, 303, 1256, 775), (420, 795, 492, 867), (505, 795, 1252, 1000)),
        card(d, i, p, (1288, 303, 2000, 775), (1308, 795, 1380, 867), (1393, 795, 2000, 1000)),
        text(d, i, p, (395, 1078, 1700, 1125)),
    ],
    'shorts': lambda d, i, p, lang: [
        circle(d, i, ACCOUNT, bg=(24, 24, 27), pad=2),
        [card(d, i, p, (x, 155, x + 415, 845), None, (x + 15, 860, min(x + 415, 2000), 1040), badge=False)
         for x in (400, 846, 1292, 1738)],
        [rect(d, i, (x, 1088, min(x + 415, 2000), 1125), top_only=True, grow=5) for x in (400, 846, 1292, 1738)],
    ],
    'search': lambda d, i, p, lang: [
        text(d, i, p, (495, 55, 1600, 115)),
        text(d, i, p, (120, 195, 2000, 255)),
        text(d, i, p, {'it': (555, 322, 1000, 378), 'en': (475, 322, 1000, 378)}[lang]),
        circle(d, i, (309, 412, 781, 880), grow=8),
        text(d, i, p, (300, 895, 800, 1060)),
        card(d, i, p, (1005, 410, 1860, 877), (1024, 903, 1096, 975), (1108, 900, 1856, 1075)),
        card(d, i, p, (1893, 410, 2000, 877), (1912, 903, 1984, 975), None, badge=False),
    ],
    'channel': lambda d, i, p, lang: [
        text(d, i, p, (150, 55, 1600, 115)),
        rect(d, i, (36, 208, 1884, 520)),
        circle(d, i, (100, 417, 306, 624)),
        text(d, i, p, (340, 540, 1500, 670)),
        [rect(d, i, (x0, 860, x1, 1125), top_only=True, grow=5) for x0, x1 in ((117, 973), (1005, 1860), (1893, 2000))],
    ],
    'player': lambda d, i, p, lang: [
        text(d, i, p, (300, 50, 1650, 178)),
        circle(d, i, {'it': (792, 183, 852, 243), 'en': (752, 183, 812, 243)}[lang], grow=6),
        text(d, i, p, {'it': (862, 183, 1400, 243), 'en': (822, 183, 1400, 243)}[lang]),
        circle(d, i, (664, 944, 726, 1006)),
        text(d, i, p, {'it': (555, 1072, 1100, 1125), 'en': (475, 1072, 1100, 1125)}[lang]),
    ],
    'settings': lambda d, i, p, lang: [],
}


def main(src_root, out_root):
    for lang in ('it', 'en'):
        for name, fn in SHOTS.items():
            src = os.path.join(src_root, lang, name + '.png')
            img = Image.open(src).convert('RGB')
            px = img.load()
            d = ImageDraw.Draw(img)
            fn(d, img, px, lang)
            img = img.resize((1920, 1080), Image.LANCZOS)
            os.makedirs(out_root, exist_ok=True)
            img.save(os.path.join(out_root, '%s_%s.png' % (name, lang)), optimize=True)
            print(name, lang)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
