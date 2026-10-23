"""machines family helpers (schematic set): process machines, the turret, the transporter, the shaft parts.

The family's visual system (STYLE.md sections 6, 7.2, 8.6 and 11):

Process machines (STYLE 7.2): INK input cue at the top -> the PROCESS ELEMENT in the tier hue in the
middle (what tells the machines apart, mostly above the stack count) -> INK product. No motor ring, no
casing. 50V / 200V twins are the same program with a different TIER (LV green / MV blue).
    macerator     kit macerator_rollers(): two offset toothed rollers, feed arrowhead, grit (reference)
    compressor    two hue plates closing on an INK workpiece, INK arrowheads pressing in from top and bottom
    plate machine a hue T ram coming down onto a hue anvil bar, the INK plate leaving to the right as an
                  arrow (product out): asymmetric, so it never reads as the compressor's squeeze
    magnetizer    IEC inductor with a magnetic core: the humps in the hue, the INK core bar above them
    furnace       the convention (STYLE 8.6): heat_waves at HEAT_TOP over an INK resistor on MID_M
    egg incubator the heater in the LV hue with an INK egg centred above it and one INK heat wave on
                  each side of the egg (heat rising round the egg). Deliberately NOT the furnace /
                  heating-corp layout (waves left, mark in the slot right), and the egg is never in
                  the mark slot (it is not a variant mark)
    auto miner    a drill arrow in the hue going down through a broken INK surface line into INK ore grains

Function glyphs (STYLE 11):
    turret        a gun-sight in the hue (ring + four cross ticks through it) with an INK aim dot
    transporter   two hue pods (plan-view D=8 rings, top-left and bottom-right) and an INK DOTTED transfer
                  arrow from one to the other along an L (2x2 dots on a 5 px pitch, i.e. dashed() spacing,
                  3 clear px everywhere, including before the arrowhead): a transfer with no physical path

Shaft parts (untiered, all INK). The shaft is a DOUBLE RAIL (two parallel 2px lines, 4 clear rows
apart: the mechanical-drawing outline of a rod), so a shaft can never be read as an electrical wire
(hub: single solid wires + a filled contact node; utility pole: one line with a small ring).
    joint         the shaft running into a bearing collar (open D=12 ring) from both sides
    joint_hub     shafts from all four sides meeting at the same collar ring (continuous rails, so the
                  rail ends never read as gear teeth)
    flywheel      the shaft through the heavy disc seen edge-on (a tall empty rectangle, as in kinematic
                  diagrams)
"""
import math

from kit import (INK, MID, MID_M, LO, HI, HEAT_TOP, RING, ARROW_HEADS, arrow, dashed, grains, heat_waves,
                 resistor, inductor, junction)


# ---- process machines -------------------------------------------------------------------------------
def compressor(g):
    """Two hue plates (cols 6..25, rows 10-11 and 20-21) with INK arrowheads pressing them inward from
    the top and bottom core edges, and the INK workpiece squeezed flat between them (rows 15-16)."""
    g.hline(6, 25, 10, g.hue)
    arrow(g, 15, LO, 15, 8)                       # shaft rows 4-5, head rows 6..9 touching the plate
    g.mirror_y()                                  # lower plate + the upward arrow (rows 20..27)
    g.hline(10, 21, MID)                          # the workpiece, 3 clear rows from each plate


def plate_machine(g):
    """A hue T ram (stem cols 11-12 from the top edge, head cols 4..19 rows 9-10) over a hue anvil bar
    (cols 4..19, rows 23-24); the INK plate runs out midway between them (rows 16-17, 5 clear rows
    to each) to the right and ends in an arrowhead at the core edge (product out)."""
    g.vline(11, LO, 8, g.hue)
    g.hline(LO, 19, 9, g.hue)
    g.hline(LO, 19, 23, g.hue)
    arrow(g, 7, 16, HI - 1, 16)


def magnetizer(g):
    """IEC inductor with a magnetic core: three hue humps (rows 15..21) with INK leads on rows 20-21,
    the INK core bar parallel above them (cols 7..24, rows 10-11); centred on the canvas."""
    inductor(g, g.hue, y=20)
    g.hline(7, 24, 10)


def furnace(g):
    """Electrical furnace (STYLE 8.6, untiered): INK heat waves at HEAT_TOP over an INK resistor on MID_M."""
    heat_waves(g, *HEAT_TOP)
    resistor(g, y=MID_M)


