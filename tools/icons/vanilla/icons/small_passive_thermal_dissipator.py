"""small_passive_thermal_dissipator - passivethermaldissipatora.obj: the THERMAL comb heat
sink seen from its fin ends (lit fin edges over the top), with the light connection square, in
SMALL_BOX (size axis).  See families/solar_thermal.py."""
from families.solar_thermal import passive_dissipator


def draw():
    return passive_dissipator()
