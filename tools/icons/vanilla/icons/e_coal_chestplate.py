"""e_coal_chestplate - front-on chestplate: broad shoulder plates either side of a stepped neck
opening, the torso tapering to the waist on the baseline.  CARBON plate with the family's LIT
charge bolt in the middle of the chest (families.brushes_armor_misc.armour)."""
from families.brushes_armor_misc import armour, BOLT

GRID = [
    "................",
    "................",
    ".####......####.",
    ".#####....#####.",
    ".######..######.",
    ".##############.",
    ".##############.",
    "..############..",
    "...##########...",
    "...##########...",
    "...##########...",
    "...##########...",
    "...##########...",
    "....########....",
    "................",
    "................",
]


def draw():
    return armour(GRID, bolts=[BOLT(6, 7)])
