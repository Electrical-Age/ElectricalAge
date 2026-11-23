"""Family helper: the RS485 bus devices (addressable_button, addressable_light, floor_sensor), 32 px.

Each is a signal sibling's device put on the RS485 bus with its own address (Baughn 2026-11-05):

    addressable_button  a push button (signal_button: kit button())
    addressable_light   an LED lamp   (led_vumeter: kit led())
    floor_sensor        a presence sensor, the proximity sensor's (electrical_entity_sensor: diode + mark)

The shared cue is a BUS mark in the mark slot (MARK_BOX, top right, INK): a 2 px bus bar with two
drops, the modbus_rtu glyph's "bus with taps" in miniature.  It replaces the sibling's own mark, so
each symbol moves to the mark layout:

    addressable_button  bus mark + the mark-layout contact with the push actuator (as wireless_button)
    addressable_light   bus mark + the diode on MID_M in the signal hue + two emission arrows (up-right)
                        left of the mark: the LED, as led_vumeter
    floor_sensor        bus mark + the diode on MID_M in the signal hue + two incident arrows (down-right)
                        left of the mark: a presence detector (it sees the elevator car), as the
                        photodiode/sensor siblings.  Light vs floor sensor = arrows out vs in.

The bus mark is family-invented (STYLE section 8: no registered mark for a bus); it should become a
registered kit mark ('bus') if the owner accepts it.  It is 6 wide (cols 22..27) so the kit's stock
arrows_out/arrows_in at (4, 4), pitch 9 (cols 4..18), keep 3 clear px from it.
"""
from kit import INK, MID_M, MARK_BOX, switch, push_actuator, diode, arrows_in, arrows_out

__all__ = ['bus_mark', 'addressable_button', 'addressable_light', 'floor_sensor']

#: the bus mark, 6 x 6 (cols 22..27, rows 4..9; a FONT letter's width): a bus bar with two drops
BUS = (
    'XXXXXX',
    'XXXXXX',
    'XX..XX',
    'XX..XX',
    'XX..XX',
    'XX..XX',
)

# mark-layout push button, as families.wireless (kit switch(mark=True) + actuator stem on the blade)
BTN_X, BTN_TOP = 11, 4


def bus_mark(g):
    """The INK bus mark, right-aligned in MARK_BOX from row 4."""
    g.bitmap(MARK_BOX[2] + 1 - len(BUS[0]), MARK_BOX[1], BUS, INK)


def addressable_button(g):
    """Bus mark + mark-layout make contact in the hue + INK push actuator on the blade."""
    bus_mark(g)
    switch(g, g.hue, mark=True)
    push_actuator(g, BTN_X, BTN_TOP, 15)


def addressable_light(g):
    """Bus mark + LED: diode in the hue on MID_M with emission arrows (up-right) left of the mark."""
    bus_mark(g)
    arrows_out(g, 4, 4)
    diode(g, g.hue, x0=8, w=10, h=10, y=MID_M)


def floor_sensor(g):
    """Bus mark + presence detector: diode in the hue on MID_M with incident arrows (down-right)."""
    bus_mark(g)
    arrows_in(g, 4, 4)
    diode(g, g.hue, x0=8, w=10, h=10, y=MID_M)
