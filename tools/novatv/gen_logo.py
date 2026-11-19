#!/usr/bin/env python3
"""NovaTV: header logo (novatv_logo.png). The same file is the app banner/logo (AndroidManifest android:banner/logo).
The wordmark is set in Inter SemiBold (the app font, SIL OFL), so every letter is readable at header size.
Needs pycairo.  Usage: python3 tools/novatv/gen_logo.py
(icon artwork = gen_icons.py: accent rounded square with a dark play triangle)"""
import os
import shutil
import subprocess
import tempfile

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
FONT = os.path.join(ROOT, 'smarttubetv', 'src', 'main', 'assets', 'fonts', 'inter_semibold.ttf')
RES = os.path.join(ROOT, 'smarttubetv', 'src')
LOGO_OUT = [os.path.join(RES, 'main', 'res', 'mipmap-nodpi', 'novatv_logo.png')]
ACCENT = (0xFF / 255, 0x8A / 255, 0x3D / 255)
DARK = (0x0F / 255, 0x0F / 255, 0x10 / 255)
TEXT = (0xF1 / 255, 0xF1 / 255, 0xF1 / 255)
WORD = 'NovaTV'


def setup_font():
    """Make Inter visible to fontconfig without installing it system wide."""
    tmp = tempfile.mkdtemp(prefix='novatv_fonts_')
    shutil.copy(FONT, tmp)
    conf = os.path.join(tmp, 'fonts.conf')
    with open(conf, 'w') as f:
        f.write('<?xml version="1.0"?><!DOCTYPE fontconfig SYSTEM "fonts.dtd"><fontconfig>'
                '<include ignore_missing="yes">/etc/fonts/fonts.conf</include>'
                f'<dir>{tmp}</dir><cachedir>{tmp}/cache</cachedir></fontconfig>')
    os.environ['FONTCONFIG_FILE'] = conf
    family = subprocess.run(['fc-scan', '--format', '%{family[0]}', FONT], capture_output=True, text=True).stdout.strip()
    return family or 'Inter'


def icon(ctx, x, y, size):
    import math
    r = size * 0.24
    ctx.new_sub_path()
    ctx.arc(x + size - r, y + r, r, -math.pi / 2, 0)
    ctx.arc(x + size - r, y + size - r, r, 0, math.pi / 2)
    ctx.arc(x + r, y + size - r, r, math.pi / 2, math.pi)
    ctx.arc(x + r, y + r, r, math.pi, 3 * math.pi / 2)
    ctx.close_path()
    ctx.set_source_rgb(*ACCENT)
    ctx.fill()
    cx, cy, t = x + size * 0.53, y + size * 0.5, size * 0.42
    ctx.move_to(cx - t * 0.43, cy - t * 0.5)
    ctx.line_to(cx - t * 0.43, cy + t * 0.5)
    ctx.line_to(cx + t * 0.57, cy)
    ctx.close_path()
    ctx.set_source_rgb(*DARK)
    ctx.fill()


def draw(cairo, family, isz, font_px, gap, pad, bg=None, size=None):
    """Icon + wordmark. Returns the surface (tightly cropped unless size is given)."""
    probe = cairo.Context(cairo.ImageSurface(cairo.FORMAT_ARGB32, 1, 1))
    probe.select_font_face(family, cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
    probe.set_font_size(font_px)
    ext = probe.text_extents(WORD)
    content_w = isz + gap + ext.x_advance - ext.x_bearing
    content_h = isz
    w, h = size if size else (int(content_w + 2 * pad + 0.5), int(content_h + 2 * pad + 0.5))
    surf = cairo.ImageSurface(cairo.FORMAT_ARGB32, w, h)
    ctx = cairo.Context(surf)
    fo = cairo.FontOptions()
    fo.set_antialias(cairo.ANTIALIAS_GRAY)
    fo.set_hint_style(cairo.HINT_STYLE_NONE)
    ctx.set_font_options(fo)
    if bg:
        ctx.set_source_rgb(*bg)
        ctx.paint()
    ox, oy = (w - content_w) / 2, (h - content_h) / 2
    icon(ctx, ox, oy, isz)
    ctx.select_font_face(family, cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_NORMAL)
    ctx.set_font_size(font_px)
    # cap height centred on the icon
    ctx.move_to(ox + isz + gap - ext.x_bearing, oy + isz / 2 - ext.y_bearing / 2 - (ext.height + ext.y_bearing) / 2)
    ctx.set_source_rgb(*TEXT)
    ctx.show_text(WORD)
    return surf


def main():
    family = setup_font()
    import cairo
    # Header logo: drawn large (300 px tall), the ImageView scales it down smoothly
    logo = draw(cairo, family, isz=288, font_px=190, gap=56, pad=6)
    for path in LOGO_OUT:
        logo.write_to_png(path)
    print('family:', family, '| written', len(LOGO_OUT), 'files')


if __name__ == '__main__':
    main()
