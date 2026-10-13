"""signal_io family: signal-tier inputs and outputs (STYLE.md sections 8.5, 8.6, 9).

Shared system. Every member is signal tier, so the signal hue sits on the defining element and the
family's members differ in STRUCTURE:

    switch-type inputs   the kit make contact, whole contact in the hue (STYLE 6)
        signal_switch        switch(g, g.hue)                                   (STYLE 8.6)
        signal_button        button(g, g.hue): + INK push actuator              (STYLE 8.6)
        electrical_timer     mark 'timer' (clock) + mark-layout contact          (STYLE 8.5)
    sources              signal_source = source(g, 'V', g.hue)                   (STYLE 7.3)
                         signal_trimmer = resistor in the hue + INK preset mark  (STYLE 9)
    alarms               standard_alarm = bell(g, g.hue);                        (STYLE 8.6)
                         nuclear_alarm = mark 'nuclear' (!) + bell lowered by 4
    redstone converters  hue staircase body + INK direction arrow, no box (proposal) -> converter()
    modbus_rtu           (no STYLE row)                                            -> rtu()

converter(): the converter BODY is a rising STAIRCASE in the signal hue (the quantised transfer
characteristic: redstone has levels 0..15, the converter maps them to/from a signal voltage). This is
the defining element, so it carries the hue (STYLE 6); the lead and its arrowhead are INK (STYLE 5.1),
the same split as the other signal waveform glyphs (d_flip_flop edge, lowpass response, data_logger
trace). The order of the halves plus the arrow carry the direction, read left to right:
    redstone_to_voltage_converter   hue staircase (bottom-left) -> INK lead with the arrowhead OUT
    voltage_to_redstone_converter   INK lead with the arrowhead IN -> hue staircase (to the top-right)
The arrow and the staircase join into one stroke. Three 4 px steps keep the hue above MIN_HUE_PX.
No box (STYLE 9 / 11): the glyph is a transfer pictogram, like the kit's trace() and response().

rtu(): a remote terminal unit: a signal bus in the hue with three channel taps ending in open
terminals (the signal channels it exposes), and an INK two-way arrow above it (the link to the
external software). No box and no label (an 'RTU' label would need MICRO).
"""
from kit import (INK, LO, HI, MID, MID_M, switch, button, source, resistor, preset_mark, bell, mark,
                 arrow, terminal)

__all__ = ['signal_switch', 'signal_button', 'timer', 'signal_source', 'trimmer', 'alarm',
           'converter', 'rtu']


# ---- STYLE-table members ----------------------------------------------------------------------------
def signal_switch(g):
    """Gate-source toggle switch: the plain make contact in the signal hue (STYLE 8.6)."""
    switch(g, g.hue)


def signal_button(g):
    """Momentary push button: make contact in the hue + INK push actuator (STYLE 8.6)."""
    button(g, g.hue)


def timer(g):
    """Electrical timer: clock mark + the mark-layout make contact (STYLE 8.5)."""
    mark(g, 'timer')
    switch(g, g.hue, mark=True)


def signal_source(g):
    """Signal source: IEC voltage source (ring with the conductor through it) in the hue (STYLE 7.3)."""
    source(g, 'V', g.hue)


def trimmer(g):
    """Signal trimmer: resistor in the hue + INK pre-set (trimmer) mark (STYLE 9)."""
    resistor(g, g.hue)
    preset_mark(g, 9, 23, 21, 8)


def alarm(g, nuclear=False):
    """Alarms (STYLE 8.6): the IEC bell in the hue; nuclear adds the '!' mark and drops the bell by 4."""
    if nuclear:
        mark(g, 'nuclear')
        bell(g, g.hue, y=MID_M + 4)
    else:
        bell(g, g.hue)


# ---- redstone converters ----------------------------------------------------------------------------
def _stairs(g, x, y, steps, c, step=4):
    """Rising staircase wire: starts with a tread at (x, y) (top-left of the 2x2 stroke), then
    `steps` x (riser up `step`, tread right `step`). Returns the end point."""
    pts = [(x, y)]
    for _ in range(steps):
        x += step
        pts.append((x, y))
        y -= step
        pts.append((x, y))
    g.wire(pts, c)
    return x, y


# converter geometry: three 4 px steps; the arrow and the staircase join (one stroke).
STEP = 4
STEPS = 3
V2R_ROW = 19          # voltage_to_redstone: arrow row, raised so the arrowhead clears the stack count


def converter(g, to_redstone):
    """Redstone <-> signal converter (family proposal for STYLE 9): a hue staircase (the quantised
    transfer characteristic, the converter body) and an INK lead with a right-pointing arrowhead.
      to_redstone=False  hue staircase rising from the bottom-left (row 23 up to row 11), the INK
                         lead leaving its top step to the right with the arrowhead at col 27 (OUT).
      to_redstone=True   INK lead from the left edge on rows 19-20, its arrowhead (cols 9..12)
                         driving INTO the bottom step of the hue staircase, which rises to the
                         top-right (cols 12..25, rows 7..20) (IN).
    The order of the two halves plus the arrow carry the direction; the two icons are duals."""
    if to_redstone:
        arrow(g, LO, V2R_ROW, 12, V2R_ROW, INK)
        _stairs(g, 12, V2R_ROW, STEPS, g.hue, STEP)
    else:
        x, y = _stairs(g, LO, 23, STEPS, g.hue, STEP)
        arrow(g, x, y, HI - 1, y, INK)


# rtu geometry: two-way link arrow on rows 4..11, bus on rows 15-16, three taps to d=6 terminals
RTU_TAPS = (6, 15, 24)


def rtu(g):
    """Modbus RTU (no STYLE row; family proposal): a signal bus in the hue (rows 15-16, full width)
    with three taps down to open terminals (d=6, rows 22..27): the signal channels it exposes. Above
    it an INK two-way arrow (rows 4..11): the data link to the external software. No box, no label."""
    arrow(g, MID, 7, HI - 1, 7, INK)
    arrow(g, MID + 1, 7, LO, 7, INK)
    g.hline(LO, HI, MID, g.hue)
    for x in RTU_TAPS:
        g.vline(x, MID, 21, g.hue)
        terminal(g, x, 24, g.hue)
