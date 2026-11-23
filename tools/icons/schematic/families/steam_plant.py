"""steam_plant family (schematic): the electric heaters (the only in-scope ids of the family; boilers and
condensers are untiered and out of schematic scope).

    electric_heater(g)  the heater resistor (IEC empty rectangle) on MID_M in the tier hue, INK heat waves at
                        HEAT_TOP above it: electricity in, heat out.  Structurally apart from its neighbours:
                        heating corps carry a metal mark (Cu/Fe/W) and no waves; the electrical furnace is the
                        same layout all INK (untiered); the thermal furnaces have two waves, a fuel mark and a
                        THERMAL heater.  48V = lv, 230V = mv: tier siblings told apart by colour only.
"""
from kit import MID_M, HEAT_TOP, resistor, heat_waves


def electric_heater(g):
    """Tier-hue heater resistor on MID_M under three INK heat waves (STYLE 7 'top' layout)."""
    heat_waves(g, *HEAT_TOP)
    resistor(g, g.hue, y=MID_M)
