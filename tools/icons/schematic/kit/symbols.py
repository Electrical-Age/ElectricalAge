"""Symbol primitives for the schematic glyph set.

Every function draws onto a Glyph `g`. Arguments shared by most functions:

    c     colour of the DEFINING element (the part that carries the tier hue): pass g.hue.
    lead  colour of leads / connecting wires: INK by default (switch-type parts default to c).
    with_leads=True/False  draw the horizontal leads out to the core edge.
    y     the top row of the 2px centre line the part is threaded on (MID = 15 -> rows 15-16).
    ring  (x0, y0, D) of a ring symbol: bbox top-left and even diameter.

LAYOUTS (STYLE.md section 7). Pick one per icon; every number below is tested by kit.catalog:

    standard   the symbol alone, centred: two-terminal parts on rows 15-16 (MID), rings at RING.
    mark       a MARK (variant letter or input pictogram) in the top-right MARK_BOX (cols 21..27,
               rows 4..11, drawn by mark()); the symbol moves down-left: rings at RING_M with one
               lead to the right on rows MID_M (19-20); two-terminal parts on MID_M.
               Two-character marks (Cu, Fe, JK, 10) are 13 px wide (cols 15..27) and only fit over
               a two-terminal part on MID_M whose top row is >= 15.
    top        a full-width element (heat waves, radiation arrows, feed arrow) on rows 4..10 above
               a symbol on MID_M (rows >= 14).

Standards: IEC 60617 for passives, sources, machines, earth, lamp; IEEE/ANSI distinctive shapes for
logic gates (each gate keeps its own silhouette; an IEC '&' box would read as the in-world chip).
"""
import math

from .canvas import MID, S, CORE, bezier, arc_points
from .palette import INK
from .font import FONT, text_width

__all__ = [
    # constants
    'LO', 'HI', 'MID_M', 'RING', 'RING_SMALL', 'RING_M', 'RING_M_SMALL', 'MARK_BOX',
    'IN_A', 'IN_B', 'IN_3', 'GATE_TOP', 'GATE_BOT', 'GATE_BACK', 'GATE_FRONT',
    'ARROW_HEADS', 'DOT', 'CONTACT', 'DIAG_ARROW', 'HEAT_WAVE', 'HEAT_CABLE', 'HEAT_TOP', 'MARKS', 'PICTOGRAMS',
    # wiring and marks
    'leads', 'junction', 'contact', 'terminal', 'dashed', 'cable_route', 'thermal_cable', 'arrow', 'diag_arrow',
    'arrows_in', 'arrows_out', 'feed_arrow', 'grains', 'heat_waves', 'adjust_arrow', 'preset_mark',
    'nonlinear_slash', 'mark', 'pictogram',
    # waveforms
    'sine', 'sawtooth', 'square', 'edge', 'hysteresis', 'response',
    # passives
    'resistor', 'fuse', 'capacitor', 'battery', 'inductor', 'diode', 'led', 'photodiode',
    # switching
    'switch', 'relay', 'button', 'breaker', 'coil', 'push_actuator',
    # rings, sources, machines
    'ring_leads', 'ring_center', 'lamp', 'source', 'ring_letter', 'meter', 'machine', 'summing',
    'transformer', 'antenna', 'transmitter', 'receiver', 'repeater', 'waves', 'ground', 'gear', 'bell', 'trace',
    # logic and analog
    'bubble', 'gate', 'opamp', 'matrix',
]

LO, HI = CORE                      # 4, 27: first / last core column and row
MID_M = 19                         # centre line (rows 19-20) of the 'mark' and 'top' layouts

RING = (7, 7, 18)                  # standard ring: cols/rows 7..24, centre 15.5; leads 4..6 and 25..27
RING_SMALL = (9, 9, 14)            # 'small' variant of an item (small_* bulbs ...): cols/rows 9..22
RING_M = (4, 12, 16)               # ring in the mark layout: cols 4..19, rows 12..27, centre (12, 20)
RING_M_SMALL = (6, 14, 12)         # small ring in the mark layout: cols 6..17, rows 14..25
MARK_BOX = (21, 4, 27, 11)         # inclusive box of a one-glyph mark / pictogram (7 wide, 8 tall)

IN_3 = (10, 15, 20)                # gate input rows, the standard (ELN gates have 3 inputs, LogicGate.kt):
                                   # 2px leads on rows 10-11, 15-16, 20-21; both rows of each outer lead
                                   # meet the curved OR/XOR back at the same column
IN_A, IN_B = 12, 18                # gate(inputs=2) input rows (2px each: 12-13, 18-19), pitch 6
GATE_TOP, GATE_BOT = 7, 24         # gate body rows (18 tall)
GATE_BACK, GATE_FRONT = 9, 19      # gate body cols; the output (bubble / lead) starts at 20

ARROW_HEADS = {                    # filled arrowheads (one of the few allowed filled marks)
    'down':  ('XXXXXXXX', '.XXXXXX.', '..XXXX..', '...XX...'),
    'up':    ('...XX...', '..XXXX..', '.XXXXXX.', 'XXXXXXXX'),
    'right': ('X...', 'XX..', 'XXX.', 'XXXX', 'XXXX', 'XXX.', 'XX..', 'X...'),
    'left':  ('...X', '..XX', '.XXX', 'XXXX', 'XXXX', '.XXX', '..XX', '...X'),
}
DOT = ('.XX.', 'XXXX', 'XXXX', '.XX.')   # 4x4 junction dot
CONTACT = ('.XXXX.', 'XXXXXX', 'XXXXXX', 'XXXXXX', 'XXXXXX', '.XXXX.')   # 6x6 switch contact dot
HEAT_WAVE = ('.XX.', 'XX..', 'XX..', '.XX.', '..XX', '..XX')   # one 6-row period, 4 cols wide
HEAT_CABLE = (9, 4)                # heat_waves() origin for the thermal cable: top-right, clear of the stack count
HEAT_TOP = (6, 4)                  # heat_waves() origin above a part on MID_M (furnace heater, dissipators)


