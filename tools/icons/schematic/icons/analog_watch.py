"""analog_watch: battery-run analog wall clock -> a clock face: INK ring with two hands, no leads (it has no
electrical connection) (families.meters.clock_face). Untiered, all INK. Full-size ring, not the 'clock'
pictogram, which is electrical_timer's (signal) mark (STYLE 5.4)."""
from kit import *
from families.meters import clock_face

TIER = None


def draw():
    g = Glyph(tier=TIER)
    clock_face(g)
    return g
