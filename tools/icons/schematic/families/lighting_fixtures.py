"""lighting_fixtures family helpers: lamp sockets, the streetlight, the lamp supply, emergency lamps.

The family's shared visual system (STYLE.md sections 6, 8.1, 8.2):
- The IEC lamp (ring + X, kit `lamp`) is the backbone. Lamp sockets are VoltageLevelColor.Neutral (the
  bulb sets the voltage), so they are untiered and all INK; their structure tells them apart.
- The mounting is said by where the lamp's lead goes, or by a 2px mounting-surface bar, always at the
  standard ring size (D=18) so the family reads as one set:
      simple        plain lamp with side leads            (kit socket 'simple')
      suspended     lead up to the ceiling edge            (kit socket 'suspended')
      long_susp.    longer lead, lamp hangs lower          (kit socket 'long_suspended')
      flat          ceiling bar across the top             (kit socket 'flat')
      sconce        wall bar on the left + arm             (kit socket 'sconce')
      streetlight   floor bar across the bottom + pole up  (streetlight(), this file)
  The streetlight is the floor counterpart of the flat socket's ceiling bar: it keeps the lamp at the
  top (rows 4..21) and puts the full-width floor bar on rows 26-27, so its left end (cols 4..11) stays
  visible beside a stack count.
- Variants without a mounting cue (fluorescent E, socket A, socket B, robust R) use the kit letter mark
  over the mark-layout lamp (kit socket()).
- The lamp supply is not a socket (it holds no bulb; it feeds the sockets on its channel within range,
  wirelessly). It therefore does NOT use the lamp: it is the IEC voltage source (ring with the conductor
  through it) in the mark layout with the kit 'wireless' (radio) mark: a wireless supply. Untiered, INK.
  It differs from signal_source (plain source, standard layout, signal hue) in structure (mark, layout)
  as well as colour (STYLE 5.4).
- Emergency lamps (50V/200V tier siblings) follow the bulb convention: kit bulb('emergency'), the lamp
  in the tier hue under the INK '!' mark; one icon per tier, colour only.
"""
from kit import LO, HI, RING_M, lamp, source, mark
from kit.conventions import socket, bulb

__all__ = ['lamp_socket', 'streetlight', 'lamp_supply', 'emergency_lamp']

STREET_RING = (7, 4, 18)      # standard-size ring raised to the top core edge: cols 7..24, rows 4..21
FLOOR_Y = 26                  # floor bar rows 26-27, full core width (mirror of the flat socket's ceiling bar)


def lamp_socket(g, kind):
    """The nine binding socket conventions (STYLE 8.2), straight from the kit."""
    socket(g, kind)


def streetlight(g):
    """Streetlight: the lamp at the top, a pole (cols 15-16) down to a full-width floor bar."""
    x0, y0, D = STREET_RING
    lamp(g, with_leads=False, ring=STREET_RING)
    g.vline(15, y0 + D, FLOOR_Y - 1)
    g.hline(LO, HI, FLOOR_Y)


def lamp_supply(g):
    """Lamp supply: 'wireless' (radio) mark + IEC voltage source at RING_M with its lead to the right."""
    mark(g, 'wireless')
    source(g, 'V', ring=RING_M)


def emergency_lamp(g):
    """50V/200V emergency lamp: STYLE 8.1, bulb(g, 'emergency') -> '!' mark, lamp in the tier hue."""
    bulb(g, 'emergency')
