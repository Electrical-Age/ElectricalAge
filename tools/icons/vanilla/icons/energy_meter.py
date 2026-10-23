"""energy_meter - energymeter.obj: white wall box (front 12 x 12, sixnode depth 2) with the white
face panel in its light-grey frame: one row of digit wheels (red unit wheel) on the white panel, the ENERGY legend,
the black power-disk window and the keypad marks.  Untiered; quality level 0 (no trim).
Drawing: families.meters.energy_meter."""
from families.meters import energy_meter


def draw():
    return energy_meter(advanced=False)
