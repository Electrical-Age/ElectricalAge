"""e_coal_leggings - front-on leggings: a full-width hip piece and two straight legs standing
on the baseline.  CARBON plate with the family's LIT charge bolt on the hip piece, where a
belt buckle would be (families.brushes_armor_misc.armour)."""
from families.brushes_armor_misc import armour, BOLT

GRID = [
    "................",
    "................",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    "................",
    "................",
]


def draw():
    return armour(GRID, bolts=[BOLT(6, 3)])
