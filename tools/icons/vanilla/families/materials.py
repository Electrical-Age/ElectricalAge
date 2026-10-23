"""Family helper: materials - ingots, dusts, plates, bare crafting wires and the raw materials
(dielectric, mercury, rubber, tree resin).  None of them is placeable; STYLE.md sections 6 and 8.

Shared visual system
--------------------
* **Form = the kind of item, ramp = the material** (STYLE 7b "material" axis).  The material
  ramp always comes from ``kit.material_of(item_id)`` (coal -> CARBON, copper -> VCOPPER, ...),
  never chosen per icon.  The kind comes from the id suffix:

  ``_ingot``  kit ``ingot(ramp)``  (the kit's own end-on trapezoid bar)
  ``_dust``   kit ``dust(ramp)``   (the kit's own low heap)
  ``_plate``  kit ``plate(ramp)``  (the kit's own sheet lying flat in oblique)
  ``_cable``  ``wire(ramp)`` below: a loose loop of bare wire

  The silhouettes are the kit's and are never redrawn here, so every material in the mod reads
  the same way.  Copper items are vanilla copper (VCOPPER); ELN has no copper ingot any more.

* **Bare crafting wires** (copper_cable, iron_cable, tungsten_cable) are drawn with the kit's
  ``strip()`` in the material ramp, ``ends=None`` (no tinned STEEL ends), 2 px wide.  Colour
  alone cannot separate them from the tier cables (STYLE 5.3: VCOPPER ~ VHV, IRON ~ signal,
  TUNGSTEN ~ THERMAL), so the SILHOUETTE does: one loose loop with an empty ~6 x 4 opening and
  a free end crossing over it to the bottom-left, instead of the tier cables' single S-bend with
  two steel ends.  Loop and free end are separate ``strip()`` calls painted back to front, so
  the free end gets its own outline where it crosses the loop (reads as wire lying over wire).

* **Raw materials**: each one has its OWN outline, so the three blobs never read as one shape
  in three colours:
  ``mercury``     IRON (the only grey ramp with a white glint): spilled droplets, one big bead at
                  the back and two smaller ones in front, all on the floor, each outlined on its
                  own; chrome-shaded (bright top, dark reflected middle, lighter bounce below).
  ``rubber``      RUBBER_BLACK: a round, slightly squashed ball with a 2 x 2 glint.
  ``tree_resin``  CERAMIC (the frozen amber): a tall drop with a rounded 2 px tip, with a ramp[5] glint at the
                  top-left and a lighter inner glow at the bottom-right (translucent amber).
  ``dielectric``  SAND (derived below, frozen here, reported for promotion): a pressed slab in
                  cabinet oblique, thicker than a plate, with a few grain pixels (no seam).

Index grids (``paint_grid``) hold ramp indices 0..5, like ``kit.templates``; the outline is
recomputed from the silhouette with the vanilla rule, so boundary digits do not matter.
"""
import math

import kit
from kit import Canvas, outline_region, strip, box, ingot, dust, plate, material_of, h

#: Pale sand for dielectric (compressed sand).  derive_ramp(h('#e3d49a')) (vanilla sand hue,
#: lifted to the 1.12 icon's yellow), frozen as derived.  Not a tier colour; far from GOLD (the
#: sand is paler and much less saturated) and from CERAMIC (L* 85 vs 77 on the top face).
SAND = (h('#362411'), h('#5c421c'), h('#816326'), h('#b39a57'), h('#e3d49a'), h('#fbf0d0'))


def ramp_of(item_id):
    """The frozen material ramp an id names (kit.material_of)."""
    name = material_of(item_id)
    if name is None:
        raise ValueError(f'{item_id}: no material in the id')
    return getattr(kit, name)


def paint_grid(grid, ramp):
    """Paint an index grid (digits 0..5 = ramp steps, '.' empty) and outline the silhouette
    with the vanilla rule (ramp[1] top/left, ramp[0] bottom/right)."""
    cv = Canvas(16)
    pal = {str(i): ramp[min(i, len(ramp) - 1)] for i in range(6)}
    cv.overlay(grid, pal)
    outline_region(cv, cv.region(), ramp[0], ramp[1])
    return cv


