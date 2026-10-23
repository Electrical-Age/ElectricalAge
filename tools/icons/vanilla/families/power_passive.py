"""Family helper: power_passive (+ converter) - the PowerElectricPrimitives sixnodes, the fuse
holder and lead fuses, the large rheostat and the DC-DC converter.

Shared visual system
--------------------
* **Sixnode primitives** (capacitor, diodes, inductor, resistor, rheostat, thermistor) all use the
  model's one ``Base`` part: the steel U-bracket of the reference ``icons/power_capacitor.py``
  (copied here as ``bracket()``, identical geometry), seen side-on with the plate at the bottom.
  The component (``*Core`` part) sits BETWEEN the end walls; the right wall is repainted over
  it from a separate render, exactly as in the reference.
* **Diodes** use the same bracket one pixel wider (right wall at x=12): the rating digits
  (FONT3, gap 1, 7 px for "25") plus the cathode band need 9 free columns of can, the
  reference bracket has 7.  The can's left end is hidden behind the left wall (end cap against
  the wall, as in the model), the band is the brightest feature and sits 1 px left of the
  right wall's dark outline; the digits are printed in a dimmer ink, clear of the band.
* **Variant marks.** Where the model cannot tell two items apart (10 A / 25 A diode, resistor /
  thermistor) the item gets a mark *printed on the component body* with ``label()`` (FONT3).
* **Signal diode** (tier signal): the tier is on the mounting base (``plate_ramp('signal')`` as the
  bracket ramp) - the kit's 'plate' cue.
* **Lead fuses**: end-on view of the D-type cap (the model's knurled ``Fuse`` ring) with the
  ``FuseType`` disc behind the window in the tier ramp: the band cue as a large disc of
  ramp[3].  The blown fuse is the same drawing with a neutral, scorched window.
* ``*_block`` variants: the same component in a full-block steel cabinet (front 10x11, depth 4)
  with an open front; the component sits inside the opening - size axis by geometry only.

CERAMIC, the amber of the resistor bodies, is the kit palette ramp (promoted from this module in
kit revision 3, same hex values).  The knurled fuse cap is the kit's ``knurled_disc``.
"""
from kit import (Canvas, solid, paint_solid, faces, cylinder, disc,
                 text, FONT3, plate_ramp, tier_ramp,
                 STEEL, CAV, CAP_BLUE, RUBBER_BLACK, COPPER, FERRITE, CARBON, WHITE, GOLD,
                 MARK_RED, CERAMIC, knurled_disc)

#: rating digits on the black diode can: lighter than the can, clearly dimmer than the band
MARK_INK = RUBBER_BLACK[5]
#: the diode can: black, matte, one step darker than RUBBER_BLACK's faces so the print reads
CAN = (RUBBER_BLACK[0], RUBBER_BLACK[1], RUBBER_BLACK[1], RUBBER_BLACK[2], RUBBER_BLACK[3])


# ------------------------------------------------------------------------------------------------
# the shared bracket (Base part)
# ------------------------------------------------------------------------------------------------
def bracket(ramp=STEEL, rw=11):
    """The reference U-bracket: base plate + two end walls as ONE solid, depth 2.
    ``rw``: x of the right wall (11 = the reference; the diodes use 12)."""
    zb = solid([('base', 1, 13, rw + 1, 2, 2), ('lw', 1, 4, 2, 11, 2), ('rw', rw, 4, 2, 11, 2)])
    cv = Canvas(16)
    paint_solid(cv, zb, ramp)
    return cv, zb


def repaint(cv, zb, front, name, face=None):
    """Repaint part ``name`` (optionally only one face) from ``front`` (occlusion)."""
    for p in faces(zb, name, face):
        cv.set(*p, front.get(*p))


def front_wall(cv, zb, front):
    """Repaint the right wall over whatever was drawn between the walls (occlusion)."""
    repaint(cv, zb, front, 'rw')


def label(cv, x, y, s, colour, gap=1, font=FONT3):
    """A printed rating / marking on a component body (FONT3, 3x5)."""
    return text(cv, x, y, s, colour, font=font, gap=gap)


