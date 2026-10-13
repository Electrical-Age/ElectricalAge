"""Family helper for the "parts_modules" job (manifest family machine_part, 16 px, untiered).

Fifteen non-placeable items: crafting chips, protection and regulator upgrades, entity-sensor
filters, the signal antenna and the machine booster.  None has a world model and none is
tiered, so nothing here carries a tier hue: housings are STEEL / RUBBER_BLACK / CAV, and each
icon gets at most one accent colour.

Shared visual system
--------------------
* **Package** (``package``): a black-plastic IC seen from above, front-on like a flat item
  (a quad-flat package: the old textures are top views with pins on all four sides).  It is
  deliberately NOT the kit's ``chip_body`` (the in-world logic-chip sixnode, oblique, with tier-like
  marks): a crafting chip must not read as a placeable logic gate next to ``not_chip``.
  Body cols 3..12 x rows 3..12 in RUBBER_BLACK (black plastic; CHIP/ACHIP are reserved for
  ``chip()``), vanilla outline, lit inner rim on top/left, shaded inner rim bottom/right, and a
  pin-1 dimple top-left on the plain chips (left off where a print or sensor fills the face).  Three
  2 x 2 pins per side, cols/rows 4-5, 7-8, 10-11, inside cols/rows 1..14 (no edge-ring pixels).
  The free face is cols 5..11 x rows 5..11 (``FACE``).  Seven ids use it:
    - cheap_chip / advanced_chip: the quality axis (``quality(id)`` -> ``QUALITY_TRIM``):
      cheap = STEEL pins and a plain silicon die; advanced = GOLD pins (trim level 2) and
      a gold-framed die.
    - electrical_probe_chip 'V' and overvoltage_protection 'VP': electrical ink (LIT, as the
      old 'V' is yellow).  thermal_probe_chip 'T' and overheating_protection 'HP': thermal ink
      (VCOPPER[4], a warm salmon).  The protection is crafted FROM the matching probe chip,
      so each pair shares its ink; one letter = probe chip, two letters = protection upgrade.
    - solar_tracker: the package with a round glass light-sensor dome (GLASS disc) on its face
      (the old icon's cream "sensor" square; the recipe is glass panes + quartz on iron).
* **Regulator** (``regulator``): a small STEEL control module, front-on with oblique depth 1,
  a dark CAV screen showing the control curve, and the red / blue terminals on its left (the
  old icons' identity).  analogic_regulator: a saturating response curve (LIT) on a CAV baseline.
  on/off regulators: a short screen with the on/off step (WHITE) plus a printed hysteresis
  value under it (STYLE 7b ``print`` axis: the two share one texture and one function, so the
  FONT3 "1" / "10" is what tells them apart; the step's hysteresis loop is narrow for 1 % and
  wide for 10 % as a secondary cue).
* **Filter lens** (``lens``): a round tinted glass lens in a STEEL ring, front-on (the filters
  are glass panes + dye; the in-world entity sensor shows the filter's colour).  Tints are the
  descriptor colours: player (0, 1, 0) green, monster (1, 0, 0) red, animal (.3, .3, 1) blue.
  The three tint ramps are a LIGHTNESS ladder on the face step (green L* 72 > red 47 > blue 45,
  red and blue far apart in hue in every mode), so red and green stay apart for protan and
  deutan viewers; checked with ``kit.cvd`` (see ``_check`` at the bottom).
* signal_antenna: a slim 2 px IRON whip mast (crafted from iron cable) on a low STEEL boss, with a
  small black tip knob (the wireless rods' black tip, ``families/wireless``), radiating two arcs per side.
* machine_booster: the old bold red up-arrow, re-drawn as a bevelled solid in RED with a
  printed 'B' on the shaft.

Derived ramps (frozen here, reported as kit requests): RED (painted red; also the monster
lens), GREEN_GLASS, BLUE_GLASS.
"""
from kit import (Canvas, box, outline_region, disc, ring, text, text_size, FONT3, FONT5,
                 h, STEEL, CAV, RUBBER_BLACK, IRON, SILICON, GLASS, LIT, VCOPPER, WHITE,
                 MARK_RED, MARK_BLUE, LEAD_RED, CAP_BLUE, QUALITY_TRIM, quality)

