"""heating_elements family helpers (heating corps), STYLE 6 and 8.3.

The family's shared visual system:
- Every icon is the IEC resistor (empty rectangle) on MID_M with its frame in the tier hue and INK
  leads out to cols 4 and 27 (kit heating_corp). No coil, meander or heat dots: nothing of the old
  item art, and no heat waves (STYLE 8.3: the mark slot needs the space and the hue already says
  "rated heater"; a tier-coloured plain resistor is reserved for heating corps).
- The element's metal is the INK kit mark in the mark slot: copper 'Cu', iron 'Fe', tungsten 'W'.
- Size is geometry only: the standard corp is the 16x10 resistor, small_* the 12x8 resistor
  (resistor(small=True)), never a letter.
- 50V and 200V are tier siblings told apart by colour only: 50V = lv (green), 200V = mv (blue).
"""
from kit import heating_corp

METALS = ('copper', 'iron', 'tungsten')


def heating_element(g, metal, small=False):
    """A heating corp: the resistor in the tier hue on MID_M + the material mark (Cu / Fe / W)."""
    assert metal in METALS, metal
    heating_corp(g, metal, small=small)
