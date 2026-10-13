"""batteries family helpers (stationary batteries and battery chargers), STYLE 6 and 8.4.

The family's shared visual system:
- Every icon is the IEC cell (long plate = +, short plate = -) with its plates in the tier hue and
  INK leads (kit battery_block). No casing, tray or slot grid: nothing of the in-world model.
- Stationary batteries are all one cell on MID_M, told apart by the kit kind mark in MARK_BOX:
  cost '$', capacity 'C', current 'A', life 'L', voltage 'V', single use '1'. The voltage-oriented
  battery is also the only MV member (blue); the rest are LV (green).
- Chargers are the same cell with the lightning-bolt mark (energy going in). The three chargers are
  tier/rating siblings: 50V (LV) and 200V (MV) differ by colour only; the weak 50V charger is the LV
  charger drawn with the small cell (12/8 plates), never with a letter or a thinner stroke.
"""
from kit import battery_block

BATTERY_KINDS = ('cost', 'capacity', 'current', 'life', 'voltage', 'single_use')


def stationary_battery(g, kind):
    """A stationary battery block: the IEC cell in the tier hue on MID_M + its kind mark."""
    assert kind in BATTERY_KINDS, kind
    battery_block(g, kind)


def charger(g, weak=False):
    """A battery charger (one icon per tier): the IEC cell in the tier hue + the bolt mark;
    weak = the small cell."""
    battery_block(g, 'charge', small=weak)