# ------------------------------------------------------------------------------------------------
# components
# ------------------------------------------------------------------------------------------------
#: cathode band down the can (diameter 7): outline, lit, glint, face, face, shade, outline
BAND = (STEEL[1], STEEL[4], WHITE, STEEL[3], STEEL[3], STEEL[2], STEEL[0])


def diode(mark=None, tier=None):
    """10A / 25A diode (``mark`` = rating digits) and the signal diode (``tier='signal'``: the
    bracket is the signal plate).

    Wide bracket (right wall x=12).  The black can (diameter 7, rows 4..10, matte: no glint row)
    runs from behind the left wall to behind the right wall, so only its body shows (cols 3..11);
    the rating sits on cols 3..9, col 10 is bare can, col 11 is the band, col 12 the right wall's
    dark outline."""
    ramp = plate_ramp(tier) if tier else STEEL
    cv, zb = bracket(ramp, rw=12)
    front = cv.copy()
    cylinder(cv, 2, 4, 11, 7, CAN, round_ends=False)
    for j, c in enumerate(BAND):
        cv.set(11, 4 + j, c)
    if mark:
        label(cv, 3, 5, mark, MARK_INK)
    repaint(cv, zb, front, 'lw', 'front')          # the can's left end cap is behind the wall
    front_wall(cv, zb, front)
    return cv


def capacitor_core(cv, x=4, y=5, length=8, d=6):
    cylinder(cv, x, y, length, d, CAP_BLUE)


def inductor_core(cv, x=3, w=8, y=3, gap=7, fh=2, turns=(6, 8)):
    """Standing drum: COPPER winding between two black octagonal flanges (seen edge-on).
    Flange fronts at rows y..y+fh-1 and y+gap..; the winding fills the rows between them."""
    cylinder(cv, x + 1, y + fh, gap - fh + 1, w - 2, COPPER[:5], axis='y')  # matte wire coil
    for yy in turns:                                       # wire turns
        for xx in range(x + 2, x + w - 2):
            cv.set(xx, yy, COPPER[2] if xx > x + 2 else COPPER[3])
    for fy in (y, y + gap):
        zf = solid([('f', x, fy, w, fh, 1)])
        paint_solid(cv, zf, RUBBER_BLACK, cut=[(x, fy + fh - 1), (x + w, fy - 1)])


def inductor():
    cv, zb = bracket()
    front = cv.copy()
    inductor_core(cv)
    front_wall(cv, zb, front)
    return cv


def resistor_body(cv, x=3, y=5, w=9, hgt=7, foot=True):
    """Ceramic box (ResistorCore), depth 1, both top edges chamfered, optional darker foot row;
    the two brass terminal tabs stand on the top face near both ends.  The body runs from the
    left wall to behind the right wall (visible cols x..10)."""
    zr = solid([('r', x, y, w, hgt, 1)])
    paint_solid(cv, zr, CERAMIC, cut=[(x, y), (x + w, y - 1), (x + 1, y - 1)])
    if foot:
        cv.hline(x + 1, 10, y + hgt - 2, CERAMIC[2])
    for tx in (x + 2, x + 7):                              # terminal tabs, rows y-3..y-2
        cv.set(tx, y - 3, GOLD[4])
        cv.set(tx, y - 2, GOLD[2])
    return zr


#: thermistor print: near-black, distinct from the brown CERAMIC outline
T_INK = RUBBER_BLACK[1]


def resistor(kind='plain'):
    """power_resistor / thermistor (same model; the thermistor is printed "°C") / rheostat
    (red track along the top face, a small black wiper block standing across it)."""
    cv, zb = bracket()
    front = cv.copy()
    resistor_body(cv, foot=(kind != 'thermistor'))
    if kind == 'thermistor':
        cv.set(5, 6, T_INK)                               # degree: one floating pixel left of the C top
        label(cv, 7, 5, 'C', T_INK)                       # C: cols 7-9, rows 5-9 (col 10 bare)
    if kind == 'rheostat':
        cv.hline(5, 10, 4, MARK_RED)                      # track: the whole top face
        for (py, c) in ((2, RUBBER_BLACK[5]), (3, RUBBER_BLACK[3]), (4, RUBBER_BLACK[1])):
            cv.hline(7, 8, py, c)                         # wiper block on the track (lit top)
    front_wall(cv, zb, front)
    return cv


