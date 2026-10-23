"""light_bulb family helpers (lamp-socket bulbs), STYLE 6 and 8.1.

The family's shared visual system:
- Every bulb is the IEC lamp (ring + X) with ring and X in the tier hue and INK leads (kit bulb).
  No glass globe, screw base or filament drawing: nothing of a physical bulb (decision 5). The
  IEC lamp has no lit/unlit state, so "lit" adds nothing here (no rays, no glow).
- The bulb type is the kit mark in MARK_BOX (lamp on RING_M): carbon 'C', economic (fluorescent)
  'E', farming 'F'. The plain iron-filament incandescent bulb is the bare lamp on RING (no mark).
- LED bulbs are the LED symbol (diode + two emission arrows, kit led), not a lamp.
- 50V (LV, green) and 200V (MV, blue) siblings differ by colour only (one icon per tier).
- small_50v_* bulbs are the same program with the small ring (RING_SMALL / RING_M_SMALL), never a
  letter or a thinner stroke.
- Emergency lamps (50v_/200v_emergency_lamp) are NOT drawn here: families/lighting_fixtures
  .emergency_lamp owns them, so this helper deliberately has no 'emergency' kind.
"""
from kit import bulb, led

KINDS = (None, 'carbon', 'economic', 'farming', 'led')


def light_bulb(g, kind=None, small=False):
    """A lamp-socket bulb in the tier hue. kind: None (incandescent), 'carbon', 'economic',
    'farming' or 'led'; small = the small ring (no small LED bulb exists)."""
    assert kind in KINDS, kind
    if kind == 'led':
        assert not small, 'no small LED bulb'
        led(g, g.hue)
    else:
        bulb(g, kind, small=small)
