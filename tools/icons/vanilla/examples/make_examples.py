"""Regenerate the example sheets referenced by STYLE.md.

    python3 examples/make_examples.py      (from vanilla/)
      -> examples/do_dont.png       every 'dont_' icon breaks exactly one rule; 'do_' is it done right
      -> examples/primitives.png    worked examples of the kit primitives (examples/primitives.py)
      -> examples/png/*.png         the individual icons, incl. examples/src/*.py sidecars
                                    (e.g. macerator_lv_32: how a 16 px drawing scales to 32)
None of these are item icons; build.py never sees them.
"""
import importlib.util
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SET = os.path.dirname(HERE)
sys.path.insert(0, SET)
sys.path.insert(0, HERE)
from kit import *                                   # noqa: E402,F403
from kit.chip import chip_body, CHIP_FACES, _inked  # noqa: E402
from families.macerator import macerator           # noqa: E402
import families.macerator as fm                     # noqa: E402
import primitives                                   # noqa: E402

D = os.path.join(HERE, 'png')
os.makedirs(D, exist_ok=True)
CONTACT = os.path.join(os.path.dirname(SET), 'tools', 'contact.py')


def save(name, cv, bucket):
    (cv.image() if isinstance(cv, Canvas) else cv).save(os.path.join(D, name + '.png'))
    bucket.append(name)


pairs = []
# 1. outline in the object's own darkest hue, never black
save('do_outline_own_hue', macerator('lv'), pairs)
cv = macerator('lv')
cv.recolor({TIER['lv'][0]: h('#000000'), TIER['lv'][1]: h('#000000')})
save('dont_outline_black', cv, pairs)

# 2. 32 px: 2 px outline (same weight as 16) vs 1 px (reads as a different, finer style)
save('do_32_outline_2px', macerator('lv', 32), pairs)
fm.paint_solid = lambda cv, zb, r, bevel=True, ow=None, cut=(): paint_solid(cv, zb, r, bevel, 1, cut)
save('dont_32_outline_1px', macerator('lv', 32), pairs)
fm.paint_solid = paint_solid

# 3. cable tiers by colour, not by width
save('do_cable_mv_colour', cable('mv'), pairs)
cv = Canvas(16)
strip(cv, CABLE, width=4)
save('dont_cable_grey_width', cv, pairs)


# 4. tier on the largest area (backplate), not a pip
def switch(t, pip=False):
    cv = Canvas(16)
    if pip:
        box(cv, 1, 9, 12, 5, 2, STEEL)
    else:
        tier_plate(cv, 1, 9, 12, 5, 2, t)
    box(cv, 4, 7, 6, 5, 2, CHIP)
    box(cv, 6, 2, 2, 6, 1, STEEL)
    if pip:
        tier_pip(cv, 10, 11, t)
    return cv


save('do_tier_on_base', switch('hv'), pairs)
save('dont_tier_as_pip', switch('hv', pip=True), pairs)

# 5. chip faces at 32: 2 px strokes (the kit table) vs the texture's 1 px strokes
save('do_32_glyph_2x', chip_icon('nor_chip'), pairs)
cv, g = chip_body(32)
for (x, y, ch) in _inked(CHIP_FACES['nor_chip']['face'], 1, 13, 15):
    cv.set(x, y, GLYPH)
save('dont_32_glyph_1x', cv, pairs)

# 6. no antialiasing / no resampling: a 16 px icon must not be a downscaled render
im = macerator('lv', 32).image().resize((16, 16), resample=3)   # bicubic -> soft edges, partial alpha
save('dont_resampled', im, pairs)
save('do_hand_placed', macerator('lv'), pairs)

# 7. one material = one solid() union (no seams) vs separate boxes
zb = solid([('base', 1, 13, 12, 2, 2), ('lw', 1, 4, 2, 11, 2), ('rw', 11, 4, 2, 11, 2)])
cv = Canvas(16)
paint_solid(cv, zb, STEEL)
save('do_one_solid', cv, pairs)
cv = Canvas(16)
box(cv, 1, 13, 12, 2, 2, STEEL); box(cv, 1, 4, 2, 11, 2, STEEL); box(cv, 11, 4, 2, 11, 2, STEEL)
save('dont_seamed_boxes', cv, pairs)

# 8. cable ends tinned (steel), not copper next to the tier colour
save('do_cable_steel_ends', cable('hv'), pairs)
cv = Canvas(16)
strip(cv, tier_ramp('hv'), width=4, ends=COPPER)
save('dont_cable_copper_ends', cv, pairs)

prims = []
for name, fn in primitives.EXAMPLES.items():
    save(name, fn(), prims)

side = []
for f in sorted(os.listdir(os.path.join(HERE, 'src'))):
    if f.endswith('.py'):
        spec = importlib.util.spec_from_file_location('ex_' + f[:-3], os.path.join(HERE, 'src', f))
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        save(f[:-3], mod.draw(), side)

subprocess.run([sys.executable, CONTACT, os.path.join(HERE, 'do_dont.png'), D] + pairs
               + ['--cols', '8', '--zoom', '6'])
subprocess.run([sys.executable, CONTACT, os.path.join(HERE, 'primitives.png'), D] + prims + side
               + ['--cols', '9', '--zoom', '6'])