# ------------------------------------------------------------------------------------------------
# full-block variants
# ------------------------------------------------------------------------------------------------
def cabinet(ox=3, oy=6, ow=7, oh=7, back=CAV[0]):
    """Full-block steel cabinet with an open front (front cols 1..10, rows 4..14, depth 4); the
    opening is cols ox..ox+ow-1, rows oy..oy+oh-1 (its left and top inner walls in shade)."""
    cv = Canvas(16)
    zb = solid([('c', 1, 4, 10, 11, 4)])
    paint_solid(cv, zb, STEEL)
    for yy in range(oy, oy + oh):
        for xx in range(ox, ox + ow):
            cv.set(xx, yy, CAV[2] if yy == oy + oh - 1 else back)
    for yy in range(oy, oy + oh):
        cv.set(ox, yy, STEEL[1])
    cv.hline(ox, ox + ow - 1, oy, STEEL[1])
    return cv


def capacitor_block():
    cv = cabinet()
    capacitor_core(cv, x=3, y=7, length=7, d=5)      # inside the opening; its floor shows below
    return cv


def inductor_block():
    cv = cabinet(back=CAV[3])                       # lighter back so the black flanges read
    inductor_core(cv, x=3, w=7, y=7, gap=5, fh=1, turns=(10,))
    return cv


# ------------------------------------------------------------------------------------------------
# fuses
# ------------------------------------------------------------------------------------------------
def knurled_cap(cv, x=1, y=1, d=14, ramp=STEEL):
    """End-on D-fuse cap: a disc with 8 knurl teeth (the kit's ``knurled_disc``)."""
    return knurled_disc(cv, x, y, d, ramp, teeth=8)


def fuse(tier=None):
    """Lead fuse end-on: knurled steel cap, FuseType disc in the tier ramp behind the window.
    ``tier=None``: the blown fuse - neutral, scorched window."""
    cv = Canvas(16)
    knurled_cap(cv)
    if tier:
        disc(cv, 4, 4, 8, tier_ramp(tier))
    else:
        disc(cv, 4, 4, 8, CAV, face=2)
        for p in ((6, 7), (7, 7), (7, 8), (8, 8), (9, 9), (8, 6)):
            cv.set(*p, CAV[0])
    return cv


#: the empty cap seat of the fuse holder (Case part, centre of the face): a copper thread ring
#: round a dark socket.  '1' lit thread, '2' thread, '3' shaded thread, 'k' socket, 'c' centre
SEAT = ["." "123" ".",
        "1kkk3",
        "2kck3",
        "2kkk3",
        "." "333" "."]


def fuse_holder():
    """Case only (the item draws no fuse): a tall narrow light-grey box whose face is faceted -
    lit upper chamfer, shaded lower chamfer - with the empty round threaded seat of the cap in
    the middle (copper thread ring, dark socket)."""
    cv = Canvas(16)
    x, y, w, hgt = 4, 3, 8, 12                           # front face cols 4..11, rows 3..14
    zb = solid([('c', x, y, w, hgt, 2)])
    paint_solid(cv, zb, STEEL, cut=[(x, y), (x + w + 1, y - 2)])
    for yy in range(y + 1, y + 4):                       # upper facet, lit
        cv.hline(x + 1, x + w - 1, yy, STEEL[4])
    for yy in range(y + hgt - 4, y + hgt - 1):           # lower facet, shaded
        cv.hline(x + 1, x + w - 1, yy, STEEL[2])
    pal = {'1': COPPER[4], '2': COPPER[3], '3': COPPER[2], 'k': CAV[0], 'c': CAV[3]}
    for j, row in enumerate(SEAT):                       # seat: cols 6..10, rows 6..10
        for i, ch in enumerate(row):
            if ch != '.':
                cv.set(6 + i, 6 + j, pal[ch])
    return cv