def _freeze(d):
    from types import MappingProxyType
    return MappingProxyType(dict(d))


ARROW_HEADS = _freeze(ARROW_HEADS)


# ---- wiring -----------------------------------------------------------------------------------
def leads(g, xl, xr, y=MID, lead=INK, x0=LO, x1=HI):
    """Horizontal 2px leads: cols x0..xl on the left and xr..x1 on the right (rows y, y+1).
    Pass xl < x0 or xr > x1 to skip a side."""
    if xl >= x0:
        g.hline(x0, xl, y, lead)
    if xr <= x1:
        g.hline(xr, x1, y, lead)


def junction(g, x, y, c=INK):
    """4x4 rounded junction dot centred on the 2x2 wire cross-section whose top-left is (x, y).

    WARNING: on an orthogonal 2px wire cross (a 4-way node) every DOT pixel already lies inside the
    cross, and on a T it adds only 2 px, so a same-colour junction() is INVISIBLE there and the node
    reads as wires crossing WITHOUT a connection. For a visible node on 2px wires use contact()
    (6x6, STYLE 11: the hub). junction() is for a dot in another colour than the wires (matrix())."""
    g.bitmap(x - 1, y - 1, DOT, c)


def contact(g, x, y, c=INK):
    """6x6 filled contact dot centred on the 2x2 cross-section at (x, y) (switch contacts)."""
    g.bitmap(x - 2, y - 2, CONTACT, c)


def terminal(g, x, y, c=INK, d=6):
    """Open terminal (IEC): a ring of even diameter d centred on the 2x2 cross-section at (x, y).
    d=6 (cols x-2..x+3) has a 2x2 hole that the halo fills at GUI 2 (reads as a dark-centred dot);
    d=8 (cols x-3..x+4) keeps a 4x4 hole and reads as an open ring (electrical_fuse_holder)."""
    r = d // 2 - 1
    g.circ(x - r, y - r, d, c)


def dashed(g, x0, y0, x1, y1, c=INK, dash=2, gap=3):
    """Orthogonal dashed 2px line (mechanical link between a relay coil and its contact). One mark
    for the gap check; gap=3 leaves a clear pixel between the dashes' halos."""
    with g.group():
        if y0 == y1:
            x = min(x0, x1)
            while x <= max(x0, x1):
                g.hline(x, min(x + dash - 1, max(x0, x1)), y0, c)
                x += dash + gap
        else:
            y = min(y0, y1)
            while y <= max(y0, y1):
                g.vline(x0, y, min(y + dash - 1, max(y0, y1)), c)
                y += dash + gap


def cable_route(g, c):
    """THE placeable-cable glyph: a right-angle routed conductor (a schematic wiring step) across
    the core box, entirely in the cable's tier hue (THERMAL for thermal cables)."""
    g.vline(4, 4, 15, c)
    g.hline(4, 27, 14, c)
    g.vline(26, 14, 27, c)


def thermal_cable(g, c):
    """Thermal cable: cable_route in c (THERMAL) + INK heat_waves top-right of the step (HEAT_CABLE),
    above the route, where the stack count never covers them."""
    cable_route(g, c)
    heat_waves(g, *HEAT_CABLE, n=3, h=7)


# ---- arrows / marks ---------------------------------------------------------------------------
def arrow(g, x0, y0, x1, y1, c=INK):
    """Orthogonal flow arrow from (x0, y0) to the tip at (x1, y1); both are the top-left of the 2x2
    shaft cross-section. Head 8x4 (down/up) or 4x8 (left/right), shaft 2px."""
    if x0 == x1:
        d = 'down' if y1 > y0 else 'up'
        hy = y1 - 2 if d == 'down' else y1
        g.bitmap(x1 - 3, hy, ARROW_HEADS[d], c)
        if (d == 'down' and hy - 1 >= y0) or (d == 'up' and hy + 4 <= y0 + 1):
            g.vline(x0, y0, hy - 1 if d == 'down' else hy + 4, c)
    elif y0 == y1:
        d = 'right' if x1 > x0 else 'left'
        hx = x1 - 2 if d == 'right' else x1
        g.bitmap(hx, y1 - 3, ARROW_HEADS[d], c)
        if (d == 'right' and hx - 1 >= x0) or (d == 'left' and hx + 4 <= x0 + 1):
            g.hline(x0, hx - 1 if d == 'right' else hx + 4, y0, c)
    else:
        raise ValueError('arrow() is orthogonal only; use diag_arrow()')


DIAG_ARROW = ('.XXXXX', '..XXXX', '.XXXXX', 'XXX.XX', 'XX...X', 'X.....')   # 6x6, points up-right


def _diag_bm(sx, sy):
    bm = DIAG_ARROW
    if sx < 0:
        bm = [r[::-1] for r in bm]
    if sy > 0:
        bm = bm[::-1]
    return bm


def diag_arrow(g, x, y, sx=1, sy=-1, c=INK):
    """6x6 45-degree arrow in the box whose top-left is (x, y), pointing (sx, sy), each +-1;
    (1, -1) = up-right. Longer arrows: adjust_arrow()."""
    g.bitmap(x, y, _diag_bm(sx, sy), c)


def arrows_in(g, x=4, y=4, c=INK, pitch=9, sx=1, sy=1):
    """Two parallel 6x6 45-degree arrows pointing (sx, sy) (default down-right) INTO a body below:
    IEC 'incident radiation' (light sensors, photocells, receivers). First box top-left (x, y), the
    second `pitch` px to the right (9 keeps 3 empty px between them). Spans rows y..y+5: the body
    below must start at row >= y+9."""
    diag_arrow(g, x, y, sx, sy, c)
    diag_arrow(g, x + pitch, y, sx, sy, c)


