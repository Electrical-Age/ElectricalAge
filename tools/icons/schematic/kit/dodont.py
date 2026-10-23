"""Render the STYLE.md do/don't pairs: python3 -m kit.dodont OUT.png

Each pair: DO (left, passes Glyph.check) and DON'T (right, what the rule forbids), 6x zoom plus
1:1 on the slot grey and dark row. The DON'T glyphs bypass check() on purpose."""
import sys
from PIL import Image, ImageDraw

from . import *                     # noqa: F401,F403
from .symbols import *              # noqa: F401,F403
from .catalog import label_font
from .palette import _rgba


def _do_lamp():
    g = Glyph(tier='mv'); lamp(g, g.hue); return g


def _dont_pip():
    g = Glyph(tier='mv'); lamp(g); g.bitmap(4, 4, DOT, g.hue); return g


def _do_gate():
    g = Glyph(tier='signal'); gate(g, 'and', g.hue); return g


def _dont_box():
    g = Glyph(tier='signal'); g.frame(8, 6, 23, 25, g.hue); g.text_center(12, '&', INK)
    g.hline(4, 7, IN_A); g.hline(4, 7, IN_B); g.hline(24, 27, MID); return g


def _do_stroke():
    g = Glyph(); resistor(g); return g


def _dont_1px():
    g = Glyph()
    for x in range(8, 24):
        g.put(x, 11); g.put(x, 20)
    for y in range(11, 21):
        g.put(8, y); g.put(23, y)
    for x in list(range(4, 8)) + list(range(24, 28)):
        g.put(x, 15)
    return g


def _do_machine():
    g = Glyph(tier='lv'); macerator_rollers(g); return g


def _dont_depth():
    g = Glyph(tier='lv')
    top = TIER_RAMPS['lv'][2]                       # the light and dark shades are banned in icons
    g.rect(6, 6, 25, 10, top)                       # lit top face, shaded front: a 3D block
    g.rect(6, 11, 25, 26, g.hue)
    g.rect(10, 14, 21, 22, TIER_RAMPS['lv'][0])
    return g


def _do_untiered():
    g = Glyph(); capacitor(g, polar=True); return g


def _dont_domain():
    g = Glyph(); capacitor(g, c=_rgba('#f2d03c'), polar=True); return g


def _do_thermal():
    g = Glyph(tier='thermal'); thermal_cable(g, g.hue); return g


def _dont_thermal_red():
    g = Glyph(); cable_route(g, _rgba('#e05050')); heat_waves(g, 10, 18); return g


def _do_switch():
    g = Glyph(tier='hv'); switch(g, g.hue); return g


def _dont_blade_only():
    g = Glyph(tier='hv'); switch(g, g.hue, lead=INK); return g


def _do_gap():
    g = Glyph(tier='lv'); mark(g, 'C'); lamp(g, g.hue, ring=RING_M); return g


def _dont_merge():
    g = Glyph(tier='lv'); mark(g, 'C'); lamp(g, g.hue, ring=(10, 8, 16)); return g


def _do_variant():
    g = Glyph(tier='lv'); heating_corp(g, 'copper'); return g


def _dont_variant():
    g = Glyph(tier='lv'); resistor(g, g.hue); g.micro(12, 14, 'CU'); return g


PAIRS = [
    ('tier hue on the defining element', _do_lamp, 'tier as a small pip', _dont_pip),
    ('whole contact (leads + blade) in the tier hue', _do_switch, 'hue on the thin blade only (unreadable)', _dont_blade_only),
    ('IEEE gate silhouette', _do_gate, "IEC '&' box = in-world chip", _dont_box),
    ('one 2px stroke weight', _do_stroke, '1px lines', _dont_1px),
    ('flat functional symbol, no face', _do_machine, 'device silhouette, depth, shading', _dont_depth),
    ('untiered = all INK', _do_untiered, "domain colour ('power' yellow ~ HV)", _dont_domain),
    ('thermal grey + INK heat waves above the route', _do_thermal, 'thermal in red (~ VHV); cue under the stack count', _dont_thermal_red),
    ('mark in MARK_BOX, symbol at RING_M (3 px gap)', _do_gap, 'mark crowding the symbol (halos merge)', _dont_merge),
    ('variant = kit mark (FONT) in the mark slot', _do_variant, 'private MICRO label inside the body', _dont_variant),
]

def main(out):
    Z = 5
    cell = 32 * Z
    fnt = label_font(12)
    pw = 2 * (cell + 10) + 40
    rh = cell + 40 + 52
    per = 3
    rows = (len(PAIRS) + per - 1) // per
    im = Image.new('RGBA', (per * pw + 10, rows * rh + 10), '#555')
    dr = ImageDraw.Draw(im)
    for i, (dl, dfn, nl, nfn) in enumerate(PAIRS):
        bx = 10 + (i % per) * pw
        by = 10 + (i // per) * rh
        for j, (lab, fn, col) in enumerate(((dl, dfn, '#7fe08a'), (nl, nfn, '#ff7a6a'))):
            g = fn()
            if j == 0:
                assert not g.check(lab)['warnings'], lab
            ic = g.render()
            x = bx + j * (cell + 10)
            im.paste(Image.new('RGBA', (cell, cell), '#8b8b8b'), (x, by))
            im.alpha_composite(ic.resize((cell, cell), Image.NEAREST), (x, by))
            sy = by + cell + 4
            dr.rectangle([x, sy, x + 33, sy + 33], fill='#8b8b8b')
            im.alpha_composite(ic, (x + 1, sy + 1))
            dr.rectangle([x + 38, sy, x + 71, sy + 33], fill='#1e1e24')
            im.alpha_composite(ic, (x + 39, sy + 1))
            dr.text((x + 78, sy + 2), 'DO' if j == 0 else "DON'T", fill=col, font=fnt)
            line, ty = '', sy + 38
            for w in lab.split(' '):
                if line and fnt.getlength(line + ' ' + w) > cell:
                    dr.text((x, ty), line, fill=col, font=fnt); ty += 15; line = w
                else:
                    line = (line + ' ' + w).strip()
            dr.text((x, ty), line, fill=col, font=fnt)
    im.save(out)
    print(f'{len(PAIRS)} pairs -> {out}')


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'dodont.png')