# ------------------------------------------------------------------------------------------------
# full-block devices
# ------------------------------------------------------------------------------------------------
#: top of the large rheostat (rows 1..7) over the plain body box: '.' cut away, ' ' keep the box,
#: '5' lit fin/wall top, '3' inner face of the back fins, 'e'/'c'/'b'/'a' coil lit..dark,
#: 'k' hub, 's' wiper arm.  Back fins at cols 5-6, 9-10, 13-14 (notches between); front fins
#: cols 1-2, 5-6, 9-10 with the coil showing in the 2 px slots between them.
RHEO_TOP = [
    ".....55..55..55.",      # 1  back fin tops
    "....533..33..5 .",      # 2  inner faces of the back fins, notches; wall tops
    "...5eeeskkee5  .",      # 3  left/right wall tops, the coil's far half, wiper, hub
    "  55ee55ee55    ",      # 4  front fin tops, the coil seen down the slots
    "   cc  cc       ",      # 5  slots: the coil's near side
    "   cc  cc       ",      # 6
    "   bb  bb       ",      # 7
    "   aa  aa       ",      # 8
]


def large_rheostat():
    """Full block (largerheostat.obj): a dark castellated heatsink - a box whose rim walls rise
    above a sunken pit, cut into fins - with the tan toroidal coil lying in the pit (seen
    between the back fins and the front fins, and through the front slots), its dark hub and
    the grey wiper arm.  Front cols 1..10, rows 5..14, depth 4; the fin and wall tops are lit
    (CARBON[5]) so the dark block keeps its silhouette on a dark background."""
    cv = Canvas(16)
    Hm = CARBON
    paint_solid(cv, solid([('body', 1, 5, 10, 10, 4)]), Hm)
    for yy in range(5, 14):                               # lit left edge of the front face
        cv.set(2, yy, Hm[4])
    pal = {'5': Hm[5], '3': Hm[3], 'e': COPPER[4], 'c': COPPER[3], 'b': COPPER[2],
           'a': COPPER[1], 'k': CAV[0], 's': STEEL[3]}
    for j, row in enumerate(RHEO_TOP):
        for i, ch in enumerate(row):
            if ch == '.':
                cv.clear(i, 1 + j)
            elif ch != ' ':
                cv.set(i, 1 + j, pal[ch])
    # the light terminal block at the bottom centre of the front
    cv.set(5, 12, STEEL[4]); cv.set(6, 12, STEEL[3])
    cv.set(5, 13, STEEL[3]); cv.set(6, 13, STEEL[2])
    return cv


def dc_dc_converter():
    """Full block (transformator.obj + ferromagnetic core): a low light-grey base with its label
    strip, the dark rectangular core loop standing on it (depth 2, an open window), and thin
    copper winding sleeves on both legs at different heights, as in the model."""
    cv = Canvas(16)
    zb = solid([('b', 1, 11, 10, 4, 4)])
    paint_solid(cv, zb, STEEL)
    cv.hline(2, 6, 13, CAV[2])                           # "DC-DC" label strip
    cv.set(8, 13, CAV[1]); cv.set(9, 13, CAV[1])
    zc = solid([('l', 3, 2, 2, 8, 2), ('r', 10, 2, 2, 8, 2), ('t', 3, 2, 9, 2, 2)])
    paint_solid(cv, zc, FERRITE)
    # winding sleeves: 2-row copper bands round a leg (front face), 1 px proud on the outer side
    for (x0, x1, by) in ((2, 4, 4), (10, 12, 6)):
        for xx in range(x0, x1 + 1):
            cv.set(xx, by, COPPER[4] if xx < x1 else COPPER[3])
            cv.set(xx, by + 1, COPPER[2] if xx < x1 else COPPER[1])
        cv.set(x0, by, COPPER[3]); cv.set(x0, by + 1, COPPER[1])
    return cv

