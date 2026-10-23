"""e_coal_boots - a pair of boots seen from the side, mirrored as vanilla draws them: the left
boot's toe points left, the right boot's right.  Two-row cuffs (the second row takes the lit
band), short shafts and feet on the baseline.  CARBON plate with a short LIT charge vein down each
shaft (the family's bolt motif, shortened to fit; families.brushes_armor_misc.armour)."""
from families.brushes_armor_misc import armour, pattern

GRID = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "..#####..#####..",
    "..#####..#####..",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "..#####..#####..",
    ".######..######.",
    ".######..######.",
    ".######..######.",
    "................",
    "................",
]


def draw():
    # a 3-pixel zigzag, so carbon still shows on the 2-pixel-wide shaft interior
    left = (".#",
            "o.",
            "#.")
    right = ("#.",
             ".o",
             ".#")
    return armour(GRID, bolts=[pattern(left, 4, 7), pattern(right, 10, 7)])
