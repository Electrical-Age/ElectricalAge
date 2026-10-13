"""e_coal_helmet - front-on helmet: a stepped, rounded dome over a full brow band, with cheek
guards either side of a face opening 6 wide and 4 tall down to the baseline.  CARBON plate with
the family's LIT charge bolt on the brow (families.brushes_armor_misc.armour)."""
from families.brushes_armor_misc import armour, BOLT

GRID = [
    "................",
    "................",
    "................",
    ".....######.....",
    "....########....",
    "...##########...",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "..###......###..",
    "..###......###..",
    "..###......###..",
    "..###......###..",
    "................",
    "................",
]


def draw():
    return armour(GRID, bolts=[BOLT(6, 4)])