def egg(g, cx=16.0, cy=9.5, w=10, h=11, point=0.35):
    """INK egg outline (2px stroke), w x h px centred on the pixel-corner point (cx, cy), the top
    clearly narrower than the bottom (so it never reads as a '0'; an 8 px egg did, and its tapered
    top hole closed up under the halo). Default: centred above a part on MID_M + 3, cols 11..20,
    rows 4..14."""
    pts = []
    for i in range(129):
        t = 2 * math.pi * i / 128
        s = math.sin(t)
        rx = (w / 2 - 1) * (1 + point * s if s < 0 else 1)     # narrower above the centre
        pts.append((cx + rx * math.cos(t), cy + (h / 2 - 1) * s))
    g.stroke(pts, INK)


def egg_incubator(g):
    """The INK egg centred above the heater (cols 11..20, rows 4..14) with one INK heat wave on each
    side of it (one heat_waves mark, cols 4..7 and 24..27, 3 clear cols from the egg); the heater
    resistor in the tier hue three rows below MID_M (rows 18..27, leads rows 22-23), 3 clear rows
    under the egg."""
    heat_waves(g, 4, 4, n=2, pitch=20)
    egg(g)
    resistor(g, g.hue, y=MID_M + 3)


def auto_miner(g):
    """Drill arrow in the hue (shaft cols 15-16 from the top edge, head rows 18..21) through a broken INK
    surface line (rows 10-11, 3 clear cols each side of the shaft) into four INK ore grains round the
    tip (rows 23..27)."""
    arrow(g, 15, LO, 15, 20, g.hue)
    g.hline(LO, 11, 10)
    g.mirror_x(only=lambda x, y: y in (10, 11) and x <= 11)
    grains(g, ((6, 23), (10, 26), (20, 26), (24, 23)))


# ---- function glyphs ---------------------------------------------------------------------------------
def turret(g):
    """Gun-sight in the hue: the standard RING (D=18, cols/rows 7..24) and four cross ticks from the core
    edges through the ring to row/col 10; the INK aim dot (4x4, cols/rows 14..17) 3 px clear of the ticks."""
    g.circ(*RING, g.hue)
    g.vline(15, LO, 10, g.hue)
    g.hline(LO, 10, 15, g.hue)
    g.mirror_x(only=lambda x, y: x <= 10 and y in (15, 16))
    g.mirror_y(only=lambda x, y: y <= 10 and x in (15, 16))
    junction(g, 15, 15, INK)


def transporter(g):
    """Two hue pods (D=8 rings, hole 4 px: cols 5..12 rows 4..11, cols 18..25 rows 20..27) and an INK
    dotted transfer arrow from the first: dashed() dots right along rows 7-8 (cols 16-17, then the
    corner dot 21-22), 4 clear rows, then the arrowhead (rows 13..16) aimed at the second pod and 3 px
    clear of it. The head does not touch the pod: joined, the two read as a flask. The glyph spans
    cols 5..25 (centred)."""
    g.circ(5, 4, 8, g.hue)
    g.circ(18, 20, 8, g.hue)
    with g.group():                               # one dotted arrow: dots + head form a single mark
        dashed(g, 16, 7, 22, 7)                   # dots cols 16-17 and 21-22 (the corner)
        g.bitmap(18, 13, ARROW_HEADS['down'])


# ---- shaft parts (untiered) --------------------------------------------------------------------------
RAILS = (12, 18)                 # the two rails of a horizontal shaft: rows 12-13 and 18-19 (centre 15.5)
COLLAR = (10, 10, 12)            # bearing / hub collar ring: cols/rows 10..21, the rails run into its sides


def shaft_h(g, x0, x1):
    """Horizontal double-rail shaft, cols x0..x1."""
    for y in RAILS:
        g.hline(x0, x1, y)


def shaft_v(g, y0, y1):
    """Vertical double-rail shaft, rows y0..y1 (rails on cols 12-13 and 18-19)."""
    for x in RAILS:
        g.vline(x, y0, y1)


def collar(g):
    x0, y0, D = COLLAR
    g.circ(x0, y0, D)


def joint(g):
    """Straight joint: the shaft from both side edges running into the bearing collar."""
    collar(g)
    shaft_h(g, LO, COLLAR[0])
    g.mirror_x(only=lambda x, y: x <= COLLAR[0] and y in (12, 13, 18, 19))


def joint_hub(g):
    """Joint hub: shafts from all four edges meeting at the collar."""
    joint(g)
    shaft_v(g, LO, COLLAR[1])
    g.mirror_y(only=lambda x, y: y <= COLLAR[1] and x in (12, 13, 18, 19))


def flywheel(g):
    """Flywheel: the shaft from both side edges into the heavy disc seen edge-on, a tall empty
    rectangle (cols 11..20, full core height, a 6 px wide hole)."""
    g.frame(11, LO, 20, HI)
    shaft_h(g, LO, 11)
    g.mirror_x(only=lambda x, y: x <= 11 and y in (12, 13, 18, 19))