def arrows_out(g, x=13, y=4, c=INK, pitch=9, sx=1, sy=-1):
    """Two parallel 6x6 45-degree arrows pointing (sx, sy) (default up-right) AWAY from a body
    below: emission (LED, lamp, transmitter). Same box convention as arrows_in()."""
    diag_arrow(g, x, y, sx, sy, c)
    diag_arrow(g, x + pitch, y, sx, sy, c)


def feed_arrow(g, c=INK, x=MID, bottom=6):
    """Standard process-machine input: INK arrow down from the top core edge (row 4) with its tip
    at row bottom+1. Default bottom=6: a bare 8x4 arrowhead on rows 4..7, cols x-3..x+4, so the
    process element can start at row 11 (3 empty rows). bottom=8: 2 rows of shaft, element at 13."""
    arrow(g, x, LO, x, bottom, c)


def grains(g, pts, c=INK):
    """2x2 grains (dust, grit, crushed output) at each (x, y): one mark for the gap check. Keep >= 2
    px between grains and >= 3 px between the grains and anything else."""
    with g.group():
        for x, y in pts:
            g.rect(x, y, x + 1, y + 1, c)


def heat_waves(g, x0, y0, n=3, h=7, pitch=7, c=INK):
    """The thermal cue: n vertical 2px 'S' waves (rising heat), each 4 cols wide, the first at cols
    x0..x0+3, rows y0..y0+h-1, `pitch` px apart (one mark). Named positions: HEAT_CABLE (thermal
    cable), HEAT_TOP (above a part on MID_M). Draw them in INK: in THERMAL grey a shape this busy
    turns into a dark smudge on the slot grey."""
    with g.group():
        for k in range(n):
            g.bitmap(x0 + k * pitch, y0, [HEAT_WAVE[j % len(HEAT_WAVE)] for j in range(h)], c)


def adjust_arrow(g, x0, y0, x1, y1, c=INK):
    """IEC 'adjustable' arrow drawn diagonally THROUGH a body (rheostat, VCA, VCO) from the tail
    (x0, y0) bottom-left to the head box ending at (x1, y1) top-right; keep |dx| == |dy| >= 8."""
    g.line(x0, y0, x1 - 5, y1 + 5, c)
    diag_arrow(g, x1 - 5, y1, 1, -1, c)


def preset_mark(g, x0, y0, x1, y1, c=INK):
    """IEC 'pre-set adjustment' (trimmer): a diagonal through the body ending in a short bar across
    its tip instead of an arrowhead. (x0, y0) bottom-left tail, (x1, y1) top-right tip."""
    g.line(x0, y0, x1, y1, c)
    g.line(x1 - 3, y1 - 3, x1 + 3, y1 + 3, c)


def nonlinear_slash(g, x0, y0, x1, y1, c=INK):
    """IEC 'inherent non-linear variability' mark (thermistor, varistor): a diagonal slash through
    the body with a short horizontal foot at the lower-left end."""
    g.line(x0, y0, x1, y1, c)
    g.hline(x0 - 4, x0, y0, c)


# ---- marks: the variant / input slot (top right) -------------------------------------------------
PICTOGRAMS = _freeze({
    # input pictograms for generators/sensors and lamp types. At most 7 wide, 8 tall; INK.
    'drop':   ('...X...', '..XXX..', '.XXXXX.', 'XXXXXXX', 'XXXXXXX', 'XXXXXXX', '.XXXXX.', '..XXX..'),
    'flame':  ('..X....', '..XX...', '.XXX.X.', '.XXXXX.', 'XXX.XXX', 'XX...XX', 'XXX.XXX', '.XXXXX.'),
    'water':  ('.XX....', 'XXXX..X', 'X..XXXX', '....XX.', '.XX....', 'XXXX..X', 'X..XXXX', '....XX.'),
    'wind':   ('XXXXX..', 'XXXXXX.', '....XX.', '.......', 'XXXXXXX', 'XXXXXXX', '.......', 'XXXX...'),
    'steam':  ('.XX..XX', 'XX..XX.', 'XX..XX.', '.XX..XX', '..XX..X', '..XX..X', '.XX..XX', 'XX..XX.'),
    'sun':    ('X..X..X', '.XXXXX.', '.XXXXX.', 'XXXXXXX', '.XXXXX.', '.XXXXX.', 'X..X..X', '.......'),
    'cloud':  ('..XXX..', '.XXXXX.', 'XXXXXXX', 'XXXXXXX', '.......', 'X.X.X..', '.X.X.X.', '.......'),
    'person': ('..XXX..', '..XXX..', '.......', 'XXXXXXX', '..XXX..', '..XXX..', '.XX.XX.', '.XX.XX.'),
    'bolt':   ('...XXXX', '..XXXX.', '.XXXX..', 'XXXXXXX', '..XXXX.', '.XXXX..', 'XXX....', 'XX.....'),
    'light':  ('.XXXXX.', '..XXXX.', '.XXXXX.', 'XXX.XX.', 'XX...X.', 'X......', '.......', '.......'),
    'radio':  ('X...X..', 'XX..XX.', '.XX..XX', '.XX..XX', '.XX..XX', '.XX..XX', 'XX..XX.', 'X...X..'),
    'clock':  ('.XXXXX.', 'XX.X.XX', 'X..X..X', 'X..XX.X', 'X.....X', 'XX...XX', '.XXXXX.', '.......'),
})

