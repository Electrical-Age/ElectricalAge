"""50v_plate_machine: process machine (STYLE 7.2): a T press ram in the LV hue coming down onto an LV anvil bar, the INK plate leaving to the right as an arrow (families.machines.plate_machine). 200v twin in MV."""
from kit import Glyph
from families.machines import plate_machine

TIER = 'lv'


def draw():
    g = Glyph(tier=TIER)
    plate_machine(g)
    return g
