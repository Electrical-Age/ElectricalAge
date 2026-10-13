"""Family conventions: the binding drawing of each multi-variant family (STYLE.md sections 8-9).

Call these from icons/<id>.py or families/<family>.py instead of re-placing the parts yourself;
every function here is drawn and checked in ref/conventions.png (python3 -m kit.catalog
ref/conventions.png conventions). Each takes the Glyph first and uses g.hue for the defining element.

    from kit.conventions import bulb
    def draw():
        g = Glyph(tier=TIER); bulb(g, 'carbon', small=True); return g
"""
from .canvas import MID
from .palette import INK
from .symbols import *          # noqa: F401,F403
from .symbols import LO, HI

__all__ = ['bulb', 'heating_corp', 'battery_block', 'generator', 'sensor_diode', 'socket',
           'sample_hold', 'oscillator', 'regulator', 'macerator_rollers']


def bulb(g, kind=None, small=False):
    """Light bulbs: lamp in the tier hue; type mark top right (None = incandescent); small_* = small ring."""
    if kind:
        mark(g, kind)
        lamp(g, g.hue, ring=RING_M_SMALL if small else RING_M)
    else:
        lamp(g, g.hue, ring=RING_SMALL if small else RING)


def heating_corp(g, metal, small=False):
    """Heating corps: resistor in the tier hue on MID_M, material mark (Cu / Fe / W)."""
    mark(g, metal)
    resistor(g, g.hue, y=MID_M, small=small)


def battery_block(g, kind=None, cells=1, small=False):
    """Batteries: IEC cell in the tier hue (or INK if untiered); kind mark top right."""
    if kind:
        mark(g, kind)
        battery(g, g.hue, cells=cells, x=8, y=MID_M, small=small)
    else:
        battery(g, g.hue, cells=cells, small=small)


def generator(g, kind):
    """Generators / turbines: 'G' ring (RING_M) in the tier hue, input pictogram top right."""
    mark(g, kind)
    machine(g, 'G', g.hue, ring=RING_M)


def sensor_diode(g, kind):
    """Optical sensors: what is sensed as a mark, the photodiode in the signal hue below it."""
    mark(g, kind)
    diode(g, g.hue, x0=6, w=10, h=10, y=MID_M)


def socket(g, kind):
    """Lamp sockets (untiered, all INK): the lamp symbol plus the mounting it stands for."""
    if kind == 'simple':
        lamp(g)
    elif kind == 'suspended':                       # hangs from the ceiling: one lead from the top
        lamp(g, with_leads=False)
        g.vline(15, LO, 6)
    elif kind == 'long_suspended':                  # longer drop: the ring hangs lower
        lamp(g, with_leads=False, ring=(7, 10, 18))
        g.vline(15, LO, 9)
    elif kind == 'flat':                            # flush on the ceiling: a 2px ceiling bar above the lamp
        g.hline(LO, HI, LO)
        lamp(g, with_leads=False, ring=(7, 9, 18))
    elif kind == 'sconce':                          # wall bracket: wall bar at the left, arm to the lamp
        g.vline(LO, LO, HI)
        g.hline(LO, 9, MID)
        lamp(g, with_leads=False, ring=(10, 7, 18))
    else:                                           # lamp_socket_a / _b_projector / robust: letter mark
        mark(g, kind)
        lamp(g, ring=RING_M)


def sample_hold(g):
    """sample_and_hold: series switch, then a hold capacitor to the bottom edge (the textbook S&H)."""
    switch(g, g.hue, y=10, pivot=8, tip=(14, 4), x1=19)
    g.vline(21, 12, 15, g.hue)
    g.hline(15, 27, 16, g.hue)
    g.hline(15, 27, 21, g.hue)
    g.vline(21, 23, HI, g.hue)


def regulator(g, label):
    """Regulators: the control law as an INK FONT label on top, the feedback loop in the signal hue
    below it: from the output (right) down, back along the bottom, up and into the input (arrow)."""
    g.text_center(4, label)
    g.wire([(26, 17), (26, 26), (LO, 26), (LO, 17), (18, 17)], g.hue)
    g.bitmap(19, 14, ARROW_HEADS['right'], g.hue)


def oscillator(g, wave, controlled=False):
    """Oscillators: the waveform itself, large, in the signal hue (rows 13..24). Voltage-controlled
    ones add the INK control input: an arrow down from the top edge onto the wave."""
    if wave == 'square':
        square(g, LO, HI, 14, 23, 1.5, g.hue)
    elif wave == 'sawtooth':
        sawtooth(g, LO, HI, 13, 23, 2, g.hue)
    else:
        sine(g, LO, HI, 19.0, 4.5, 1.5, g.hue)
    if controlled:
        arrow(g, 15, LO, 15, 8)


def macerator_rollers(g, feed=True, grit=True):
    """Crushing process (macerators): two toothed rollers in g.hue, offset diagonally so the pair
    never reads as a face, the INK feed arrowhead aimed at their nip and two grains of grit below it."""
    if feed:
        feed_arrow(g, x=17)
    gear(g, 6, 11, g.hue, -1)
    gear(g, 18, 15, g.hue, +1)
    if grit:
        grains(g, ((12, 24), (16, 26)))
