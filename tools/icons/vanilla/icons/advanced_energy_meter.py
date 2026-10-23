"""advanced_energy_meter - advancedenergymeter.obj: the energy meter's white box with two counter
rows (ENERGY, TIME) and their red unit wheels, the wide disk window at the bottom and the
perforated top.  Quality level 2 (kit.decisions.QUALITY, STYLE 7b): GOLD trim bands on the
frame above and below the face panel.  Untiered.  Drawing: families.meters.energy_meter."""
from families.meters import energy_meter


def draw():
    return energy_meter(advanced=True)