# ----------------------------------------------------------------------------- derived ramps
#: Painted / tinted red (machine booster arrow, monster filter lens).  Hand-tuned from
#: derive_ramp(h('#cc3a2c'), at=3); face [3] L* 47 (VHV[3] is L* 40: dE 20.6 normal vision;
#: the shade step [2] is only dE 9.5 from VHV[3], so RED stays off tiered devices).
#: Near families/sensors.CUP_RED (the anemometer cups) - candidate for one promoted RED.
RED = (h('#3e1012'), h('#6e1a18'), h('#9e2620'), h('#cc3a2c'), h('#ec6a4c'), h('#f8b09a'))
#: Green-tinted glass (player filter, descriptor colour (0, 1, 0)): a yellow-green kept clear of
#: the LV mint (dE >= 30 to every LV step); face [3] L* 72 = the top of the lightness ladder.
GREEN_GLASS = (h('#1f3c12'), h('#33641a'), h('#559a24'), h('#7cc436'), h('#a6e052'), h('#dcf6a0'))
#: Blue-tinted glass (animal filter, descriptor (.3, .3, 1)): a periwinkle / indigo, violet of
#: the MV sky blue (dE >= 37 to every MV step).
BLUE_GLASS = (h('#1c1a48'), h('#2c2c7a'), h('#3e42a8'), h('#5560d0'), h('#7f8cec'), h('#c0c8f8'))

#: The ink a probe chip / protection prints its letters in.
INK_ELECTRIC = LIT[3]
INK_THERMAL = VCOPPER[4]

PKG = RUBBER_BLACK
#: Free face of the package (x, y, w, h).
FACE = (5, 5, 7, 7)
_PINS_AT = (4, 7, 10)            # first col/row of each 2-px pin along a side


# ----------------------------------------------------------------------------- package
def _pins(cv, ramp):
    """Twelve 2 x 2 pins round the package body (cols/rows 1-2 and 13-14)."""
    lit, mid, dark = ramp[4], ramp[3], ramp[1]
    for a in _PINS_AT:
        # left / right pins (horizontal): lit top row, darker bottom row; the outer end darker
        for x0 in (1, 13):
            cv.set(x0, a, lit if x0 == 13 else mid)
            cv.set(x0 + 1, a, lit)
            cv.set(x0, a + 1, dark)
            cv.set(x0 + 1, a + 1, dark if x0 == 1 else ramp[2])
            if x0 == 13:
                cv.set(14, a, mid)
                cv.set(14, a + 1, dark)
        # top / bottom pins (vertical): lit left column, darker right column
        for y0 in (1, 13):
            cv.set(a, y0, lit)
            cv.set(a + 1, y0, ramp[2])
            cv.set(a, y0 + 1, lit if y0 == 1 else mid)
            cv.set(a + 1, y0 + 1, ramp[2] if y0 == 1 else dark)
            if y0 == 13:
                cv.set(a + 1, 14, dark)


def package(pins=STEEL, dimple=True):
    """The black QFP package (see module doc).  Returns the Canvas; draw on ``FACE``."""
    cv = Canvas(16)
    _pins(cv, pins)
    reg = {(x, y) for y in range(3, 13) for x in range(3, 13)}
    for p in reg:
        cv.set(*p, PKG[3])
    outline_region(cv, reg, PKG[0], PKG[1])
    cv.hline(4, 11, 4, PKG[4])                    # lit inner rim: top row, left column
    cv.vline(4, 5, 11, PKG[4])
    cv.set(4, 4, PKG[5])
    cv.hline(5, 11, 11, PKG[2])                   # shaded inner rim: bottom row, right column
    cv.vline(11, 5, 11, PKG[2])
    if dimple:
        cv.set(5, 5, PKG[1])                      # pin-1 dimple
    return cv


def chip_quality(item_id):
    """cheap_chip / advanced_chip: the quality axis on pins and die (see module doc)."""
    trim = QUALITY_TRIM[quality(item_id)]
    cv = package(pins=trim or STEEL)
    S = SILICON
    x, y, w, hh = 6, 6, 4, 4                      # the silicon die, centred on the face
    if trim is not None:                          # gold frame round the die (bond ring)
        fr = {(xx, yy) for yy in range(y - 1, y + hh + 1) for xx in range(x - 1, x + w + 1)}
        for p in fr:
            cv.set(*p, trim[3])
        for xx in range(x - 1, x + w + 1):
            cv.set(xx, y - 1, trim[4])
        for yy in range(y - 1, y + hh + 1):
            cv.set(x - 1, yy, trim[4])
        cv.hline(x, x + w, y + hh, trim[2])
        cv.vline(x + w, y, y + hh, trim[2])
    cv.rect(x, y, w, hh, S[3])
    cv.hline(x, x + w - 1, y, S[5])
    cv.vline(x, y, y + hh - 1, S[4])
    cv.set(x + w - 1, y + hh - 1, S[2])
    return cv