MARKS = _freeze({
    # variant marks -> FONT strings (one or two glyphs) or a pictogram name. STYLE.md section 8 is the
    # binding table of which item gets which mark.
    'carbon': 'C', 'economic': 'E', 'emergency': '!', 'farming': 'F', 'fluorescent': 'E',
    'copper': 'Cu', 'iron': 'Fe', 'tungsten': 'W',
    'cost': '$', 'capacity': 'C', 'current': 'A', 'life': 'L', 'voltage': 'V', 'single_use': '1',
    'fuel': 'drop', 'gas': 'flame', 'steam': 'steam', 'water': 'water', 'wind': 'wind',
    'daylight': 'sun', 'weather': 'cloud', 'entity': 'person', 'fire': 'flame', 'charge': 'bolt',
    'timer': 'clock', 'led': 'light', 'advanced': '+', 'large': 'L', 'socket_a': 'A', 'socket_b': 'B',
    'robust': 'R', 'wireless': 'radio', 'one': '1', 'ten': '10', 'd': 'D', 'jk': 'JK', 'nuclear': '!',
    '10': '10', '25': '25',        # current ratings (10a_diode, 25a_diode); two glyphs: only over MID_M parts
    'B': 'B',                      # block form of a component (power_capacitor_block, power_inductor_block)
    'temperature': 't°',           # IEC '-t°' temperature dependence (thermistor); two glyphs, cols 15..27
})


def pictogram(g, name, x=None, y=4, c=INK):
    """Stamp PICTOGRAMS[name] with its top-left at (x, y); default: right-aligned in MARK_BOX."""
    bm = PICTOGRAMS[name]
    if x is None:
        x = MARK_BOX[2] + 1 - len(bm[0])
    g.bitmap(x, y, bm, c)


def mark(g, key, c=INK):
    """Draw the variant / input mark for `key` (a MARKS key, a PICTOGRAMS name, or a literal FONT
    string of one or two glyphs) right-aligned at col 27, top row 4. Returns its bbox.

    One glyph fits MARK_BOX (cols 21..27, rows 4..11) and goes with RING_M rings or anything else
    that keeps 3 px clear of the box. Two glyphs (Cu, Fe, JK, 10) span cols 15..27, rows 4..10, and
    only fit over a two-terminal part on MID_M (top row >= 15). The gap check enforces both."""
    v = MARKS.get(key, key)
    if v in PICTOGRAMS:
        bm = PICTOGRAMS[v]
        x = HI + 1 - len(bm[0])
        pictogram(g, v, x, 4, c)
        return (x, 4, HI, 4 + len(bm) - 1)
    w = text_width(v)
    x = HI + 1 - w
    g.text(x, 4, v, c)
    return (x, 4, HI, 10)


# ---- waveforms ---------------------------------------------------------------------------------
def sine(g, x0, x1, cy=16.0, amp=3.0, periods=1.0, c=INK):
    """2px sine wave from col x0 to x1 around the pixel-corner line cy (16.0 = centre rows 15/16).
    AC sources, oscillators."""
    n = 64
    pts = [(x0 + 1 + (x1 - x0 - 1) * i / n,
            cy - amp * math.sin(2 * math.pi * periods * i / n)) for i in range(n + 1)]
    g.stroke(pts, c)


def sawtooth(g, x0, x1, top, bot, teeth=2, c=INK):
    """Sawtooth: `teeth` slow ramps from row bot up to row top, each followed by a vertical drop.
    Cols x0..x1 (the drops are 2px verticals)."""
    w = (x1 - x0 + 1) // teeth
    for k in range(teeth):
        xa = x0 + k * w
        xb = xa + w - 2
        g.line(xa, bot, xb, top, c)
        g.vline(xb, top, bot + 1, c)


def square(g, x0, x1, top, bot, periods=1.5, c=INK):
    """Square wave (2px) between rows top and bot, starting low at col x0; half-period = width/2/periods."""
    half = max(4, int(round((x1 - x0 + 1) / (2 * periods))))
    x1 -= 1                                   # wire points are the top-left of the 2x2 stroke
    x, high = x0, False
    pts = [(x, bot)]
    while x < x1:
        nx = min(x1, x + half)
        y = top if high else bot
        pts.append((nx, y))
        if nx < x1:
            pts.append((nx, bot if high else top))
        x, high = nx, not high
    g.wire(pts, c)


def edge(g, x0, x1, top, bot, c=INK):
    """Rising clock edge (flip-flops): low from x0 to the middle, a vertical rise, high to x1 (cols
    x0..x1, rows top..bot+1). The flip-flop type goes in the mark (D, JK)."""
    xm = (x0 + x1) // 2
    g.wire([(x0, bot), (xm, bot), (xm, top), (x1 - 1, top)], c)


def hysteresis(g, x0, y0, w=16, h=12, c=INK):
    """Hysteresis loop (Schmitt trigger, on/off regulator): bottom line from x0 to the right
    vertical, top line from the left vertical to x0+w-1, verticals 6 px apart. Box (x0, y0) w x h."""
    xl = x0 + w // 2 - 4
    xr = xl + 6
    yt, yb = y0, y0 + h - 2
    g.hline(x0, xr + 1, yb, c)
    g.hline(xl, x0 + w - 1, yt, c)
    g.vline(xl, yt, yb + 1, c)
    g.vline(xr, yt, yb + 1, c)


def response(g, kind='lowpass', c=INK, axes=INK, x0=6, y0=5, x1=27, y1=25):
    """Frequency-response pictogram (Bode magnitude) for filters: INK L-shaped axes and the curve in
    c. kind: 'lowpass' (flat, then falling) or 'highpass' (rising, then flat)."""
    g.vline(x0 - 2, y0, y1, axes)
    g.hline(x0 - 2, x1, y1, axes)
    yf, yl = y0 + 3, y1 - 5
    xk = x0 + (x1 - x0) // 2 - 1
    if kind == 'lowpass':
        g.hline(x0 + 3, xk, yf, c)
        g.line(xk, yf, x1 - 3, yl, c)
    elif kind == 'highpass':
        g.line(x0 + 3, yl, xk, yf, c)
        g.hline(xk, x1 - 3, yf, c)
    else:
        raise ValueError(kind)


