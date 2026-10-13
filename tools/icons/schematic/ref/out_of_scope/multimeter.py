"""multimeter: hand instrument. NOTE: a tool, so OUT of schematic scope under contract decision 2,
so this module lives in ref/out_of_scope/ (build.py never builds it; nothing here ships).
Render: python3 -c "import sys; sys.path[:0]=['.','ref/out_of_scope']; import multimeter; multimeter.draw().render().save('ref/out_of_scope/multimeter.png')"

kept as the reference for the hand-instrument convention in case the owner brings tools in.
Convention: INK meter ring with the quantity letter, two probe leads leaving the bottom of the
ring at 45 degrees and ending in 3px probe handles with tapered pins (an in-circuit meter instead
has horizontal leads: meter())."""
from kit import Glyph, FONT

TIER = None

PROBE = ['......XX',      # left probe, mirrored for the right one
         '.....XX.',
         '....XXX.',
         '...XXX..',
         '..XXX...',
         '.XXX....',
         'XX......',
         'X.......']


def draw():
    g = Glyph(tier=TIER)
    g.circ(8, 4, 16)                         # ring cols 8..23, rows 4..19
    g.bitmap(12, 8, FONT['V8'])
    g.bitmap(4, 20, PROBE)
    g.mirror_x(only=lambda x, y: y >= 20)
    return g
