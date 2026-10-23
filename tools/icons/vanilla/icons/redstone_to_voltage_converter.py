"""redstone_to_voltage_converter - redtoele.obj, wall sixnode.

Light grey/white square slab (WHITE_PLASTIC, the texture's #f0f0f0) with the red 'R' redstone
glyph printed on it.  drawItem draws redstone 15, so the glyph is lit (bright redstone red).
The glyph is drawn upright (the model maps it turned a quarter; upright reads at GUI 2).
Lit strokes: LEAD_RED[2] face, LEAD_RED[1] on each stroke's right and bottom edges."""
from kit import Canvas
from families.signal_io import wall_plate, RS, RS_LIT

R = [
    "#####.",
    "##..##",
    "##..##",
    "#####.",
    "##.##.",
    "##..##",
    "##..##",
]


def draw():
    cv = Canvas(16)
    wall_plate(cv, 'white')
    pts = {(x, y) for y, row in enumerate(R) for x, c in enumerate(row) if c == '#'}
    for (x, y) in pts:
        edge = (x + 1, y) not in pts or (x, y + 1) not in pts
        cv.set(4 + x, 5 + y, RS if edge else RS_LIT)
    return cv