# ---- passives ----------------------------------------------------------------------------------
def resistor(g, c=INK, lead=INK, with_leads=True, x0=8, x1=23, h=10, y=MID, small=False):
    """IEC resistor: empty 2px rectangle (default 16x10, cols 8..23, centred on rows y, y+1) with
    leads. small=True: 12x8 (cols 10..21). Heating corps, power resistors, rheostats
    (+ adjust_arrow), trimmers (+ preset_mark), thermistors (+ nonlinear_slash)."""
    if small:
        x0, x1, h = x0 + 2, x1 - 2, 8
    y0 = y + 1 - h // 2
    g.frame(x0, y0, x1, y0 + h - 1, c)
    if with_leads:
        leads(g, x0 - 1, x1 + 1, y, lead=lead)


def fuse(g, c=INK, lead=INK, with_leads=True, x0=8, x1=23, h=12, y=MID, blown=False):
    """IEC fuse: rectangle (default 16x12) with the conductor passing through it. blown=True cuts
    the element in the middle (blown_lead_fuse); the cut is 4 px so the halves stay separate."""
    y0 = y + 1 - h // 2
    g.frame(x0, y0, x1, y0 + h - 1, c)
    g.hline(x0 + 2, x1 - 2, y, c)
    if blown:
        xm = (x0 + x1) // 2
        g.erase(xm - 2, y, xm + 3, y + 1)
    if with_leads:
        leads(g, x0 - 1, x1 + 1, y, lead=lead)


def capacitor(g, c=INK, lead=INK, with_leads=True, x=13, gap=6, h=18, y=MID, polar=False, cells=1):
    """IEC capacitor: two 2px plates `gap` px apart (default 18 tall: plates at cols 13-14 and
    21-22) with leads. The plates sit right of centre so the polar '+' (polar=True, INK, cols 4..9,
    rows 4..9) keeps 3 px clear of the plate. cells=2: two capacitors in series (a pack), centred."""
    y0 = y + 1 - h // 2
    if cells == 2:
        xs = [6, 6 + gap + 2, 18 - 1, 18 + gap + 1]
        for xp in xs:
            g.vline(xp, y0, y0 + h - 1, c)
        g.hline(xs[1] + 2, xs[2] - 1, y, lead)
        if with_leads:
            leads(g, xs[0] - 1, xs[3] + 2, y, lead=lead)
    else:
        xa, xb = x, x + gap + 2
        g.vline(xa, y0, y0 + h - 1, c)
        g.vline(xb, y0, y0 + h - 1, c)
        if with_leads:
            leads(g, xa - 1, xb + 2, y, lead=lead)
    if polar:
        g.bitmap(LO, LO, FONT['+'], lead)


