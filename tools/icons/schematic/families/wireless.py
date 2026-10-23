"""wireless family: power antennas, signal antennas and the wireless switch / button (STYLE.md 7.5, 8.6).

Shared system: the IEC antenna (mast + V, kit `antenna`) is the family's backbone, always in the tier hue
(the defining element, STYLE 6); what the antenna does is said by INK cues around it:

    power_antenna('tx')   {low,medium,high}_power_transmitter_antenna   kit transmitter(): two radiation arcs out
    power_antenna('rx')   {low,medium,high}_power_receiver_antenna      kit receiver(): two arrows pointing in
    signal_antenna('tx')  wireless_signal_transmitter                   transmitter + INK signal feed line
    signal_antenna('rx')  wireless_signal_receiver                      receiver + INK signal feed line
    repeater()            wireless_signal_repeater                      kit repeater(): one arrow in, one out
    wireless_contact()    wireless_switch                               'wireless' (radio) mark + mark-layout contact
    wireless_contact(button=True)  wireless_button                      the same + a push actuator on the blade

Power level = the antenna colour (low/medium/high -> lv/mv/hv; contract decision 3: one icon per tier, the
same program with a different TIER). Transmitter vs receiver = arcs radiating away vs arrows coming in.

Signal vs power (STYLE 5.4: the family has LV and HV members that differ in function, so colour alone may not
tell signal broadcast from power beaming): the two signal antennas add the SIGNAL-ANTENNA CUE, an INK lead
(the signal-cable lead) from the mast foot to the LEFT core edge (rows 26-27, cols 4..8, outside the
stack-count zone). It is shown only on the antennas that join the wired signal network (they read a
signal cable / drive one); it does not mean "has a cable" in general (the power antennas are cabled too,
but keep the bare binding convention), and the repeater leaves it out because it only relays radio to
radio. The cue is family-invented and not yet in STYLE 7.5: pending owner sign-off (kit request filed).
"""
from kit import INK, LO, transmitter, receiver, repeater, switch, mark, push_actuator

__all__ = ['power_antenna', 'signal_antenna', 'signal_repeater', 'wireless_contact']

# signal-antenna cue: an INK lead on rows FEED_Y..FEED_Y+1 from the mast foot to the left core edge.
FEED_Y = 26


def power_antenna(g, kind):
    """Wireless power antenna (binding convention, ref/conventions.png): kit transmitter()/receiver() in
    the tier hue."""
    if kind == 'tx':
        transmitter(g, g.hue)
    elif kind == 'rx':
        receiver(g, g.hue)
    else:
        raise ValueError(kind)


def signal_antenna(g, kind):
    """Wireless signal transmitter / receiver: the power antenna's glyph (same mast, arcs or arrows) plus
    the INK signal-antenna cue (signal-cable lead from the mast foot to the left edge), the structural
    signal-vs-power cue."""
    power_antenna(g, kind)
    g.hline(LO, 8, FEED_Y, INK)          # joins the mast (cols 9-10, down to row 27)


def signal_repeater(g):
    """Wireless signal repeater (binding convention): kit repeater() in the signal hue."""
    repeater(g, g.hue)


# mark-layout push button: kit switch(mark=True) (pivot (8, 19), blade tip (16, 12), fixed contact at
# cols 20..25) with the push actuator stem (cols 11-12) standing on the blade and its cap on row 4 like
# kit button() / signal_button (cols 8..15, 5 clear cols before the mark at col 21).
BTN_X = 11
BTN_TOP = 4


def wireless_contact(g, button=False):
    """wireless_switch / wireless_button: the 'wireless' (radio) mark top right + the mark-layout make
    contact in the signal hue; the button adds an INK push actuator onto the blade."""
    mark(g, 'wireless')
    switch(g, g.hue, mark=True)
    if button:
        # blade runs from (8, 19) to (16, 12): at col BTN_X its upper edge is about row 16
        push_actuator(g, BTN_X, BTN_TOP, 15)