# ------------------------------------------------------------------------------ bare wire
def _bezier(p0, p1, p2):
    return lambda t: ((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
                      (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])


def _arc(cx, cy, rx, ry, a0, a1):
    """Elliptical arc from angle a0 to a1 (degrees, y down)."""
    def f(t):
        a = math.radians(a0 + (a1 - a0) * t)
        return cx + rx * math.cos(a), cy + ry * math.sin(a)
    return f


def wire(ramp, width=2):
    """A loose loop of bare wire, in the material ramp with no tinned ends.

    One strand: an open loop (from the bottom-left round over the top to the bottom-right)
    whose free end then runs off to the bottom-left, crossing over the loop's start.  The two
    pieces are separate ``strip()`` calls painted back to front, so the free end carries its own
    outline where it crosses the loop (wire over wire).  The opening is about 6 x 4 px and empty,
    so at GUI 2 it reads as a loop of wire (vanilla ``lead`` idiom), not as rings or a target.
    """
    cv = Canvas(16)
    strip(cv, ramp, width=width, path=_arc(8.8, 6.4, 5.0, 4.2, 120, 420), ends=None)        # loop
    strip(cv, ramp, width=width, path=_bezier((11.3, 10.0), (7.0, 12.0), (1.6, 13.4)), ends=None)  # free end
    return cv


# ------------------------------------------------------------------------------ raw materials
#: Mercury beads, back to front: (grid, ox, oy).  Each bead is outlined on its own, so a nearer
#: bead keeps its outline where it overlaps the one behind.  All three sit on or next to the floor
#: (big bead at the back on row 12, the two nearer ones on row 13): spilled droplets, not a stack.
MERCURY_BEADS = [
    ([                      # big bead (back, centre-left): bright top, dark reflected band, bounce
        "...11111...",
        "..1455431..",
        ".145544331.",
        ".134433321.",
        ".122222221.",
        ".122222220.",
        "..1333320..",
        "...00000...",
    ], 1, 5),
    ([                      # middle bead (front right)
        ".111.",
        "15441",
        "12220",
        "13320",
        ".000.",
    ], 9, 9),
    ([                      # small bead (front left), a plus: glint in the middle
        ".1.",
        "150",
        ".0.",
    ], 1, 11),
]

RUBBER = [
    "................",
    "................",
    "................",
    "................",
    ".....111111.....",
    "....14444431....",
    "...1445543321...",
    "..144554333221..",
    "..144444333221..",
    "..144433332221..",
    "..143333322221..",
    "..133333222220..",
    "...1332222220...",
    "....00000000....",
    "................",
    "................",
]

TREE_RESIN = [
    "................",
    ".......11.......",
    "......1440......",
    "......14420.....",
    "......14320.....",
    ".....1443320....",
    "....154333220...",
    "...15433333220..",
    "...14333333320..",
    "..1433333334320.",
    "..1433333344320.",
    "..1433333443220.",
    "...13333332220..",
    "....000000000...",
    "................",
    "................",
]


def mercury():
    cv = Canvas(16)
    for grid, ox, oy in MERCURY_BEADS:
        bead = Canvas(16)
        bead.overlay(grid, {str(i): kit.IRON[i] for i in range(6)}, ox, oy)
        outline_region(bead, bead.region(), kit.IRON[0], kit.IRON[1])
        cv.paste(bead)
    return cv


def rubber():
    return paint_grid(RUBBER, kit.RUBBER_BLACK)


def tree_resin():
    return paint_grid(TREE_RESIN, kit.CERAMIC)


def dielectric():
    """A pressed sand slab: front 11 x 4 (cols 1..11, rows 10..13), depth 3.  No seam; the
    compressed-sand grain is two hand-placed pixels on the front (ramp[2]) and two on the top
    (ramp[3]), STYLE section 3 texture rule."""
    cv = Canvas(16)
    box(cv, 1, 10, 11, 4, 3, SAND)
    for (x, y) in ((4, 11), (8, 12)):                   # front grain
        cv.set(x, y, SAND[2])
    for (x, y) in ((6, 8), (11, 8)):                    # top grain
        cv.set(x, y, SAND[3])
    return cv


# ------------------------------------------------------------------------------ dispatcher
_SPECIAL = {'mercury': mercury, 'rubber': rubber, 'tree_resin': tree_resin, 'dielectric': dielectric}


def icon(item_id):
    """The icon for any id of the material family."""
    if item_id in _SPECIAL:
        return _SPECIAL[item_id]()
    kind = item_id.rsplit('_', 1)[1]
    r = ramp_of(item_id)
    if kind == 'ingot':
        return ingot(r)
    if kind == 'dust':
        return dust(r)
    if kind == 'plate':
        return plate(r)
    if kind == 'cable':
        return wire(r)
    raise ValueError(f'{item_id}: unknown material kind {kind!r}')
