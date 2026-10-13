"""digital_watch: battery-run digital wall clock -> the analog_watch clock face at RING_M + mark D (digital),
the set's variant mechanism (STYLE 8). Untiered, all INK, no leads.

PROVISIONAL: 'D' is a literal FONT mark that no STYLE table sanctions yet (STYLE 8: do not invent marks).
A kit request asks for a registered MARKS['digital'] = 'D' and a STYLE 8.6 row for the watches; once it
lands, change the call to mark(g, 'digital'). It draws the same glyph as the 'd' mark of d_flip_flop_chip,
but over a clock face, never over a clock-edge symbol; if the owner rejects D, another registered mark
letter is the fallback (a FONT time label such as '12' does not fit inside the ring)."""
from kit import *
from families.meters import clock_face

TIER = None


def draw():
    g = Glyph(tier=TIER)
    mark(g, 'D')
    clock_face(g, ring=RING_M)
    return g