def battery(g, c=INK, lead=INK, with_leads=True, cells=1, x=None, y=MID, small=False):
    """IEC cell / battery: long plate (+) and short plate (-) 4 px apart; cells=2 is a battery of
    two cells (3 px between cells). No '+' sign: in IEC the long plate is the positive one.
    Plates 18/10 tall on MID, 16/10 on MID_M (mark layout: pass x=8 so the right lead runs under
    the mark), 12/8 with small=True. x = column of the first long plate (default: centred)."""
    hl, hs = (12, 8) if small else ((18, 10) if y <= MID else (16, 10))
    w = cells * 8 + (cells - 1) * 3
    if x is None:
        x = 16 - w // 2
    xs = x
    for _ in range(cells):
        g.vline(xs, y + 1 - hl // 2, y + hl // 2, c)
        g.vline(xs + 6, y + 1 - hs // 2, y + hs // 2, c)
        xs += 11
    if with_leads:
        leads(g, x - 1, x + w, y, lead=lead)


def inductor(g, c=INK, lead=INK, with_leads=True, n=3, pitch=6, height=5.5, y=MID):
    """Inductor / coil: n tall semicircular humps (2px) standing on the centre line, adjacent humps
    sharing their foot. Default 3 humps, 6px pitch -> cols 7..24, rows 10..16."""
    a = 16 - n * pitch / 2                    # pixel-corner x of the first foot
    base = y + 1.0
    for i in range(n):
        x = a + i * pitch
        pts = [(x + pitch / 2 - pitch / 2 * math.cos(math.pi * k / 32), base - height * math.sin(math.pi * k / 32))
               for k in range(33)]
        g.stroke(pts, c)
    if with_leads:
        leads(g, int(a) - 2, int(a + n * pitch) + 1, y, lead=lead)


def diode(g, c=INK, lead=INK, with_leads=True, x0=10, w=10, h=12, y=MID):
    """IEC/IEEE diode: outlined triangle pointing right (back at col x0, h tall, centred on rows y,
    y+1) + 2px bar at the apex; leads."""
    top = y + 1 - h // 2
    ax = x0 + w
    s = 2 * y + 1
    g.vline(x0, top, y, c)
    g.line(x0, top, ax - 1, y, c)
    g.mirror_y(s=s, only=lambda x, yy: x0 <= x < ax and top <= yy <= y)
    g.vline(ax, top, top + h - 1, c)
    if with_leads:
        leads(g, x0 - 1, ax + 2, y, lead=lead)


def led(g, c=INK, lead=INK, with_leads=True, arrows=INK):
    """LED: a 10-tall diode on MID_M (rows 15..24) with arrows_out() above it (rows 4..9, cols
    13..27). LED bulbs, LED indicators."""
    diode(g, c, lead, with_leads, x0=8, w=10, h=10, y=MID_M)
    arrows_out(g, 13, 4, arrows)


def photodiode(g, c=INK, lead=INK, with_leads=True, arrows=INK):
    """Photodiode / photocell: a 10-tall diode on MID_M with arrows_in() above it pointing down
    onto it (rows 4..9, cols 4..18). Light sensor, solar panels."""
    diode(g, c, lead, with_leads, x0=12, w=10, h=10, y=MID_M)
    arrows_in(g, 4, 4, arrows)


# ---- switching -----------------------------------------------------------------------------------
def switch(g, c=INK, lead=None, with_leads=True, y=18, pivot=9, tip=(21, 4), x1=21, terminals=True,
           mark=False):
    """IEC make contact (open), drawn heavy enough to carry the tier colour at GUI 2. The WHOLE
    contact is the defining element, all in c: left lead to the pivot terminal (col `pivot`), the
    blade rising to `tip`, the fixed contact at col x1+2 and the right lead (pass lead=INK only for
    an untiered part). terminals=True draws both contacts as 6x6 contact dots (contact()).
    Default: leads on rows 18-19, blade up to row 4 (rows 4..21 overall).
    mark=True: the mark-layout contact (rows 12..22, clear of MARK_BOX) for timers, wireless switches.
    Switches, relays, breakers and buttons all use it."""
    if mark:
        y, pivot, tip, x1 = MID_M, 8, (16, 12), 20
    lead = c if lead is None else lead
    if with_leads:
        g.hline(LO, pivot, y, lead)
        g.hline(x1, HI, y, lead)
    g.line(pivot, y, tip[0], tip[1], c)
    if terminals:
        contact(g, pivot, y, c)
        contact(g, x1 + 2, y, c)


def coil(g, x0, y0, x1, y1, c=INK):
    """Relay coil (IEC): a 2px rectangle. relay() pairs it with a dashed link to the contact."""
    g.frame(x0, y0, x1, y1, c)


def relay(g, c=INK, coil_c=INK):
    """Relay: switch() (in c, leads on rows 13-14) with an INK coil (cols 10..19, rows 21..27: a 6x3
    hole, so the halo does not fill it) under it and a dashed mechanical link from the coil up towards
    the blade (rows 12..17). Every part keeps the 3 px gap."""
    switch(g, c, y=13, pivot=8, tip=(20, 4), x1=21)
    coil(g, 10, 21, 19, 27, coil_c)
    dashed(g, 14, 12, 14, 17, coil_c, dash=2, gap=3)


def button(g, c=INK, cap_c=INK):
    """Push button: switch() (in c, leads on rows 18-19) with an INK push actuator (stem from the blade
    up to a cap on row 4)."""
    switch(g, c, y=18, pivot=8, tip=(18, 10), x1=21)
    push_actuator(g, 14, 4, 13, cap_c)


def breaker(g, c=INK):
    """Circuit breaker (IEC: make contact with the breaker cross ON the fixed contact), all in c (the
    whole contact is the defining element). Leads on rows 17-18; moving contact dot cols 6..11; blade
    open up to (14, 6); the cross (cols 19..27, rows 13..21) REPLACES the fixed-contact dot, which
    would fuse with it into a blob at GUI 2; the right lead leaves the cross."""
    y = MID + 2
    g.hline(LO, 8, y, c)
    g.line(8, y, 14, 6, c)
    contact(g, 8, y, c)
    g.stroke([(19.0, 13.0), (26.0, 20.0)], c)
    g.stroke([(26.0, 13.0), (19.0, 20.0)], c)
    g.hline(25, HI, y, c)


def push_actuator(g, x, y_top, y_bot, c=INK):
    """Push-button actuator: a 2px stem from y_bot (touching the blade) up to a T-bar cap at y_top."""
    g.vline(x, y_top, y_bot, c)
    g.hline(x - 3, x + 4, y_top, c)


# ---- rings: sources, loads, machines -------------------------------------------------------------
def ring_center(ring=RING):
    """Pixel-corner centre (cx, cy) of a ring (x0, y0, D)."""
    x0, y0, D = ring
    return x0 + D / 2, y0 + D / 2


def ring_leads(g, lead=INK, ring=RING):
    """Horizontal leads from the core edge to the ring on its centre line (skips a side with no room)."""
    x0, y0, D = ring
    y = y0 + D // 2 - 1
    leads(g, x0 - 1, x0 + D, y, lead=lead)


def lamp(g, c=INK, lead=INK, with_leads=True, ring=RING):
    """IEC lamp: ring with an X inside, in c; 50V/200V bulbs (tiered) and lamp sockets (INK).
    ring: RING (default), RING_SMALL (small_* bulbs), RING_M / RING_M_SMALL (with a mark)."""
    x0, y0, D = ring
    cx, cy = ring_center(ring)
    g.circ(x0, y0, D, c)
    a = int(round((D / 2 - 2) * 0.72))
    xi, yi = int(cx), int(cy)
    g.line(xi - a, yi - a, xi + a - 1, yi + a - 1, c)
    g.mirror_x(s=2 * xi - 1, only=lambda x, y: x0 + 2 <= x <= x0 + D - 3 and y0 + 2 <= y <= y0 + D - 3)
    if with_leads:
        ring_leads(g, lead, ring)


def source(g, kind='V', c=INK, lead=INK, with_leads=True, ring=RING):
    """Source in a ring. kind='V': IEC voltage source (conductor drawn through the ring);
    'I': IEC current source (bar across the ring, perpendicular to the conductor);
    'AC': sine inside (AC / signal source)."""
    x0, y0, D = ring
    cx, cy = ring_center(ring)
    y = int(cy) - 1
    g.circ(x0, y0, D, c)
    if kind == 'V':
        g.hline(x0 + 2, x0 + D - 3, y, c)
    elif kind == 'I':
        g.vline(int(cx) - 1, y0 + 2, y0 + D - 3, c)
    elif kind == 'AC':
        sine(g, x0 + 3, x0 + D - 4, cy, D / 7, 1, c)
    else:
        raise ValueError(kind)
    if with_leads:
        ring_leads(g, lead, ring)


def ring_letter(g, letter, ring_c=INK, letter_c=INK, lead=INK, with_leads=True, ring=RING):
    """Ring with a centred FONT label (one glyph, or a key such as 'V8') and optional leads.
    Building block of meter() and machine(). RING (D=18) takes any glyph incl. the 8-wide M/W/V8;
    D=16 rings (RING_M) take only 6-wide glyphs (G, A, V, T, n ...)."""
    x0, y0, D = ring
    cx, cy = ring_center(ring)
    g.circ(x0, y0, D, ring_c)
    if letter in FONT:
        bm = FONT[letter]
        w = len(bm[0])
        g.bitmap(int(cx) - w // 2, int(cy) - 3, bm, letter_c)
    else:
        w = text_width(letter)
        g.text(int(cx) - w // 2, int(cy) - 3, letter, letter_c)
    if with_leads:
        ring_leads(g, lead, ring)


def meter(g, letter, c=INK, lead=INK, with_leads=True, ring=RING):
    """In-circuit meter / probe: ring in c (the defining element, so it carries the tier hue) with
    the measured quantity's INK letter (V, A, W, T, n ...), horizontal leads."""
    ring_letter(g, letter, c, INK, lead, with_leads, ring)


def machine(g, letter='M', c=INK, lead=INK, with_leads=True, ring=RING):
    """Rotating machine (IEC): ring in c (the defining element) with an INK letter: M motor,
    G generator / turbine / dynamo."""
    ring_letter(g, letter, c, INK, lead, with_leads, ring)


def summing(g, c=INK, lead=INK, ring=RING):
    """Summing point: ring in c with an INK '+' (configurable_summing_unit). Inputs left and top,
    output right."""
    x0, y0, D = ring
    cx, cy = ring_center(ring)
    g.circ(x0, y0, D, c)
    g.bitmap(int(cx) - 3, int(cy) - 3, FONT['+'], lead)
    ring_leads(g, lead, ring)
    g.vline(int(cx) - 1, LO, y0 - 1, lead)


def transformer(g, c=INK, c2=None, x0=4, y0=8):
    """IEC single-line transformer: two overlapping 16px rings, offset 8 (default cols 4..19 and
    12..27, rows 8..23, centred). (x0, y0) = top-left of the first ring: y0=12 puts it at rows 12..27
    under something on top (utility_pole_w_dc_dc_converter). c2 colours the second winding
    (defaults to c)."""
    g.circ(x0, y0, 16, c)
    g.circ(x0 + 8, y0, 16, c if c2 is None else c2)


def antenna(g, c=INK, cx=9, top=4, bot=27, arm=5):
    """IEC antenna: mast at cols cx..cx+1 from row top to bot, with a 'V' of two 2px arms opening
    upward from row top + arm. Use transmitter(), receiver() or repeater() rather than placing
    the arcs/arrows yourself."""
    g.vline(cx, top + 2, bot, c)
    g.line(cx - arm, top, cx, top + arm, c)
    g.mirror_x(s=2 * cx + 1, only=lambda x, y: x < cx and y <= top + arm + 1)


def transmitter(g, c=INK):
    """Transmitting antenna: antenna() in c (mast cols 9-10) + two INK radiation arcs to its right."""
    antenna(g, c)
    waves(g, 11, 16)


def receiver(g, c=INK):
    """Receiving antenna: antenna() in c + two parallel INK arrows pointing in (down-left) at it."""
    antenna(g, c)
    diag_arrow(g, 19, 7, -1, 1)
    diag_arrow(g, 19, 16, -1, 1)


def repeater(g, c=INK):
    """Repeater (receives and re-transmits): antenna() in c + one INK arrow in (top) and one out (bottom)."""
    antenna(g, c)
    diag_arrow(g, 19, 7, -1, 1)
    diag_arrow(g, 19, 16, 1, -1)


def waves(g, cx, cy, radii=(7, 12), side='right', span=40, c=INK):
    """Radiation arcs around the pixel-corner point (cx, cy): 2px arcs of the given radii, each
    +-span degrees about `side` ('right', 'left', 'up', 'down'). One mark. Transmitters:
    transmitter() = antenna(g, c) + waves(g, 11, 16)."""
    ang = {'right': 0, 'down': 90, 'left': 180, 'up': 270}[side]
    with g.group():
        for r in radii:
            def keep(dx, dy, ang=ang):
                a = math.degrees(math.atan2(dy, dx))
                return abs((a - ang + 180) % 360 - 180) <= span
            g.circ(int(cx - r), int(cy - r), 2 * r, c, keep=keep)


def ground(g, c=INK, cx=MID, top=4):
    """IEC earth: stem from row top down to three bars of 20, 14 and 8 px, 2 px apart (one mark: the
    bars are meant to read as a stack)."""
    with g.group():
        g.vline(cx, top, top + 11, c)
        g.hline(cx - 9, cx + 10, top + 12, c)
        g.hline(cx - 6, cx + 7, top + 16, c)
        g.hline(cx - 3, cx + 4, top + 20, c)


def gear(g, x0, y0, c, outer):
    """Toothed roller: 8px ring (bbox x0..x0+7, y0..y0+7) with five 2x2 teeth (N, S, outer side,
    two outer diagonals); the inner side stays smooth so two rollers keep a clear gap.
    outer = -1 (teeth on the left) or +1 (on the right). Macerators, crushers."""
    g.circ(x0, y0, 8, c)
    xo = x0 - 2 if outer < 0 else x0 + 8
    xd = x0 - 1 if outer < 0 else x0 + 7
    for tx, ty in ((x0 + 3, y0 - 2), (x0 + 3, y0 + 8), (xo, y0 + 3), (xd, y0 - 1), (xd, y0 + 7)):
        g.rect(tx, ty, tx + 1, ty + 1, c)


def bell(g, c=INK, lead=INK, y=MID_M):
    """IEC bell (alarms, buzzers): a half ring (dome) standing on a 2px bar, with leads down to the
    core edge. Dome cols 8..23, rows y-8..y; bar rows y+1..y+2."""
    g.circ(8, y - 8, 16, c, keep=lambda dx, dy: dy < 0.5)
    g.hline(6, 25, y, c)
    g.vline(11, y + 2, HI, lead)
    g.vline(20, y + 2, HI, lead)


def trace(g, c=INK, ring=RING, trace_c=INK):
    """Recorder / data logger: a ring in c with an INK recorded trace (a rising zig-zag) inside."""
    x0, y0, D = ring
    cx, cy = ring_center(ring)
    g.circ(x0, y0, D, c)
    xi, yi = int(cx), int(cy)
    g.stroke([(xi - 5.5, yi + 3.0), (xi - 2.5, yi - 1.0), (xi + 0.5, yi + 2.0), (xi + 5.0, yi - 4.0)], trace_c)
    ring_leads(g, INK, ring)


# ---- logic & analog -------------------------------------------------------------------------------
def _arc_quarter(r):
    pts = []
    for dy in range(-r, 0):
        for dx in range(0, r):
            d = math.hypot(dx + 0.5, dy + 0.5)
            if r - 2 < d <= r + 0.1:
                pts.append((dx, dy))
    return pts


def bubble(g, x0, c=INK, y=MID):
    """Inversion bubble: 6px ring at cols x0..x0+5 centred on rows y, y+1."""
    g.circ(x0, y - 2, 6, c)


def gate(g, kind, c=INK, lead=INK, invert=False, with_leads=True, inputs=3):
    """IEEE distinctive-shape gate in the standard gate box (body cols 9..19, rows 7..24).
    kind: 'and', 'or', 'xor', 'buf' (triangle; with invert=True it is NOT).
    invert=True adds the bubble at cols 20..25 (NAND, NOR, XNOR, NOT).
    inputs=3 (the standard: ELN gates have 3 inputs): leads on rows 10-11, 15-16, 20-21 (IN_3);
    inputs=2: rows 12-13 and 18-19 (IN_A, IN_B). 'buf' always has one input on the centre line.
    Each input lead runs from the core edge to the first body pixel; output on the centre line.
    XOR's extra back curve keeps 3 empty px from the body so it reads as its own stroke."""
    b, f = GATE_BACK, GATE_FRONT
    if inputs not in (2, 3):
        raise ValueError(f'gate inputs={inputs!r}: 2 or 3')
    inputs = IN_3 if inputs == 3 else (IN_A, IN_B)
    if kind == 'and':
        r = 9
        cx = f + 1 - r              # arc centre (pixel corner) x
        g.vline(b, GATE_TOP, GATE_BOT, c)
        g.hline(b, cx - 1, GATE_TOP, c)
        g.hline(b, cx - 1, GATE_BOT - 1, c)
        for dx, dy in _arc_quarter(r):
            g.put(cx + dx, 16 + dy, c)
            g.put(cx + dx, 15 - dy, c)
    elif kind in ('or', 'xor'):
        top = GATE_TOP + 1.0        # stroke centre line of the top edge
        g.stroke(bezier((b, top), (b + 6, top), (f + 1, 16.0)), c)
        back = bezier((b + 0.5, top), (b + 5.0, 12.0), (b + 5.0, 16.0))
        g.stroke(back, c)
        if kind == 'xor':                     # extra back curve: steeper than the body's, 3+ px clear of it
            g.stroke(bezier((6.0, GATE_TOP + 0.0), (8.0, 12.0), (8.0, 16.0)), c)
        g.mirror_y(only=lambda x, y: y <= MID)
    elif kind == 'buf':
        g.vline(b, GATE_TOP, GATE_BOT, c)
        g.line(b, GATE_TOP, f - 1, MID, c)
        g.mirror_y(only=lambda x, y: b <= x <= f and y <= MID)
        inputs = (MID,)
    else:
        raise ValueError(kind)
    out = f + 1
    if invert:
        bubble(g, out, c)
        out += 6
    if with_leads:
        for y in inputs:                      # each input runs from the core edge to the first body pixel
            xe = next((x for x in range(LO, f) if g.get(x, y) or g.get(x, y + 1)), b)
            if xe - 1 >= LO + 1:
                g.hline(LO, xe - 1, y, lead)
        if out <= HI - 1:
            g.hline(out, HI, MID, lead)


def opamp(g, c=INK, lead=INK, with_leads=True, signs=None, inputs=2, control=False):
    """Op-amp / amplifier / comparator: outlined triangle in c (back col 7, rows 4..27, apex col 24),
    output lead on the centre line.
    inputs=2 (opamp): inverting input on rows 11-12 with the INK '-' (cols 10..14) level with it,
             non-inverting input on rows 17-18 with the INK '+' (cols 10..15, rows 15..20) centred on
             it; both signs clear of the slopes. signs=False drops the signs.
    inputs=1 (amplifier, VCA): one input on the centre line, no signs.
    control=True (VCA): the control input as an INK lead (cols 19-20) from the top core edge down onto
             the upper slope. No arrowhead: it would fuse with the slope (the VCOs' arrow, oscillator(),
             has room for one)."""
    if inputs not in (1, 2):
        raise ValueError(f'opamp inputs={inputs!r}: 1 or 2')
    if signs is None:
        signs = inputs == 2
    if signs and inputs != 2:
        raise ValueError('opamp signs need inputs=2')
    g.vline(7, 4, 27, c)
    g.line(7, 4, 23, MID, c)
    g.mirror_y(only=lambda x, y: 7 <= x <= 24 and y <= MID)
    if with_leads:
        for y in ((11, 17) if inputs == 2 else (MID,)):
            g.hline(LO, 6, y, lead)
        g.hline(25, HI, MID, lead)
    if signs:
        g.hline(10, 14, 11, lead)
        g.bitmap(10, 15, FONT['+'], lead)
    if control:
        g.vline(19, LO, 11, lead)


def matrix(g, c=INK, dots=((0, 0), (1, 1), (0, 2), (2, 1))):
    """Programmable logic array (PAL): three vertical and three horizontal 2px lines in c forming a
    grid (cols 8/15/22, rows 7/15/23) with INK junction dots at the programmed crossings
    `dots` ((col index, row index), ...)."""
    xs, ys = (8, 15, 22), (7, 15, 23)
    for x in xs:
        g.vline(x, 5, 26, c)
    for y in ys:
        g.hline(5, 26, y, c)
    for i, j in dots:
        junction(g, xs[i], ys[j], INK)
