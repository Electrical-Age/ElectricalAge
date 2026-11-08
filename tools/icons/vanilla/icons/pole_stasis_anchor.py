"""pole_stasis_anchor - stasis anchor: a pale-blue rift crystal standing in a small dark-steel
socket, two thin crystal-blue rings floating round it.

RIFT is a new material ramp (derive_ramp from #78b4ff at index 4, hand-tuned; report for
promotion if the family grows).  The crystal is a faceted gem seen front-on: lit left facet
RIFT[4], centre RIFT[3], shaded right facet RIFT[2], RIFT[5] glint, outline RIFT[1]/[0].  The
rings are hand-placed 1 px ellipses (no outline, STYLE 4): the back arc (RIFT[3]) is hidden behind
the crystal, the front arc (RIFT[5]) passes in front of it.  The socket is a DARK (RUBBER_BLACK)
oblique box.  Untiered."""
from kit import Canvas, box, outline_region, RUBBER_BLACK, h

RIFT = (h('#1e2647'), h('#324380'), h('#3f6dc4'), h('#5a96ea'), h('#78b4ff'), h('#cfe4ff'))
DARK = RUBBER_BLACK

#  C = crystal, b = ring back arc, f = ring front arc (drawn over the crystal)
GRID = [
    "................",
    ".......CC.......",
    "...bbbCCCCbbb...",
    "..b..CCCCCC..b..",
    "...ffCCCCCCff...",
    ".....CCCCCC.....",
    "..bbbCCCCCCbbb..",
    ".b...CCCCCC...b.",
    "b....CCCCCC....b",
    ".ff..CCCCCC..ff.",
    "...ffCCCCCCff...",
    ".....ffffff.....",
    ".......CC.......",
]
#: front-arc pixels that lie over the crystal (the crystal continues under them)
UNDER = {(x, 4) for x in range(5, 11)} | {(x, 11) for x in range(6, 10)}


def _cells(ch):
    return {(x, y) for y, row in enumerate(GRID) for x, c in enumerate(row) if c == ch}


def draw():
    cv = Canvas(16)
    back, front = _cells('b'), _cells('f') | UNDER
    crystal = _cells('C') | UNDER
    for p in back:
        cv.set(*p, RIFT[3])
    # the socket: dark box cols 4..11, rows 13..14, depth 1
    box(cv, 4, 13, 8, 2, 1, DARK, bevel=False)
    for (x, y) in crystal:
        row = [xx for (xx, yy) in crystal if yy == y]
        a, b = min(row), max(row)
        cv.set(x, y, RIFT[4] if x <= a + (1 if b - a >= 5 else 0) else (RIFT[2] if x == b or (b - a >= 5 and x == b - 1) else RIFT[3]))
    outline_region(cv, crystal, RIFT[0], RIFT[1])
    cv.pixels([(6, 6), (6, 7), (7, 2)], RIFT[5])
    for p in front:
        cv.set(*p, RIFT[5])
    return cv
