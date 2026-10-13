"""50v_macerator: process machine (STYLE 7.2: INK feed in -> process element in the tier hue ->
INK product out). The crushing rollers (kit.conventions.macerator_rollers): two toothed rollers in
the LV hue, offset diagonally so the pair never reads as a face, the feed arrowhead aimed at their
nip, two grains of grit leaving below. 200v_macerator is the same program with TIER = 'mv'."""
from kit import Glyph, macerator_rollers

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    macerator_rollers(g)
    return g
