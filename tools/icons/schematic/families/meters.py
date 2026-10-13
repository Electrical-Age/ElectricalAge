"""Meters / displays family helpers (manifest family meter_display), STYLE.md sections 7.4, 8.5, 8.6.

The family's shared system: every member is a RING (the meter ring of section 6), and the interior
says what kind of instrument it is:

    letter E           energy meters (kit meter(); untiered -> all INK)          energy_meter, advanced (+)
    zig-zag trace      data loggers (kit trace(); signal hue ring)               data_logger, modern (M), industrial (I)
    needle             analog indicating instrument (needle_meter(); signal)    analog_gauge
    two clock hands    clocks (clock_face(); untiered, NO leads: battery-run)    analog_watch, digital_watch (D)

led_vumeter is the one member without a ring: the kit LED symbol (led(), signal hue), STYLE 8.1's LED.

    needle_meter(g, ring=RING)     ring in g.hue + INK leads + an INK pointer standing on a short INK
                                   base bar (the pivot / scale zero line) in the lower half of the hole,
                                   leaning to the upper right (1:2 slope, clean stair-steps). No arrowhead
                                   (that would read as IEC 'adjustable', the rheostats) and no pivot dot.
                                   A bare pointer through the hole read as a slashed zero at GUI 2; the
                                   base bar makes it a needle on its pivot. Bar and pointer are short and
                                   keep clear pixels to the ring, so they never fuse with it.
    clock_face(g, ring=RING)       INK ring + minute hand (up) + hour hand (right), joined at the centre:
                                   a 3 o'clock face. Full-size ring, never the 7x7 'clock' pictogram (that
                                   is electrical_timer's signal mark; STYLE 5.4 forbids reusing it unchanged
                                   on an untiered item). No leads: the watches run from an internal battery.
"""
from kit import INK, RING, ring_center, ring_leads


def needle_meter(g, ring=RING, needle=INK):
    """Analog indicating instrument: ring in g.hue (the defining element), INK leads, INK needle."""
    x0, y0, D = ring
    cx, cy = ring_center(ring)
    xi, yi = int(cx) - 1, int(cy) - 1         # top-left of the 2x2 centre cell (15, 15 for RING)
    g.circ(x0, y0, D, g.hue)
    # base bar (rows yi+4, yi+5; 6 wide, cols 13..18 for RING) and the pointer rising from it to the
    # upper right (dx 3, dy 6; (15,18) -> (18,12) for RING). Both stand free inside the hole with clear
    # pixels to the ring: a needle touching the ring fused with it into a chord / slashed zero at GUI 2.
    g.hline(xi - 2, xi + 3, yi + 4, needle)
    g.line(xi, yi + 3, xi + 3, yi - 3, needle)
    ring_leads(g, INK, ring)


def clock_face(g, ring=RING, c=INK):
    """Clock: ring + two hands from the centre (minute hand up, shorter hour hand right). No leads."""
    x0, y0, D = ring
    cx, cy = ring_center(ring)
    xi, yi = int(cx) - 1, int(cy) - 1         # top-left of the 2x2 centre cell (15, 15 for RING)
    r = D // 2                                # 9 for RING, 8 for RING_M
    g.circ(x0, y0, D, c)
    g.vline(xi, yi - (r - 4), yi + 1, c)      # minute hand, up to 3 px inside the ring
    g.hline(xi, xi + 1 + (r - 5), yi, c)      # hour hand, shorter, to the right
