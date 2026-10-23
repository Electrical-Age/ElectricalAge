"""led_vumeter: on/off signal indicator LED -> the kit LED symbol in the signal hue (led(g, g.hue): diode on
MID_M + INK emission arrows). It shares the symbol of 50v/200v_led_bulb (lv/mv) and differs from them by
tier colour only; STYLE 5.4's structural rules (signal vs untiered, LV vs HV) do not apply to that pair.
Accepted as is pending the owner: signal vs LV is the closest pair under protan vision (~14.6 apart). If a
structural difference is wanted, a registered 'indicator' mark + led() in the mark layout is the lever
(requested from the kit; not invented locally)."""
from kit import *

TIER = 'signal'


def draw():
    g = Glyph(tier=TIER)
    led(g, g.hue)
    return g