def lettered(s, ink, font=None):
    """A package with ``s`` printed centred on its face (1 letter FONT5, 2 letters FONT3)."""
    cv = package(dimple=False)
    fx, fy, fw, fh = FACE
    if s in _GLYPHS:                              # hand-placed glyph (FONT5's V reads as U)
        g = _GLYPHS[s]
        cv.overlay(g, {'#': ink}, fx + (fw - len(g[0]) + 1) // 2, fy + max(0, (fh - 1 - len(g)) // 2))
        return cv
    font = font or (FONT5 if len(s) == 1 else FONT3)
    w, hh = text_size(s, font)
    x = fx + (fw - w + 1) // 2
    y = fy + (fh - hh + 1) // 2
    text(cv, x, y, s, ink, font=font)
    return cv


#: Hand-placed face glyphs.  'V' narrows on every pair of rows so it cannot read as 'U'.
_GLYPHS = {
    'V': ["#...#",
          "#...#",
          ".#.#.",
          ".#.#.",
          "..#..",
          "..#.."],
}


def solar_tracker():
    cv = package()
    disc(cv, 5, 5, 7, GLASS)                      # glass sensor dome, cols/rows 5..11
    cv.rect(8, 8, 2, 2, LIT[3])                   # the photocell under the glass, catching light
    cv.set(7, 6, GLASS[5])
    cv.set(6, 7, GLASS[5])
    return cv


# ----------------------------------------------------------------------------- regulator
#: Screen traces (local grids, '#' = trace), 7 wide.
_RAMP_CURVE = [                   # a saturating response: steep rise, flattening top right
    "....###",
    "..##...",
    ".#.....",
    ".#.....",
    "#......",
    "#......",
    "#......",
]
#: On/off step: 1 % = a plain step, 10 % = a wide hysteresis loop.
_STEP = {
    '1': ["...####",
          "...#...",
          "####..."],
    '10': [".######",
           ".#...#.",
           "######."],
}


def regulator(kind, label=None):
    """kind 'analog' (smooth rising curve) or 'onoff' (the step and the printed ``label``)."""
    cv = Canvas(16)
    box(cv, 2, 3, 11, 12, 2, STEEL)               # front cols 2..12, rows 3..14; depth 2
    # terminals on the left wall: red (upper) and blue (lower), 2 x 2 posts at cols 0..1
    for (y, top, body, dark) in ((6, LEAD_RED[2], MARK_RED, LEAD_RED[0]),
                                 (9, CAP_BLUE[4], MARK_BLUE, CAP_BLUE[1])):
        cv.set(0, y, top)
        cv.set(1, y, top)
        cv.set(0, y + 1, body)
        cv.set(1, y + 1, dark)
    # screen: CAV inset cols 4..11, rows 4..13 under a 1 px bezel; the inner left jamb is the
    # body's ramp[1] (STYLE 3).  Content area cols 5..11, rows 4..12.
    sx, sy, sw, sh = 4, 4, 8, 10
    cv.rect(sx, sy, sw, sh, CAV[1])
    cv.vline(sx, sy, sy + sh - 1, STEEL[1])
    if kind == 'analog':
        cv.hline(sx + 1, sx + sw - 1, sy + sh - 2, CAV[3])   # graph baseline, row 12
        cv.overlay(_RAMP_CURVE, {'#': LIT[3]}, sx + 1, sy + 1)   # curve rows 5..11
    else:
        # one CAV row between the light top bezel and the step, so the step reads as a trace
        cv.overlay(_STEP[label], {'#': WHITE}, sx + 1, sy + 1)
        tw, _ = text_size(label, FONT3)
        text(cv, sx + 1 + (7 - tw + 1) // 2, sy + 5, label, LIT[3], font=FONT3)
    return cv


# ----------------------------------------------------------------------------- filter lens
def lens(glass):
    cv = Canvas(16)
    # steel mount, cols/rows 1..14: the band is drawn unoutlined, the glass disc brings its own
    # (normal, STYLE 4) outline on the inside, and the whole lens gets one outer outline, so a
    # 1 px lit/shaded metal band shows between the two outlines.
    band = ring(cv, 1, 1, 14, STEEL, width=2, outline=False)
    face = disc(cv, 3, 3, 10, glass)               # tinted glass inside the mount
    outline_region(cv, band | face, STEEL[0], STEEL[1])
    for p in ((5, 5), (6, 5), (5, 6)):            # glint, top-left
        cv.set(*p, glass[5])
    cv.set(9, 10, glass[4])
    return cv


# ----------------------------------------------------------------------------- antenna
def signal_antenna():
    """A slim whip: 2 px IRON mast (cols 7..8, rows 4..12), a 2 x 2 black tip knob (rows 2..3)
    with a lit corner so it holds on dark slots, a low STEEL boss for a foot (rows 13..14), and
    two LIT arcs per side centred on the knob."""
    cv = Canvas(16)
    # foot: one lit row of steel on a dark underside, cols 5..10
    for x, c in zip(range(5, 11), (STEEL[1], STEEL[4], STEEL[3], STEEL[3], STEEL[2], STEEL[0])):
        cv.set(x, 13, c)
    cv.hline(5, 10, 14, STEEL[0])
    # mast: lit left column, shaded right column (the shaded side doubles as its outline)
    cv.vline(7, 4, 12, IRON[4])
    cv.vline(8, 4, 12, IRON[1])
    cv.set(7, 4, IRON[5])
    # tip knob
    cv.set(7, 2, RUBBER_BLACK[5])
    cv.set(8, 2, RUBBER_BLACK[3])
    cv.set(7, 3, RUBBER_BLACK[2])
    cv.set(8, 3, RUBBER_BLACK[0])
    # radio arcs, two per side, centred on the knob (rows 2..3), mirrored about x = 7.5
    arcs_l = [[(5, 1), (4, 2), (4, 3), (5, 4)],
              [(3, 0), (2, 1), (2, 2), (2, 3), (2, 4), (3, 5)]]
    for arc in arcs_l:
        for (x, y) in arc:
            cv.set(x, y, LIT[3])
            cv.set(15 - x, y, LIT[2])
    return cv


# ----------------------------------------------------------------------------- booster
ARROW = [
    "................",
    ".......##.......",
    "......####......",
    ".....######.....",
    "....########....",
    "...##########...",
    "..############..",
    ".##############.",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "....########....",
    "................",
]


def machine_booster():
    cv = Canvas(16)
    reg = {(x, y) for y, r in enumerate(ARROW) for x, ch in enumerate(r) if ch == '#'}
    for (x, y) in reg:                            # a ridge down the middle: lit left, shaded right
        cv.set(x, y, RED[3] if x <= 7 else RED[2])
    edge = outline_region(cv, reg, RED[0], RED[1])
    inner = reg - edge
    for (x, y) in inner:                          # lit bevel one px inside the left / top outline
        if x <= 7 and ((x - 1, y) not in inner or (x, y - 1) not in inner):
            cv.set(x, y, RED[4])
    # near-black print (STYLE 7b) on the shaft; the label patch behind it is flat RED[3] so the
    # B's counters stay light (on the split ridge it read as dark hatching at GUI-2)
    # Shaft is 6 px inside its outline (cols 5..10) so the 3 px B keeps a margin on both sides.
    cv.rect(7, 9, 3, 5, RED[3])
    text(cv, 7, 9, 'B', RED[0], font=FONT3)
    return cv


# ----------------------------------------------------------------------------- self-check
def _check():
    """Print the lens tints' pairwise dE (face steps 2..4, four vision modes) and each tint's
    nearest tier colour.  Run: python3 -c 'from families.parts_modules import _check; _check()'"""
    import itertools
    from kit.cvd import delta_e, MODES
    from kit import TIER
    tints = {'player': GREEN_GLASS, 'monster': RED, 'animal': BLUE_GLASS}
    for a, b in itertools.combinations(tints, 2):
        for i in (2, 3, 4):
            print(a, b, i, ' '.join('%s %.1f' % (m[:3], delta_e(tints[a][i], tints[b][i], m))
                                    for m in MODES))
    for k, r in tints.items():
        print(k, 'nearest tier (normal) %.1f %s[%d]' % min(
            (delta_e(r[i], R[j]), t, j) for i in (2, 3, 4) for t, R in TIER.items() for j in (2, 3, 4)))
