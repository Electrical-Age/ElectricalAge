"""200v_plate_machine: process machine (STYLE 7.2): a T press ram in the MV hue coming down onto an MV anvil bar, the INK plate leaving to the right as an arrow (families.machines.plate_machine). 50v twin in LV."""
from kit import Glyph
from families.machines import plate_machine

TIER = 'mv'


def draw():
    g = Glyph(tier=TIER)
    plate_machine(g)
    return g
